package com.dreamsigner.wizard.ui.studio

import kotlinx.serialization.Serializable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.runtime.staticCompositionLocalOf

enum class ReasoningEffort(val label: String, val wireValue: String?) {
    AUTOMATIC("Automatique", null), LOW("Faible", "low"), MEDIUM("Moyen", "medium"), HIGH("Élevé", "high")
}

data class StudioMessage(val role: String, val text: String, val quiz: StudioQuiz? = null, val quizAnswer: StudioQuizAnswer? = null, val problem: StudioProblem? = null)
data class StudioPlanStep(val title: String, val prerequisites: List<Int>, val evidence: String, val sources: List<Int>)
data class StudioPlan(val steps: List<StudioPlanStep>, val decision: String = "proposed")
data class StudioDiagnosisObservation(val observation: String, val sources: List<Int>)
data class StudioPlanProposal(val diagnosis: List<StudioDiagnosisObservation>, val plan: StudioPlan)
data class StudioModelChoice(val providerMode: String, val baseUrl: String, val model: String)
@Serializable
data class StudioStepEvidence(val stepIndex: Int, val messageIndex: Int)

fun StudioSession.evidenceSource(index: Int): Boolean {
    val message = messages.getOrNull(index) ?: return false
    return (message.quiz != null && message.quizAnswer?.correct == true && message.quizAnswer.thinking.isNotBlank()) ||
        (message.problem?.attempt?.thinking?.isNotBlank() == true && message.problem.result?.outcome?.let { it == "solved" || it == "solvedWithHints" } == true)
}

fun StudioSession.stepReady(index: Int): Boolean {
    val accepted = plan?.takeIf { it.decision == "accepted" } ?: return false
    val step = accepted.steps.getOrNull(index) ?: return false
    return step.prerequisites.all { prerequisite ->
        prerequisite in 0 until index && stepEvidence.any { it.stepIndex == prerequisite && evidenceSource(it.messageIndex) }
    }
}

enum class StudioSessionKind { TEACHING, PRACTICE }

data class StudioSession(val id: String, val goal: String, val messages: List<StudioMessage>, val plan: StudioPlan? = null, val reasoningEffort: ReasoningEffort? = null, val modelChoice: StudioModelChoice? = null, val planProposal: StudioPlanProposal? = null, val assessment: StudioDiagnosticAssessment? = null, val stepEvidence: List<StudioStepEvidence> = emptyList(), val kind: StudioSessionKind = StudioSessionKind.TEACHING)
data class StudioState(
    val sessions: List<StudioSession> = emptyList(), val selectedId: String? = null,
    val baseUrl: String = "", val model: String = "", val reasoningEffort: ReasoningEffort = ReasoningEffort.AUTOMATIC, val busy: Boolean = false,
    val error: String? = null, val storageError: String? = null, val available: Boolean = true,
    val connectionError: String? = null, val hasApiKey: Boolean = false,
    val providerMode: String = "api", val apiModel: String = "", val chatGptModel: String = "", val chatGptConnected: Boolean = false,
    val chatGptEmail: String? = null, val chatGptModels: List<String> = emptyList(),
    val chatGptBusy: Boolean = false, val chatGptError: String? = null,
    val apiModels: List<String> = emptyList(), val apiModelsUrl: String = "",
    val apiModelsBusy: Boolean = false, val apiModelsError: String? = null,
    val modelSlots: Map<StudioModelSlot, StudioModelSetting> = emptyMap(), val modelOverrides: Map<StudioModelRole, StudioModelSetting> = emptyMap(),
    val knowledgeGraph: KnowledgeGraph = KnowledgeGraph(), val graphProposal: KnowledgeGraphProposal? = null, val graphError: String? = null,
)

interface StudioController {
    val state: StateFlow<StudioState>
    fun select(id: String)
    fun create(goal: String): Boolean
    fun create(goal: String, kind: StudioSessionKind): Boolean = create(goal)
    fun configure(baseUrl: String, model: String, apiKey: String): Boolean
    fun discoverApiModels(baseUrl: String, apiKey: String) = Unit
    fun cancelApiModelDiscovery() = Unit
    fun setModelSlot(slot: StudioModelSlot, model: String, effort: ReasoningEffort?) = Unit
    fun setModelOverride(role: StudioModelRole, model: String, effort: ReasoningEffort?) = Unit
    fun setSessionModel(model: String?) = Unit
    fun setReasoningEffort(effort: ReasoningEffort) = Unit
    fun setSessionReasoningEffort(effort: ReasoningEffort?) = Unit
    fun send(message: String): Boolean
    fun requestQuiz(type: StudioQuizType = StudioQuizType.CHECK): Boolean = false
    fun submitQuiz(messageIndex: Int, selectedIds: List<String>, dontKnow: Boolean, thinking: String, confidence: Int?): Boolean = false
    fun requestPracticeProblem(): Boolean = false
    fun revealPracticeHint(messageIndex: Int): Boolean = false
    fun submitPracticeAttempt(messageIndex: Int, answer: String, thinking: String): Boolean = false
    fun surrenderPracticeProblem(messageIndex: Int): Boolean = false
    fun requestDiagnosisConclusion(): Boolean = false
    fun requestGraphExtraction(): Boolean = false
    fun importNotes(): Boolean = false
    fun acceptGraphProposal(): Boolean = false
    fun discardGraphProposal(): Boolean = false
    fun updateGraphProposition(id: String, statement: String): Boolean = false
    fun deleteGraphProposition(id: String): Boolean = false
    fun deleteGraphReasoning(id: String): Boolean = false
    fun refreshKnowledgeGraph(): Boolean = false
    fun requestGeneratedPlan(): Boolean = false
    fun discardPlanProposal(): Boolean = false
    fun savePlan(plan: StudioPlan): Boolean = false
    fun acceptStepEvidence(stepIndex: Int, messageIndex: Int): Boolean = false
    fun retry()
    fun cancel()
    fun connectChatGpt() = Unit
    fun selectChatGptModel(model: String) = Unit
    fun disconnectChatGpt() = Unit
    fun openChatGptUsage() = Unit
    fun close() = Unit
}

private class UnavailableStudioController : StudioController {
    override val state = MutableStateFlow(StudioState(available = false, error = "Le tuteur IA est actuellement disponible uniquement dans l’application Windows."))
    override fun select(id: String) = Unit
    override fun create(goal: String): Boolean = false
    override fun configure(baseUrl: String, model: String, apiKey: String): Boolean = false
    override fun send(message: String): Boolean { state.value = state.value.copy(error = "Le tuteur IA est disponible dans l’application Windows."); return false }
    override fun retry() = Unit
    override fun cancel() = Unit
}

val LocalStudioController = staticCompositionLocalOf<StudioController> { UnavailableStudioController() }
