package com.dreamsigner.wizard.ui.studio

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import kotlinx.serialization.json.contentOrNull
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.*
import java.nio.file.AtomicMoveNotSupportedException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Flow
import java.nio.ByteBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

internal data class TutorRequest(
    val endpoint: URI, val model: String, val apiKey: String, val goal: String,
    val history: List<StudioMessage>, val acceptedPlan: StudioPlan? = null, val reasoningEffort: ReasoningEffort = ReasoningEffort.AUTOMATIC,
)
internal fun interface StudioTutor { suspend fun respond(request: TutorRequest): String }

@Serializable private data class StudioDisk(val baseUrl: String = "", val model: String = "", val sessions: List<StudioSessionDisk> = emptyList(), val selectedId: String? = null, val providerMode: String = "api", val reasoningEffort: String = "AUTOMATIC", val apiModel: String? = null, val chatGptModel: String? = null, val graphProposal: KnowledgeGraphProposal? = null, val modelSlots: Map<String, StudioModelSettingDisk> = emptyMap(), val modelOverrides: Map<String, StudioModelSettingDisk> = emptyMap())
@Serializable private data class StudioModelSettingDisk(val model: StudioModelChoiceDisk? = null, val reasoningEffort: String? = null)
@Serializable private data class StudioSessionDisk(val id: String, val goal: String, val messages: List<StudioMessageDisk>, val plan: StudioPlanDisk? = null, val reasoningEffort: String? = null, val modelChoice: StudioModelChoiceDisk? = null, val planProposal: StudioPlanProposalDisk? = null, val assessment: StudioDiagnosticAssessment? = null)
@Serializable private data class StudioModelChoiceDisk(val providerMode: String, val baseUrl: String, val model: String)
@Serializable private data class StudioPlanDisk(val steps: List<StudioPlanStepDisk>, val decision: String = "proposed")
@Serializable private data class StudioPlanStepDisk(val title: String, val prerequisites: List<Int>, val evidence: String, val sources: List<Int>)
@Serializable private data class StudioDiagnosisDisk(val observation: String, val sources: List<Int>)
@Serializable private data class StudioPlanProposalDisk(val diagnosis: List<StudioDiagnosisDisk>, val plan: StudioPlanDisk)
@Serializable private data class StudioMessageDisk(val role: String, val text: String, val quiz: StudioQuiz? = null, val quizAnswer: StudioQuizAnswer? = null, val problem: StudioProblem? = null)

