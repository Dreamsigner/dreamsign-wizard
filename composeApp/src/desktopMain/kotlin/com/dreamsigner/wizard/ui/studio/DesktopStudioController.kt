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

@Serializable private data class StudioDisk(val baseUrl: String = "", val model: String = "", val sessions: List<StudioSessionDisk> = emptyList(), val selectedId: String? = null, val providerMode: String = "api", val reasoningEffort: String = "AUTOMATIC")
@Serializable private data class StudioSessionDisk(val id: String, val goal: String, val messages: List<StudioMessageDisk>, val plan: StudioPlanDisk? = null, val reasoningEffort: String? = null)
@Serializable private data class StudioPlanDisk(val steps: List<StudioPlanStepDisk>, val decision: String = "proposed")
@Serializable private data class StudioPlanStepDisk(val title: String, val prerequisites: List<Int>, val evidence: String, val sources: List<Int>)
@Serializable private data class StudioMessageDisk(val role: String, val text: String)

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
    private val pendingBySession = ConcurrentHashMap<String, String>()
    private val chatGpt = ChatGptPlanClient(file.resolveSibling("chatgpt-plan.json"))
    override val state = MutableStateFlow(load().copy(chatGptConnected = chatGpt.connected, chatGptEmail = chatGpt.accountEmail, chatGptError = chatGpt.storageError))

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
        val session = StudioSession(UUID.randomUUID().toString(), goal.trim(), emptyList())
        return mutate { copy(sessions = listOf(session) + sessions, selectedId = session.id, error = null) }
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
        if (apiKey.isBlank() && !endpoint.isLoopback()) {
            transient { copy(connectionError = "Saisis la clé API du fournisseur distant.") }
            return false
        }
        val saved = mutate { copy(baseUrl = normalizedUrl, model = model.trim(), providerMode = "api", error = null, connectionError = null, hasApiKey = apiKey.isNotBlank()) }
        if (saved) synchronized(this.apiKey) { this.apiKey.setLength(0); this.apiKey.append(apiKey.trim()) }
        return saved
    }

    override fun send(message: String): Boolean {
        val snapshot = state.value
        if (snapshot.busy || snapshot.storageError != null || message.isBlank() || message.length > 4_000) return false
        val session = snapshot.sessions.firstOrNull { it.id == snapshot.selectedId } ?: return false
        if (pendingBySession.containsKey(session.id)) return false
        val endpoint = if (snapshot.providerMode == "chatgpt") null else try { validateEndpoint(snapshot.baseUrl) } catch (_: Exception) { null }
        val secret = synchronized(apiKey) { apiKey.toString() }
        if (snapshot.providerMode == "chatgpt") {
            if (!chatGpt.connected || snapshot.model.isBlank()) {
                transient { copy(error = "Connecte ton compte ChatGPT et choisis un modèle dans Connexion IA.") }
                return false
            }
        } else if (endpoint == null || snapshot.model.isBlank() || (secret.isBlank() && !endpoint.isLoopback())) {
            transient { copy(error = "Configure une URL HTTPS, un modèle et une clé (sauf pour un service local).") }
            return false
        }
        val learnerMessage = message.trim()
        val saved = mutate {
            copy(sessions = sessions.map { if (it.id == session.id) it.copy(messages = it.messages + StudioMessage("user", learnerMessage)) else it }, error = null)
        }
        if (!saved) return false
        pendingBySession[session.id] = learnerMessage
        startRequest(session.id, snapshot.copy(sessions = state.value.sessions, selectedId = session.id), endpoint, secret)
        return true
    }

    override fun savePlan(plan: StudioPlan): Boolean {
        val snapshot = state.value
        val session = snapshot.sessions.firstOrNull { it.id == snapshot.selectedId } ?: return false
        if (snapshot.busy || snapshot.storageError != null || !validPlan(plan, session.messages.size)) return false
        return mutate { copy(sessions = sessions.map { if (it.id == session.id) it.copy(plan = plan) else it }) }
    }

    override fun retry() {
        val current = state.value
        if (current.busy || current.storageError != null) return
        val id = current.selectedId ?: return
        if (pendingBySession[id].isNullOrBlank()) return
        val endpoint = try { validateEndpoint(current.baseUrl) } catch (_: Exception) { null }
        if (current.providerMode == "chatgpt") {
            if (!chatGpt.connected || current.model.isBlank()) {
                transient { copy(error = "Connecte ton compte ChatGPT et choisis un modèle pour reprendre cette séance.") }
                return
            }
            startRequest(id, current, null, "")
            return
        }
        if (endpoint == null || current.model.isBlank()) { transient { copy(error = "Configure une URL et un modèle valides pour reprendre cette séance.") }; return }
        val secret = synchronized(apiKey) { apiKey.toString() }
        if (secret.isBlank() && !endpoint.isLoopback()) {
            transient { copy(error = "Saisis à nouveau la clé du fournisseur pour reprendre cette séance.") }
            return
        }
        startRequest(id, current, endpoint, secret)
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
        val effort = session.reasoningEffort ?: config.reasoningEffort
        val request = endpoint?.let { TutorRequest(it, config.model, secret, session.goal, session.messages.takeLast(12), acceptedPlan, effort) }
        val version = synchronized(lock) {
            requestVersion += 1
            state.value = state.value.copy(busy = true, error = null)
            requestVersion
        }
        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                val reply = withTimeout(90_000) {
                    if (config.providerMode == "chatgpt") chatGpt.respond(config.model, session.goal, session.messages, acceptedPlan, effort)
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
        val updated = current.copy(messages = current.messages + StudioMessage("assistant", reply))
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
                    copy(providerMode = "chatgpt", model = models.first(), chatGptConnected = true,
                        chatGptEmail = email, chatGptModels = models, chatGptBusy = false, chatGptError = null)
                }
                if (!saved) transient { copy(chatGptBusy = false, chatGptError = "Connexion réussie, mais les préférences de l’atelier n’ont pas pu être enregistrées.") }
            } catch (_: Exception) {
                transient { copy(chatGptBusy = false, chatGptError = "Connexion ChatGPT impossible ou annulée. Vérifie le navigateur et réessaie.") }
            }
        }
    }

    override fun selectChatGptModel(model: String) {
        if (state.value.providerMode != "chatgpt" || model !in state.value.chatGptModels || state.value.busy) return
        mutate { copy(model = model) }
    }

    override fun disconnectChatGpt() {
        if (state.value.busy || state.value.chatGptBusy) return
        transient { copy(chatGptBusy = true) }
        scope.launch {
            try {
                val revoked = chatGpt.disconnect()
                mutate { copy(providerMode = "api", chatGptConnected = false, chatGptEmail = null, chatGptModels = emptyList(), model = "", chatGptBusy = false,
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
        val disk = StudioDisk(s.baseUrl, s.model, s.sessions.map { it.toDisk() }, s.selectedId, s.providerMode, s.reasoningEffort.name)
        val temp = Files.createTempFile(file.parent, "studio-", ".tmp")
        try {
            Files.writeString(temp, json.encodeToString(disk))
            try { Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING) }
            catch (_: AtomicMoveNotSupportedException) { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING) }
        } finally { Files.deleteIfExists(temp) }
    }

    override fun close() { cancel(); synchronized(apiKey) { apiKey.setLength(0) }; scope.cancel() }
    private fun fromDisk(d: StudioDisk) = StudioState(baseUrl = d.baseUrl, model = d.model, sessions = d.sessions.map { it.fromDisk() }, selectedId = d.selectedId, providerMode = d.providerMode, reasoningEffort = ReasoningEffort.entries.firstOrNull { it.name == d.reasoningEffort } ?: ReasoningEffort.AUTOMATIC)
    private fun StudioSession.toDisk() = StudioSessionDisk(id, goal, messages.map { StudioMessageDisk(it.role, it.text) }, plan?.let { StudioPlanDisk(it.steps.map { step -> StudioPlanStepDisk(step.title, step.prerequisites, step.evidence, step.sources) }, it.decision) }, reasoningEffort?.name)
    private fun StudioSessionDisk.fromDisk() = StudioSession(id, goal, messages.map { StudioMessage(it.role, it.text) }, plan?.let { StudioPlan(it.steps.map { step -> StudioPlanStep(step.title, step.prerequisites, step.evidence, step.sources) }, it.decision) }, reasoningEffort?.let { saved -> ReasoningEffort.entries.firstOrNull { it.name == saved } })
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
internal const val TUTOR_SYSTEM_PROMPT = "Tu es un tuteur francophone. Commence par diagnostiquer les connaissances et le raisonnement de l’apprenant. Après sa réponse, propose les prérequis et un plan concis, puis enseigne progressivement en vérifiant sa compréhension. Sois honnête sur les limites et ne prétends pas avoir évalué une compétence sans preuve."

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
        put("content", turn.text)
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