internal class DesktopStudioController(
    private val file: Path = defaultStudioFile(),
    private val tutor: StudioTutor = OpenAiCompatibleTutor(),
) : StudioController {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = false }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()
    private val apiKey = StringBuilder()
    private var requestJob: Job? = null
    private var requestVersion = 0L
    private var catalogVersion = 0L
    private var catalogJob: Job? = null
    private val pendingBySession = ConcurrentHashMap<String, String>()
    private val chatGpt = ChatGptPlanClient(file.resolveSibling("chatgpt-plan.json"))
    private val graphStore = DesktopKnowledgeGraphStore(file.resolveSibling("knowledge-graph.json"))
    override val state = MutableStateFlow(load().let { saved ->
        val graph = runCatching { graphStore.load() }
        saved.copy(chatGptConnected = chatGpt.connected, chatGptEmail = chatGpt.accountEmail, chatGptError = chatGpt.storageError,
            knowledgeGraph = graph.getOrDefault(KnowledgeGraph()), graphError = if (graph.isFailure) "Le graphe de connaissances est illisible. Son fichier est conservé ; corrige-le avant d'enregistrer des changements." else null)
    })

    init {
        state.value.sessions.forEach { session ->
            session.messages.lastOrNull()?.takeIf { it.role == "user" }?.let { pendingBySession[session.id] = it.text }
        }
        if (chatGpt.connected) scope.launch {
            try {
                val models = chatGpt.models().map { it.slug }
                transient { copy(chatGptModels = models, chatGptError = if (models.isEmpty()) "Aucun modèle ChatGPT disponible pour ce compte." else null) }
            } catch (_: Exception) {
                transient { copy(chatGptError = "Impossible de charger les modèles ChatGPT. Vérifie la connexion puis réessaie.") }
            }
        }
    }

    override fun select(id: String) {
        if (state.value.busy) return
        mutate { copy(selectedId = id, error = null) }
    }

    override fun create(goal: String): Boolean {
        if (state.value.storageError != null || state.value.busy || goal.isBlank() || goal.length > 500) return false
        val current = state.value
        val routingEnabled = current.modelSlots.isNotEmpty() || current.modelOverrides.isNotEmpty()
        val choice = if (routingEnabled) null else current.model.takeIf { it.isNotBlank() }?.let { StudioModelChoice(current.providerMode, if (current.providerMode == "api") current.baseUrl else "", it) }
        val session = StudioSession(UUID.randomUUID().toString(), goal.trim(), emptyList(), reasoningEffort = if (routingEnabled) null else current.reasoningEffort, modelChoice = choice)
        return mutate { copy(sessions = listOf(session) + sessions, selectedId = session.id, error = null) }
    }

    override fun discoverApiModels(baseUrl: String, apiKey: String) {
        if (state.value.busy || state.value.apiModelsBusy) return
        val normalized = baseUrl.trim().trimEnd('/')
        val endpoint = try { validateEndpoint(normalized) } catch (_: Exception) {
            transient { copy(apiModels = emptyList(), apiModelsUrl = "", apiModelsError = "Saisis une URL HTTPS valide, ou une adresse HTTP locale.") }
            return
        }
        val secret = apiKey.trim().ifBlank {
            if (normalized == state.value.baseUrl) synchronized(this.apiKey) { this.apiKey.toString() } else ""
        }
        if (secret.isBlank() && !endpoint.isLoopback()) {
            transient { copy(apiModels = emptyList(), apiModelsUrl = "", apiModelsError = "Saisis la clé API du fournisseur distant.") }
            return
        }
        val version = synchronized(lock) { ++catalogVersion }
        transient { copy(apiModels = emptyList(), apiModelsUrl = "", apiModelsBusy = true, apiModelsError = null) }
        catalogJob = scope.launch {
            try {
                val modelsUri = URI(endpoint.toString().removeSuffix("/chat/completions") + "/models")
                val builder = HttpRequest.newBuilder(modelsUri).timeout(java.time.Duration.ofSeconds(15))
                if (secret.isNotBlank()) builder.header("Authorization", "Bearer $secret")
                val client = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build()
                val response = withTimeout(15_000) { client.sendAsync(builder.GET().build(), boundedBodyHandler(1024 * 1024)).await() }
                require(response.statusCode() in 200..299)
                val data = Json.parseToJsonElement(response.body().decodeToString()).jsonObject.getValue("data").jsonArray
                val models = data.mapNotNull { item -> (item as? JsonObject)?.get("id")?.let { (it as? JsonPrimitive)?.contentOrNull } }
                    .filter { it.isNotBlank() && it.length <= 200 }.distinct().sorted().take(2000)
                synchronized(lock) {
                    if (version == catalogVersion) state.value = state.value.copy(apiModels = models, apiModelsUrl = normalized, apiModelsBusy = false,
                        apiModelsError = if (models.isEmpty()) "Le fournisseur n’a retourné aucun modèle. Tu peux saisir son identifiant manuellement." else null)
                }
            } catch (_: Exception) {
                synchronized(lock) {
                    if (version == catalogVersion) state.value = state.value.copy(apiModelsBusy = false, apiModelsError = "Impossible de charger les modèles. Vérifie l’URL et la clé, ou saisis un identifiant manuellement.")
                }
            }
        }
    }

    override fun cancelApiModelDiscovery() {
        synchronized(lock) {
            catalogVersion += 1
            catalogJob?.cancel()
            catalogJob = null
            state.value = state.value.copy(apiModels = emptyList(), apiModelsUrl = "", apiModelsBusy = false, apiModelsError = null)
        }
    }

    private fun routingSetting(model: String, effort: ReasoningEffort?): StudioModelSetting {
        val current = state.value
        val choice = model.trim().takeIf { it.isNotEmpty() }?.let { StudioModelChoice(current.providerMode, if (current.providerMode == "api") current.baseUrl else "", it) }
        return StudioModelSetting(choice, effort)
    }

    override fun setModelSlot(slot: StudioModelSlot, model: String, effort: ReasoningEffort?) {
        if (!state.value.busy && state.value.storageError == null) mutate { copy(modelSlots = modelSlots + (slot to routingSetting(model, effort))) }
    }

    override fun setModelOverride(role: StudioModelRole, model: String, effort: ReasoningEffort?) {
        if (!state.value.busy && state.value.storageError == null) mutate { copy(modelOverrides = modelOverrides + (role to routingSetting(model, effort))) }
    }

    override fun setSessionModel(model: String?) {
        val current = state.value
        val id = current.selectedId ?: return
        if (current.busy || current.storageError != null) return
        val available = if (current.providerMode == "chatgpt") current.chatGptModels else if (current.apiModelsUrl == current.baseUrl) current.apiModels else emptyList()
        if (model != null && model != current.model && model !in available) return
        val choice = model?.let { StudioModelChoice(current.providerMode, if (current.providerMode == "api") current.baseUrl else "", it) }
        mutate { copy(sessions = sessions.map { if (it.id == id) it.copy(modelChoice = choice) else it }, error = null) }
    }

    private fun effectiveSessionConfig(current: StudioState, session: StudioSession, message: String): StudioState? {
        val role = when {
            message == STUDIO_GRAPH_REQUEST -> StudioModelRole.GRAPH_EXTRACTION
            message == STUDIO_PLAN_REQUEST -> StudioModelRole.TEACHING_PLAN
            message == STUDIO_DIAGNOSIS_REQUEST -> StudioModelRole.DIAGNOSTIC
            message == STUDIO_PROBLEM_REQUEST || message.startsWith(STUDIO_PRACTICE_ATTEMPT_PREFIX) -> StudioModelRole.PRACTICE
            session.plan?.decision == "accepted" -> StudioModelRole.TEACHING_PLAN
            else -> StudioModelRole.DIAGNOSTIC
        }
        val fallback = StudioModelSetting(StudioModelChoice(current.providerMode, if (current.providerMode == "api") current.baseUrl else "", current.model), current.reasoningEffort)
        val slot = current.modelSlots[studioSlotFor(role)]
        val slots = mapOf(studioSlotFor(role) to StudioModelSetting(slot?.model ?: fallback.model, slot?.reasoningEffort ?: fallback.reasoningEffort))
        val resolved = resolveStudioModel(role, slots, current.modelOverrides, StudioModelSetting(session.modelChoice, session.reasoningEffort)) ?: return null
        val choice = resolved.model
        if (choice.providerMode != current.providerMode || (choice.providerMode == "api" && choice.baseUrl != current.baseUrl)) return null
        return current.copy(model = choice.model, reasoningEffort = resolved.reasoningEffort ?: current.reasoningEffort)
    }

    override fun setReasoningEffort(effort: ReasoningEffort) {
        if (!state.value.busy && state.value.storageError == null) mutate { copy(reasoningEffort = effort) }
    }

    override fun setSessionReasoningEffort(effort: ReasoningEffort?) {
        val current = state.value
        val id = current.selectedId ?: return
        if (current.busy || current.storageError != null) return
        mutate { copy(sessions = sessions.map { if (it.id == id) it.copy(reasoningEffort = effort) else it }) }
    }

    override fun configure(baseUrl: String, model: String, apiKey: String): Boolean {
        if (state.value.storageError != null || state.value.busy) return false
        val normalizedUrl = baseUrl.trim().trimEnd('/')
        val endpoint = try { validateEndpoint(normalizedUrl) } catch (_: Exception) {
            transient { copy(connectionError = "Saisis une URL HTTPS valide, ou une adresse HTTP locale.") }
            return false
        }
        if (model.isBlank()) {
            transient { copy(connectionError = "Saisis le nom du modèle proposé par ton fournisseur.") }
            return false
        }
        val effectiveKey = apiKey.trim().ifBlank {
            if (normalizedUrl == state.value.baseUrl) synchronized(this.apiKey) { this.apiKey.toString() } else ""
        }
        if (effectiveKey.isBlank() && !endpoint.isLoopback()) {
            transient { copy(connectionError = "Saisis la clé API du fournisseur distant.") }
            return false
        }
        val saved = mutate { copy(baseUrl = normalizedUrl, model = model.trim(), apiModel = model.trim(), providerMode = "api", error = null, connectionError = null, hasApiKey = effectiveKey.isNotBlank(),
            apiModels = if (apiModelsUrl == normalizedUrl) apiModels else emptyList(), apiModelsUrl = if (apiModelsUrl == normalizedUrl) apiModelsUrl else "") }
        if (saved) synchronized(this.apiKey) { this.apiKey.setLength(0); this.apiKey.append(effectiveKey) }
        return saved
    }

    private data class QuizSubmission(val sessionId: String, val index: Int, val quiz: StudioQuiz, val answer: StudioQuizAnswer)
    private data class ProblemSubmission(val sessionId: String, val index: Int, val problem: StudioProblem, val attempt: StudioProblemAttempt)

    override fun send(message: String): Boolean = sendTurn(message, null, null)
    override fun requestQuiz(type: StudioQuizType): Boolean = synchronized(lock) {
        val session = state.value.sessions.firstOrNull { it.id == state.value.selectedId }
        if (session == null || session.messages.any { (it.quiz != null && it.quizAnswer == null) || (it.problem != null && it.problem.result == null) }) false
        else sendTurnLocked(if (type == StudioQuizType.PREDICT) STUDIO_PREDICT_REQUEST else STUDIO_CHECK_REQUEST, null, null)
    }

    override fun requestPracticeProblem(): Boolean = synchronized(lock) {
        val session = state.value.sessions.firstOrNull { it.id == state.value.selectedId }
        if (session == null || session.messages.any { (it.quiz != null && it.quizAnswer == null) || (it.problem != null && it.problem.result == null) }) false
        else sendTurnLocked(STUDIO_PROBLEM_REQUEST, null, null)
    }

    override fun requestDiagnosisConclusion(): Boolean = synchronized(lock) {
        val session = state.value.sessions.firstOrNull { it.id == state.value.selectedId }
        val answered = session?.messages?.takeLast(12)?.count { it.quizAnswer != null } ?: 0
        if (session == null || answered < 2 || session.messages.any { (it.quiz != null && it.quizAnswer == null) || (it.problem != null && it.problem.result == null) }) false
        else sendTurnLocked(STUDIO_DIAGNOSIS_REQUEST, null, null)
    }

    override fun requestGraphExtraction(): Boolean = synchronized(lock) {
        val snapshot = state.value
        val session = snapshot.sessions.firstOrNull { it.id == snapshot.selectedId }
        if (session == null || snapshot.graphError != null || snapshot.graphProposal != null ||
            graphSourceIndices(session.messages + StudioMessage("user", STUDIO_GRAPH_REQUEST)).isEmpty() ||
            session.messages.any { (it.quiz != null && it.quizAnswer == null) || (it.problem != null && it.problem.result == null) }) false
        else sendTurnLocked(STUDIO_GRAPH_REQUEST, null, null)
    }

    override fun discardGraphProposal(): Boolean {
        if (state.value.busy || state.value.storageError != null) return false
        return mutate { copy(graphProposal = null) }
    }

    override fun refreshKnowledgeGraph(): Boolean = synchronized(lock) {
        val graph = try { graphStore.load() } catch (_: Exception) {
            state.value = state.value.copy(graphError = "Le graphe est encore illisible. Son fichier est conservé.")
            return false
        }
        state.value = state.value.copy(knowledgeGraph = graph, graphError = null)
        true
    }

    override fun acceptGraphProposal(): Boolean = synchronized(lock) {
        val snapshot = state.value
        val proposal = snapshot.graphProposal ?: return false
        if (snapshot.busy || snapshot.storageError != null || snapshot.graphError != null) return false
        val live = try { graphStore.load() } catch (_: Exception) {
            state.value = snapshot.copy(graphError = "Le graphe est illisible. Aucun changement n'a été enregistré.")
            return false
        }
        if (live != proposal.base && live != proposal.graph) {
            state.value = snapshot.copy(knowledgeGraph = live, graphError = "Le graphe a changé depuis cette proposition. Retire-la puis relance l'extraction.")
            return false
        }
        try {
            if (live != proposal.graph) graphStore.save(proposal.graph)
            state.value = snapshot.copy(knowledgeGraph = proposal.graph, graphError = null)
        } catch (_: Exception) {
            state.value = snapshot.copy(graphError = "Impossible d'enregistrer le graphe. La proposition est conservée.")
            return false
        }
        mutate { copy(graphProposal = null) }
    }

    override fun updateGraphProposition(id: String, statement: String): Boolean = changeGraph { graph ->
        val old = graph.propositions.firstOrNull { it.id == id } ?: return@changeGraph null
        statement.trim().takeIf { it.isNotEmpty() && it.length <= 2_000 }?.let { graph.putProposition(old.copy(statement = it)) }
    }

    override fun deleteGraphProposition(id: String): Boolean = changeGraph { graph ->
        if (graph.propositions.none { it.id == id }) null else graph.deleteProposition(id)
    }

    override fun deleteGraphReasoning(id: String): Boolean = changeGraph { graph ->
        if (graph.reasonings.none { it.id == id }) null else graph.deleteReasoning(id)
    }

    private fun changeGraph(transform: (KnowledgeGraph) -> KnowledgeGraph?): Boolean = synchronized(lock) {
        val snapshot = state.value
        if (snapshot.busy || snapshot.storageError != null || snapshot.graphError != null || snapshot.graphProposal != null) return false
        val live = try { graphStore.load() } catch (_: Exception) {
            state.value = snapshot.copy(graphError = "Le graphe est illisible. Aucun changement n'a été enregistré.")
            return false
        }
        if (live != snapshot.knowledgeGraph) {
            state.value = snapshot.copy(knowledgeGraph = live, graphError = "Le graphe a changé ailleurs. Vérifie-le avant de modifier une proposition.")
            return false
        }
        val next = try { transform(live)?.validate() } catch (_: Exception) { null } ?: return false
        try { graphStore.save(next); state.value = snapshot.copy(knowledgeGraph = next, graphError = null); true }
        catch (_: Exception) { state.value = snapshot.copy(graphError = "Impossible d'enregistrer le graphe. Le fichier précédent est conservé."); false }
    }

    private fun graphSourceIndices(messages: List<StudioMessage>): Set<Int> = messages.indices.filterTo(mutableSetOf()) { index ->
        val message = messages[index]
        index >= (messages.size - 12).coerceAtLeast(0) && message.role == "assistant" && message.quiz == null && message.problem == null &&
            message.text.isNotBlank() && !message.text.startsWith("Diagnostic conclu :") && message.text != "Plan proposé. Ouvre Mon plan pédagogique pour examiner, corriger et accepter les étapes." &&
            message.text != "Retour de pratique disponible."
    }

    override fun revealPracticeHint(messageIndex: Int): Boolean {
        val snapshot = state.value
        val session = snapshot.sessions.firstOrNull { it.id == snapshot.selectedId } ?: return false
        val problem = session.messages.getOrNull(messageIndex)?.problem ?: return false
        if (snapshot.busy || snapshot.storageError != null || problem.result != null || problem.attempt != null || problem.shownHints >= problem.hints.size) return false
        return mutate { copy(sessions = sessions.map { current ->
            if (current.id != session.id) current else current.copy(messages = current.messages.mapIndexed { index, message ->
                if (index == messageIndex) message.copy(problem = problem.copy(shownHints = problem.shownHints + 1)) else message
            })
        }) }
    }

    override fun submitPracticeAttempt(messageIndex: Int, answer: String, thinking: String): Boolean {
        val snapshot = state.value
        val session = snapshot.sessions.firstOrNull { it.id == snapshot.selectedId } ?: return false
        val problem = session.messages.getOrNull(messageIndex)?.problem ?: return false
        if (problem.result != null || problem.attempt != null || answer.isBlank() || answer.length > 4_000 || thinking.length > 3_000) return false
        val attempt = StudioProblemAttempt(answer.trim(), thinking.trim(), System.currentTimeMillis())
        val text = buildString {
            append(STUDIO_PRACTICE_ATTEMPT_PREFIX).append("\nRéponse : ").append(attempt.answer)
            if (attempt.thinking.isNotBlank()) append("\nRaisonnement : ").append(attempt.thinking)
        }
        return sendTurn(text, null, ProblemSubmission(session.id, messageIndex, problem, attempt))
    }

    override fun surrenderPracticeProblem(messageIndex: Int): Boolean {
        val snapshot = state.value
        val session = snapshot.sessions.firstOrNull { it.id == snapshot.selectedId } ?: return false
        val problem = session.messages.getOrNull(messageIndex)?.problem ?: return false
        if (snapshot.busy || snapshot.storageError != null || pendingBySession.containsKey(session.id) || problem.result != null || problem.attempt != null) return false
        return mutate { copy(sessions = sessions.map { current ->
            if (current.id != session.id) current else current.copy(messages = current.messages.mapIndexed { index, message ->
                if (index == messageIndex) message.copy(problem = problem.copy(result = StudioProblemResult("surrendered", "Problème arrêté par l'apprenant."))) else message
            })
        }) }
    }

    override fun requestGeneratedPlan(): Boolean = synchronized(lock) {
        val session = state.value.sessions.firstOrNull { it.id == state.value.selectedId }
        if (session == null || session.messages.none { it.role == "user" && it.text !in (STUDIO_QUIZ_REQUESTS + setOf(STUDIO_PROBLEM_REQUEST, STUDIO_PLAN_REQUEST, STUDIO_DIAGNOSIS_REQUEST, STUDIO_GRAPH_REQUEST)) && !it.text.startsWith(STUDIO_PRACTICE_ATTEMPT_PREFIX) } ||
            session.messages.any { (it.quiz != null && it.quizAnswer == null) || (it.problem != null && it.problem.result == null) }) false
        else sendTurnLocked(STUDIO_PLAN_REQUEST, null, null)
    }

    override fun discardPlanProposal(): Boolean {
        val snapshot = state.value
        val id = snapshot.selectedId ?: return false
        if (snapshot.busy || snapshot.storageError != null) return false
        return mutate { copy(sessions = sessions.map { if (it.id == id) it.copy(planProposal = null) else it }) }
    }

    override fun submitQuiz(messageIndex: Int, selectedIds: List<String>, dontKnow: Boolean, thinking: String, confidence: Int?): Boolean {
        val snapshot = state.value
        val session = snapshot.sessions.firstOrNull { it.id == snapshot.selectedId } ?: return false
        val message = session.messages.getOrNull(messageIndex) ?: return false
        val quiz = message.quiz ?: return false
        if (message.role != "assistant" || message.quizAnswer != null || !validStudioQuiz(quiz) || thinking.length > 3_000 ||
            (confidence != null && confidence !in 0..100)) return false
        val ids = selectedIds.distinct()
        if (ids.size != selectedIds.size || ids.any { id -> quiz.options.none { it.id == id } } ||
            (dontKnow && ids.isNotEmpty()) || (!dontKnow && ids.isEmpty()) || (!quiz.multiSelect && ids.size > 1)) return false
        val answer = StudioQuizAnswer(ids, dontKnow, thinking.trim(), confidence, !dontKnow && ids.toSet() == quiz.correctIds.toSet(), System.currentTimeMillis())
        val selected = if (dontKnow) "Je ne sais pas" else quiz.options.filter { it.id in ids }.joinToString(" ; ") { "${it.label} (${it.id})" }
        val learnerMessage = buildString {
            append("Réponse à la question : ").append(selected)
            if (answer.thinking.isNotBlank()) append("\nMon raisonnement : ").append(answer.thinking)
            confidence?.let { append("\nConfiance : ").append(it).append(" %") }
        }
        return sendTurn(learnerMessage, QuizSubmission(session.id, messageIndex, quiz, answer), null)
    }

    private fun sendTurn(message: String, submission: QuizSubmission?, problemSubmission: ProblemSubmission?): Boolean = synchronized(lock) { sendTurnLocked(message, submission, problemSubmission) }

    private fun sendTurnLocked(message: String, submission: QuizSubmission?, problemSubmission: ProblemSubmission?): Boolean {
        val snapshot = state.value
        if (snapshot.busy || snapshot.storageError != null || message.isBlank() || message.length > (if (submission == null && problemSubmission == null) 4_000 else 14_000)) return false
        val session = snapshot.sessions.firstOrNull { it.id == snapshot.selectedId } ?: return false
        if (pendingBySession.containsKey(session.id)) return false
        val sessionConfig = effectiveSessionConfig(snapshot, session, message) ?: run {
            transient { copy(error = "Cette séance utilise un autre fournisseur. Rétablis sa connexion ou choisis le modèle actuel dans la séance.") }
            return false
        }
        val endpoint = if (sessionConfig.providerMode == "chatgpt") null else try { validateEndpoint(sessionConfig.baseUrl) } catch (_: Exception) { null }
        val secret = synchronized(apiKey) { apiKey.toString() }
        if (snapshot.providerMode == "chatgpt") {
            if (!chatGpt.connected || sessionConfig.model.isBlank()) {
                transient { copy(error = "Connecte ton compte ChatGPT et choisis un modèle dans Connexion IA.") }
                return false
            }
        } else if (endpoint == null || sessionConfig.model.isBlank() || (secret.isBlank() && !endpoint.isLoopback())) {
            transient { copy(error = "Configure une URL HTTPS, un modèle et une clé (sauf pour un service local).") }
            return false
        }
        val learnerMessage = message.trim()
        if (submission != null && (submission.sessionId != session.id || session.messages.getOrNull(submission.index)?.quiz != submission.quiz ||
                session.messages.getOrNull(submission.index)?.quizAnswer != null)) return false
        if (problemSubmission != null && (problemSubmission.sessionId != session.id || session.messages.getOrNull(problemSubmission.index)?.problem != problemSubmission.problem ||
                problemSubmission.problem.attempt != null || problemSubmission.problem.result != null)) return false
        val saved = mutate {
            copy(sessions = sessions.map { current ->
                if (current.id != session.id) current else {
                    val updatedMessages = current.messages.mapIndexed { index, item ->
                        when {
                            submission != null && index == submission.index -> item.copy(quizAnswer = submission.answer)
                            problemSubmission != null && index == problemSubmission.index -> item.copy(problem = problemSubmission.problem.copy(attempt = problemSubmission.attempt))
                            else -> item
                        }
                    }
                    current.copy(messages = updatedMessages + StudioMessage("user", learnerMessage))
                }
            }, error = null)
        }
        if (!saved) return false
        pendingBySession[session.id] = learnerMessage
        startRequest(session.id, sessionConfig.copy(sessions = state.value.sessions, selectedId = session.id), endpoint, secret)
        return true
    }

    override fun savePlan(plan: StudioPlan): Boolean {
        val snapshot = state.value
        val session = snapshot.sessions.firstOrNull { it.id == snapshot.selectedId } ?: return false
        if (snapshot.busy || snapshot.storageError != null || !validPlan(plan, session.messages.size)) return false
        return mutate { copy(sessions = sessions.map { if (it.id == session.id) it.copy(plan = plan, planProposal = null) else it }) }
    }

    override fun retry() {
        val current = state.value
        if (current.busy || current.storageError != null) return
        val id = current.selectedId ?: return
        if (pendingBySession[id].isNullOrBlank()) return
        val session = current.sessions.firstOrNull { it.id == id } ?: return
        val config = effectiveSessionConfig(current, session, session.messages.lastOrNull()?.text.orEmpty()) ?: run {
            transient { copy(error = "Cette séance utilise un autre fournisseur. Rétablis sa connexion ou choisis le modèle actuel dans la séance.") }
            return
        }
        val endpoint = try { validateEndpoint(config.baseUrl) } catch (_: Exception) { null }
        if (config.providerMode == "chatgpt") {
            if (!chatGpt.connected || config.model.isBlank()) {
                transient { copy(error = "Connecte ton compte ChatGPT et choisis un modèle pour reprendre cette séance.") }
                return
            }
            startRequest(id, config, null, "")
            return
        }
        if (endpoint == null || config.model.isBlank()) { transient { copy(error = "Configure une URL et un modèle valides pour reprendre cette séance.") }; return }
        val secret = synchronized(apiKey) { apiKey.toString() }
        if (secret.isBlank() && !endpoint.isLoopback()) {
            transient { copy(error = "Saisis à nouveau la clé du fournisseur pour reprendre cette séance.") }
            return
        }
        startRequest(id, config, endpoint, secret)
    }

    override fun cancel() {
        synchronized(lock) {
            if (!state.value.busy) return
            requestVersion += 1
            requestJob?.cancel()
            requestJob = null
            state.value = state.value.copy(busy = false, error = "Requête interrompue. Ton message est conservé; tu peux réessayer.")
        }
    }

    private fun startRequest(id: String, config: StudioState, endpoint: URI?, secret: String) {
        val session = config.sessions.firstOrNull { it.id == id } ?: return
        if (state.value.busy) return
        if (pendingBySession[id].isNullOrBlank()) return
        val acceptedPlan = session.plan?.takeIf { it.decision == "accepted" && validPlan(it, session.messages.size) }
        val effort = config.reasoningEffort
        val numberedContext = session.messages.lastOrNull()?.text in setOf(STUDIO_PLAN_REQUEST, STUDIO_DIAGNOSIS_REQUEST, STUDIO_GRAPH_REQUEST)
        val history = session.messages.takeLast(12).mapIndexed { offset, message ->
            if (!numberedContext) message else message.copy(
                text = "[message ${session.messages.size - minOf(12, session.messages.size) + offset}]\n${studioMessageForTutor(message)}",
                quiz = null,
            )
        }
        val request = endpoint?.let { TutorRequest(it, config.model, secret, session.goal, history, acceptedPlan, effort) }
        val version = synchronized(lock) {
            requestVersion += 1
            state.value = state.value.copy(busy = true, error = null)
            requestVersion
        }
        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                val reply = withTimeout(90_000) {
                    if (config.providerMode == "chatgpt") chatGpt.respond(config.model, session.goal, history, acceptedPlan, effort)
                    else tutor.respond(requireNotNull(request))
                }
                persistReply(id, reply, version)
            } catch (cancelled: CancellationException) {
                setFailure(version, "Requête interrompue. Ton message est conservé; tu peux réessayer.")
                throw cancelled
            } catch (failure: Exception) {
                val message = if (config.providerMode == "chatgpt") {
                    "${(failure as? ChatGptPlanException)?.safeMessage ?: "Impossible d’obtenir une réponse ChatGPT."} Ton message est conservé; tu peux réessayer."
                } else providerFailureMessage(failure)
                setFailure(version, message)
            } finally { synchronized(lock) { if (requestVersion == version) requestJob = null } }
        }
        synchronized(lock) {
            if (requestVersion == version) { requestJob = job; job.start() } else job.cancel()
        }
    }

    private fun persistReply(id: String, reply: String, version: Long) = synchronized(lock) {
        if (requestVersion != version) return@synchronized
        val before = state.value
        val current = before.sessions.firstOrNull { it.id == id }
        if (current == null) { state.value = before.copy(busy = false, error = "La séance n’existe plus."); return@synchronized }
        if (current.messages.lastOrNull()?.text == STUDIO_PROBLEM_REQUEST) {
            val problem = parseStudioProblem(reply)
            if (problem == null) {
                state.value = before.copy(busy = false, error = "Le fournisseur n’a pas produit de problème structuré utilisable. Ta demande est conservée ; réessaie ou choisis un autre modèle.")
                return@synchronized
            }
            val updated = current.copy(messages = current.messages + StudioMessage("assistant", "", problem = problem))
            val next = before.copy(sessions = before.sessions.map { if (it.id == id) updated else it }, busy = false, error = null)
            try { write(next); state.value = next; pendingBySession.remove(id) }
            catch (_: Exception) { state.value = before.copy(busy = false, storageError = "Impossible d’enregistrer le problème. Vérifie l’accès au dossier DreamsignWizard; les données précédentes sont conservées.") }
            return@synchronized
        }
        if (current.messages.lastOrNull()?.text == STUDIO_DIAGNOSIS_REQUEST) {
            val firstVisible = (current.messages.size - 12).coerceAtLeast(0)
            val answered = current.messages.indices.filterTo(mutableSetOf()) { index -> index >= firstVisible && current.messages[index].quizAnswer != null }
            val assessment = parseStudioDiagnostic(reply, answered)
            if (assessment == null) {
                state.value = before.copy(busy = false, error = "Le fournisseur n’a pas produit de diagnostic structuré utilisable. Ta demande est conservée ; réessaie ou pose d’autres questions de diagnostic.")
                return@synchronized
            }
            val updated = current.copy(assessment = assessment, messages = current.messages + StudioMessage("assistant", "Diagnostic conclu : ${assessment.summary}"))
            val next = before.copy(sessions = before.sessions.map { if (it.id == id) updated else it }, busy = false, error = null)
            try { write(next); state.value = next; pendingBySession.remove(id) }
            catch (_: Exception) { state.value = before.copy(busy = false, storageError = "Impossible d’enregistrer le diagnostic. Vérifie l’accès au dossier DreamsignWizard; les données précédentes sont conservées.") }
            return@synchronized
        }
        if (current.messages.lastOrNull()?.text == STUDIO_GRAPH_REQUEST) {
            val allowed = graphSourceIndices(current.messages)
            val proposal = runCatching {
                val extraction = KnowledgeGraphExtraction.parse(reply, allowed)
                KnowledgeGraphExtraction.propose(before.knowledgeGraph, extraction, id, allowed)
            }.getOrNull()
            if (proposal == null) {
                state.value = before.copy(busy = false, error = "Le fournisseur n'a pas produit d'extraction sourcée utilisable. Ta demande est conservée ; réessaie ou reformule le cours.")
                return@synchronized
            }
            val updated = current.copy(messages = current.messages + StudioMessage("assistant", "Extraction proposée. Ouvre le graphe de connaissances pour examiner et accepter les éléments."))
            val next = before.copy(sessions = before.sessions.map { if (it.id == id) updated else it }, graphProposal = proposal, busy = false, error = null)
            try { write(next); state.value = next; pendingBySession.remove(id) }
            catch (_: Exception) { state.value = before.copy(busy = false, storageError = "Impossible d'enregistrer la proposition de graphe. Les données précédentes sont conservées.") }
            return@synchronized
        }
        if (current.messages.lastOrNull()?.text?.startsWith(STUDIO_PRACTICE_ATTEMPT_PREFIX) == true) {
            val problemIndex = current.messages.indexOfLast { it.problem?.attempt != null && it.problem.result == null }
            val problem = current.messages.getOrNull(problemIndex)?.problem
            val result = problem?.let { parseStudioProblemVerdict(reply, it.shownHints) }
            if (problemIndex < 0 || result == null) {
                state.value = before.copy(busy = false, error = "Le fournisseur n’a pas produit de verdict structuré utilisable. Ta tentative est conservée ; réessaie ou choisis un autre modèle.")
                return@synchronized
            }
            val updated = current.copy(messages = current.messages.mapIndexed { index, message ->
                if (index == problemIndex) message.copy(problem = problem!!.copy(result = result)) else message
            } + StudioMessage("assistant", "Retour de pratique disponible."))
            val next = before.copy(sessions = before.sessions.map { if (it.id == id) updated else it }, busy = false, error = null)
            try { write(next); state.value = next; pendingBySession.remove(id) }
            catch (_: Exception) { state.value = before.copy(busy = false, storageError = "Impossible d’enregistrer le verdict. Vérifie l’accès au dossier DreamsignWizard; les données précédentes sont conservées.") }
            return@synchronized
        }
        val parsed = parseStudioTutorReply(reply)
        if ("```studio-problem" in reply || "```studio-verdict" in reply || "```studio-diagnosis" in reply || "\"propositions\"" in reply && "\"reasonings\"" in reply) {
            state.value = before.copy(busy = false, error = "Le fournisseur a renvoyé une structure de pratique hors séquence. Ton message est conservé ; réessaie.")
            return@synchronized
        }
        if (current.messages.lastOrNull()?.text == STUDIO_PLAN_REQUEST) {
            val allowedSources = current.messages.indices.filterTo(mutableSetOf()) { index ->
                index < current.messages.lastIndex && current.messages[index].text != STUDIO_PLAN_REQUEST
            }
            val generated = parseGeneratedPlan(reply, allowedSources)
            if (generated == null) {
                state.value = before.copy(busy = false, error = "Le fournisseur n’a pas produit de plan structuré utilisable. Ta demande est conservée ; réessaie ou choisis un autre modèle.")
                return@synchronized
            }
            val proposal = StudioPlanProposal(
                generated.diagnosis.map { StudioDiagnosisObservation(it.observation, it.sources) },
                StudioPlan(generated.steps.map { StudioPlanStep(it.title, it.prerequisites, it.evidence, it.sources) })
            )
            val updated = current.copy(planProposal = proposal, messages = current.messages + StudioMessage("assistant", "Plan proposé. Ouvre Mon plan pédagogique pour examiner, corriger et accepter les étapes."))
            val next = before.copy(sessions = before.sessions.map { if (it.id == id) updated else it }, busy = false, error = null)
            try { write(next); state.value = next; pendingBySession.remove(id) }
            catch (_: Exception) { state.value = before.copy(busy = false, storageError = "Impossible d’enregistrer le plan proposé. Vérifie l’accès au dossier DreamsignWizard; les données précédentes sont conservées.") }
            return@synchronized
        }
        if (current.messages.lastOrNull()?.text in STUDIO_QUIZ_REQUESTS && (parsed.quiz == null ||
            current.messages.lastOrNull()?.text == STUDIO_PREDICT_REQUEST && parsed.quiz.type != StudioQuizType.PREDICT ||
            current.messages.lastOrNull()?.text == STUDIO_CHECK_REQUEST && parsed.quiz.type != StudioQuizType.CHECK)) {
            state.value = before.copy(busy = false, error = "Le fournisseur n’a pas produit de QCM utilisable. Ton message est conservé ; réessaie ou choisis un autre modèle.")
            return@synchronized
        }
        val otherQuizOpen = current.messages.any { it.quiz != null && it.quizAnswer == null }
        val displayedReply = if (otherQuizOpen && parsed.quiz != null) {
            ParsedStudioReply("Le tuteur a proposé une autre question. Réponds d’abord au QCM encore ouvert.", null)
        } else parsed
        val updated = current.copy(messages = current.messages + StudioMessage("assistant", displayedReply.text, quiz = displayedReply.quiz))
        val next = before.copy(sessions = before.sessions.map { if (it.id == id) updated else it }, busy = false, error = null)
        try { write(next); state.value = next; pendingBySession.remove(id) }
        catch (_: Exception) { state.value = before.copy(busy = false, storageError = "Impossible d’enregistrer la réponse. Vérifie l’accès au dossier DreamsignWizard; les données précédentes sont conservées.") }
    }

    private fun setFailure(version: Long, message: String) = synchronized(lock) {
        if (requestVersion == version) state.value = state.value.copy(busy = false, error = message)
    }

    private fun mutate(transform: StudioState.() -> StudioState): Boolean = synchronized(lock) {
        val before = state.value
        val next = before.transform()
        try { write(next); state.value = next; true }
        catch (_: Exception) {
            state.value = before.copy(busy = false, storageError = "Impossible d’enregistrer les séances. Vérifie l’accès au dossier DreamsignWizard; le fichier précédent a été conservé.")
            false
        }
    }

    private fun transient(transform: StudioState.() -> StudioState) = synchronized(lock) { state.value = state.value.transform() }

    override fun connectChatGpt() {
        if (state.value.chatGptBusy || state.value.busy || state.value.storageError != null) return
        transient { copy(chatGptBusy = true, chatGptError = null) }
        scope.launch {
            try {
                val email = chatGpt.signIn()
                val models = chatGpt.models().map { it.slug }
                if (models.isEmpty()) error("Aucun modèle ChatGPT disponible pour ce compte.")
                val saved = mutate {
                    copy(providerMode = "chatgpt", model = chatGptModel.takeIf { it in models } ?: models.first(), chatGptModel = chatGptModel.takeIf { it in models } ?: models.first(), chatGptConnected = true,
                        chatGptEmail = email, chatGptModels = models, chatGptBusy = false, chatGptError = null)
                }
                if (!saved) transient { copy(chatGptBusy = false, chatGptError = "Connexion réussie, mais les préférences de l’atelier n’ont pas pu être enregistrées.") }
            } catch (_: Exception) {
                transient { copy(chatGptBusy = false, chatGptError = "Connexion ChatGPT impossible ou annulée. Vérifie le navigateur et réessaie.") }
            }
        }
    }

    override fun selectChatGptModel(model: String) {
        if (model !in state.value.chatGptModels || state.value.busy) return
        mutate { copy(providerMode = "chatgpt", model = model, chatGptModel = model, error = null) }
    }

    override fun disconnectChatGpt() {
        if (state.value.busy || state.value.chatGptBusy) return
        transient { copy(chatGptBusy = true) }
        scope.launch {
            try {
                val revoked = chatGpt.disconnect()
                mutate { copy(providerMode = "api", chatGptConnected = false, chatGptEmail = null, chatGptModels = emptyList(), model = apiModel, chatGptModel = "", chatGptBusy = false,
                    chatGptError = if (revoked) null else "Jetons locaux effacés. La révocation distante n’a pas été confirmée ; vérifie les connexions dans ChatGPT.") }
            } catch (_: Exception) {
                transient { copy(chatGptBusy = false, chatGptError = "Impossible d’effacer la connexion ChatGPT locale.") }
            }
        }
    }

    override fun openChatGptUsage() {
        try { java.awt.Desktop.getDesktop().browse(URI("https://chatgpt.com/settings/usage")) }
        catch (_: Exception) { transient { copy(chatGptError = "Impossible d’ouvrir les paramètres d’usage ChatGPT.") } }
    }

    private fun load(): StudioState = try {
        if (!Files.exists(file)) StudioState() else fromDisk(json.decodeFromString<StudioDisk>(Files.readString(file)))
    } catch (_: Exception) {
        StudioState(storageError = "Le fichier des séances est illisible. Il est conservé; corrige-le avant de continuer.")
    }

    private fun write(s: StudioState) {
        Files.createDirectories(file.parent)
        val disk = StudioDisk(s.baseUrl, s.model, s.sessions.map { it.toDisk() }, s.selectedId, s.providerMode, s.reasoningEffort.name, s.apiModel, s.chatGptModel, s.graphProposal, s.modelSlots.mapKeys { it.key.name }.mapValues { it.value.toDisk() }, s.modelOverrides.mapKeys { it.key.name }.mapValues { it.value.toDisk() })
        val temp = Files.createTempFile(file.parent, "studio-", ".tmp")
        try {
            Files.writeString(temp, json.encodeToString(disk))
            try { Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING) }
            catch (_: AtomicMoveNotSupportedException) { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING) }
        } finally { Files.deleteIfExists(temp) }
    }

    override fun close() { cancel(); synchronized(apiKey) { apiKey.setLength(0) }; scope.cancel() }
    private fun fromDisk(d: StudioDisk) = StudioState(baseUrl = d.baseUrl, model = d.model, sessions = d.sessions.map { it.fromDisk() }, selectedId = d.selectedId, providerMode = d.providerMode, reasoningEffort = ReasoningEffort.entries.firstOrNull { it.name == d.reasoningEffort } ?: ReasoningEffort.AUTOMATIC, apiModel = d.apiModel ?: if (d.providerMode == "api") d.model else "", chatGptModel = d.chatGptModel ?: if (d.providerMode == "chatgpt") d.model else "", graphProposal = d.graphProposal?.also { it.base.validate(); it.graph.validate() }, modelSlots = d.modelSlots.mapNotNull { (key, value) -> StudioModelSlot.entries.firstOrNull { it.name == key }?.let { it to value.fromDisk() } }.toMap(), modelOverrides = d.modelOverrides.mapNotNull { (key, value) -> StudioModelRole.entries.firstOrNull { it.name == key }?.let { it to value.fromDisk() } }.toMap())
    private fun StudioModelSetting.toDisk() = StudioModelSettingDisk(model?.let { StudioModelChoiceDisk(it.providerMode, it.baseUrl, it.model) }, reasoningEffort?.name)
    private fun StudioModelSettingDisk.fromDisk() = StudioModelSetting(model?.let { StudioModelChoice(it.providerMode, it.baseUrl, it.model) }, reasoningEffort?.let { name -> ReasoningEffort.entries.firstOrNull { it.name == name } })
    private fun StudioPlan.toDisk() = StudioPlanDisk(steps.map { step -> StudioPlanStepDisk(step.title, step.prerequisites, step.evidence, step.sources) }, decision)
    private fun StudioPlanDisk.fromDisk() = StudioPlan(steps.map { step -> StudioPlanStep(step.title, step.prerequisites, step.evidence, step.sources) }, decision)
    private fun StudioSession.toDisk() = StudioSessionDisk(id, goal, messages.map { StudioMessageDisk(it.role, it.text, it.quiz, it.quizAnswer, it.problem) }, plan?.toDisk(), reasoningEffort?.name, modelChoice?.let { StudioModelChoiceDisk(it.providerMode, it.baseUrl, it.model) }, planProposal?.let { StudioPlanProposalDisk(it.diagnosis.map { observation -> StudioDiagnosisDisk(observation.observation, observation.sources) }, it.plan.toDisk()) }, assessment)
    private fun StudioSessionDisk.fromDisk() = StudioSession(id, goal, messages.map { message ->
        require(message.quiz == null || validStudioQuiz(message.quiz))
        require(message.quiz == null || message.role == "assistant")
        require(message.quizAnswer == null || (message.quiz != null && validStudioQuizAnswer(message.quiz, message.quizAnswer)))
        require(message.problem == null || (message.role == "assistant" && message.quiz == null && validStudioProblem(message.problem)))
        StudioMessage(message.role, message.text, message.quiz, message.quizAnswer, message.problem)
    }, plan?.fromDisk(), reasoningEffort?.let { saved -> ReasoningEffort.entries.firstOrNull { it.name == saved } }, modelChoice?.let { StudioModelChoice(it.providerMode, it.baseUrl, it.model) }, planProposal?.let { StudioPlanProposal(it.diagnosis.map { observation -> StudioDiagnosisObservation(observation.observation, observation.sources) }, it.plan.fromDisk()) }, assessment?.also {
        require(validStudioDiagnostic(it, messages.indices.filterTo(mutableSetOf()) { index -> messages[index].quizAnswer != null }))
    })
}

private class OpenAiCompatibleTutor : StudioTutor {
    private val client = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build()

    override suspend fun respond(request: TutorRequest): String {
        val body = buildTutorBody(request.model, request.goal, request.history, request.acceptedPlan, request.reasoningEffort)
        val builder = HttpRequest.newBuilder(request.endpoint).timeout(java.time.Duration.ofSeconds(45)).header("Content-Type", "application/json")
        if (request.apiKey.isNotBlank()) builder.header("Authorization", "Bearer ${request.apiKey}")
        val response = withTimeout(45_000) {
            client.sendAsync(builder.POST(HttpRequest.BodyPublishers.ofString(body)).build(), boundedBodyHandler(256 * 1024)).await()
        }
        if (response.statusCode() !in 200..299) throw TutorHttpException(response.statusCode(), request.reasoningEffort)
        val content = Json.parseToJsonElement(response.body().decodeToString()).jsonObject["choices"]!!.jsonArray.first().jsonObject["message"]!!.jsonObject["content"]!!.jsonPrimitive.contentOrNull.orEmpty()
        require(content.isNotBlank() && content.length <= 20_000) { "provider returned an empty reply" }
        return content
    }
}

private class TutorHttpException(val status: Int, val effort: ReasoningEffort) : Exception()

private fun providerFailureMessage(failure: Exception): String {
    val status = (failure as? TutorHttpException)?.status
    val detail = when (status) {
        400 -> if ((failure as TutorHttpException).effort != ReasoningEffort.AUTOMATIC) "Requête refusée (400). Le modèle ou fournisseur peut ne pas prendre en charge ce niveau de réflexion ; choisis Automatique ou un niveau pris en charge." else "Requête refusée par le fournisseur (400). Vérifie sa configuration."
        401, 403 -> "Clé API refusée par le fournisseur. Vérifie-la dans Connexion IA."
        404 -> "Adresse ou modèle introuvable chez le fournisseur. Vérifie l’URL et le nom du modèle."
        429 -> "Limite de requêtes ou de crédit atteinte chez le fournisseur."
        else -> if (status != null && status in 500..599) "Le fournisseur est temporairement indisponible."
            else "Impossible d’obtenir une réponse du fournisseur. Vérifie la connexion et sa configuration."
    }
    return "$detail Ton message est conservé; tu peux réessayer."
}

internal const val MAX_TUTOR_BODY_CHARS = 60_000
internal const val TUTOR_SYSTEM_PROMPT = "Tu es un tuteur francophone. Commence par diagnostiquer les connaissances et le raisonnement de l’apprenant. Après sa réponse, propose les prérequis et un plan concis, puis enseigne progressivement en vérifiant sa compréhension. Sois honnête sur les limites et ne prétends pas avoir évalué une compétence sans preuve.\n" +
    "Pour chaque question de diagnostic ou de pratique, propose un QCM et réponds uniquement par un bloc structuré ```studio-quiz suivi d'un objet JSON puis ``` ; ne dévoile jamais la solution ou l'explication hors de ce bloc avant la réponse. " +
    "Le JSON doit contenir type (predict pour prédire un résultat avant explication, check pour vérifier la compréhension), question (énoncé complet autonome), options (tableau de 2 à 8 objets avec id unique court et label), multiSelect (booléen), correctIds (tableau non vide des id corrects) et explanation (explication complète). " +
    "Pour un seul choix correct, multiSelect vaut false et correctIds contient un seul id. Les labels ne contiennent pas la solution ni de raisonnement. " +
    "L'apprenant peut choisir, dire qu'il ne sait pas, indiquer sa confiance et expliquer librement son raisonnement. Après sa réponse, examine ce raisonnement et corrige les confusions avant de poursuivre. " +
    "Exception : si la dernière demande porte sur un problème de pratique ou son verdict, suis exclusivement le format studio-problem ou studio-verdict demandé, sans QCM. La solution du problème est préengagée avant sa présentation et doit rester cachée jusqu'à la conclusion de la tentative. " +
    "Si la dernière demande porte sur l'extraction du graphe, renvoie uniquement le JSON demandé, sans commentaire ni QCM ; ne présente jamais un résultat de quiz ou une maîtrise de l'apprenant comme une proposition factuelle."

/** Keeps recent whole turns by their encoded size, always retaining the goal and latest learner turn. */
internal fun acceptedPlanContext(plan: StudioPlan?): String = if (plan?.decision != "accepted") "" else buildString {
    append("\nPlan pédagogique accepté par l'élève. Suis les étapes dans l'ordre, vérifie les preuves attendues et ne déduis aucune maîtrise du seul plan :")
    plan.steps.forEachIndexed { index, step ->
        append("\n${index + 1}. ${step.title} | prérequis : ${step.prerequisites.joinToString { (it + 1).toString() }.ifBlank { "aucun" }} | preuve attendue : ${step.evidence}")
    }
}

internal fun buildTutorBody(model: String, goal: String, history: List<StudioMessage>, acceptedPlan: StudioPlan? = null, reasoningEffort: ReasoningEffort = ReasoningEffort.AUTOMATIC): String {
    val fixed = listOf(
        buildJsonObject { put("role", "system"); put("content", TUTOR_SYSTEM_PROMPT + acceptedPlanContext(acceptedPlan)) },
        buildJsonObject { put("role", "user"); put("content", "Objectif d’apprentissage : $goal") },
    )
    val newestFirst = history.takeLast(12).asReversed()
    val retained = mutableListOf<JsonObject>()
    fun encode(): String = buildJsonObject {
        put("model", model)
        reasoningEffort.wireValue?.let { put("reasoning_effort", it) }
        put("messages", buildJsonArray { (fixed + retained.asReversed()).forEach(::add) })
    }.toString()
    fun encodedTurn(turn: StudioMessage) = buildJsonObject {
        put("role", if (turn.role == "assistant") "assistant" else "user")
        put("content", studioMessageForTutor(turn))
    }
    newestFirst.firstOrNull()?.let { latest ->
        retained.add(encodedTurn(latest))
        require(encode().length <= MAX_TUTOR_BODY_CHARS) { "latest learner turn exceeds context limit" }
    }
    newestFirst.drop(1).forEach { turn ->
        val encoded = encodedTurn(turn)
        retained.add(encoded)
        if (encode().length > MAX_TUTOR_BODY_CHARS) retained.removeAt(retained.lastIndex)
    }
    val body = encode()
    require(body.length <= MAX_TUTOR_BODY_CHARS) { "goal and latest learner turn exceed context limit" }
    return body
}

private fun validateEndpoint(base: String): URI {
    val uri = URI(base)
    require(uri.userInfo == null && uri.query == null && uri.fragment == null && uri.host != null) { "invalid endpoint" }
    require(uri.path.orEmpty().split('/').none { it == ".." }) { "invalid endpoint path" }
    val local = uri.host.equals("localhost", true) || uri.host == "127.0.0.1" || uri.host == "::1" || uri.host == "[::1]"
    require(uri.scheme == "https" || (uri.scheme == "http" && local)) { "HTTPS required" }
    val prefix = base.trimEnd('/')
    val suffix = if (prefix.endsWith("/v1", true)) "/chat/completions" else "/v1/chat/completions"
    return URI(prefix + suffix)
}

private fun URI.isLoopback(): Boolean = host.equals("localhost", true) || host == "127.0.0.1" || host == "::1" || host == "[::1]"

private fun boundedBodyHandler(maxBytes: Int): HttpResponse.BodyHandler<ByteArray> = HttpResponse.BodyHandler {
    object : HttpResponse.BodySubscriber<ByteArray> {
        private val result = CompletableFuture<ByteArray>()
        private val output = java.io.ByteArrayOutputStream()
        private var subscription: Flow.Subscription? = null
        private var received = 0
        override fun getBody(): java.util.concurrent.CompletionStage<ByteArray> = result
        override fun onSubscribe(value: Flow.Subscription) { subscription = value; value.request(Long.MAX_VALUE) }
        override fun onNext(items: List<ByteBuffer>) {
            for (item in items) {
                val size = item.remaining()
                if (received + size > maxBytes) {
                    subscription?.cancel()
                    result.completeExceptionally(IllegalStateException("provider response too large"))
                    return
                }
                val bytes = ByteArray(size); item.get(bytes); output.write(bytes); received += size
            }
        }
        override fun onError(error: Throwable) { result.completeExceptionally(error) }
        override fun onComplete() { result.complete(output.toByteArray()) }
    }
}

private suspend fun <T> java.util.concurrent.CompletableFuture<T>.await(): T = suspendCancellableCoroutine { cont ->
    whenComplete { value, error -> if (error == null) cont.resume(value) else cont.resumeWithException(error) }
    cont.invokeOnCancellation { cancel(true) }
}

private fun defaultStudioFile(): Path {
    val base = System.getenv("LOCALAPPDATA")?.takeIf(String::isNotBlank) ?: System.getProperty("user.home")
    return Path.of(base, "DreamsignWizard", "studio.json")
}

internal fun validPlan(plan: StudioPlan, messageCount: Int): Boolean =
    plan.decision in setOf("proposed", "accepted", "diagnosis") && plan.steps.isNotEmpty() && plan.steps.size <= 20 &&
        plan.steps.withIndex().all { (index, step) ->
            step.title.isNotBlank() && step.title.length <= 500 && step.evidence.isNotBlank() && step.evidence.length <= 2000 &&
                step.prerequisites.distinct() == step.prerequisites && step.prerequisites.all { it in 0 until index } &&
                step.sources.isNotEmpty() && step.sources.distinct() == step.sources && step.sources.all { it in 0 until messageCount }
        }
