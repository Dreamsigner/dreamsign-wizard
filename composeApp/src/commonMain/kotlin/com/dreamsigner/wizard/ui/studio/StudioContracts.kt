package com.dreamsigner.wizard.ui.studio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.runtime.staticCompositionLocalOf

enum class ReasoningEffort(val label: String, val wireValue: String?) {
    AUTOMATIC("Automatique", null), LOW("Faible", "low"), MEDIUM("Moyen", "medium"), HIGH("Élevé", "high")
}

data class StudioMessage(val role: String, val text: String)
data class StudioPlanStep(val title: String, val prerequisites: List<Int>, val evidence: String, val sources: List<Int>)
data class StudioPlan(val steps: List<StudioPlanStep>, val decision: String = "proposed")
data class StudioSession(val id: String, val goal: String, val messages: List<StudioMessage>, val plan: StudioPlan? = null, val reasoningEffort: ReasoningEffort? = null)
data class StudioState(
    val sessions: List<StudioSession> = emptyList(), val selectedId: String? = null,
    val baseUrl: String = "", val model: String = "", val reasoningEffort: ReasoningEffort = ReasoningEffort.AUTOMATIC, val busy: Boolean = false,
    val error: String? = null, val storageError: String? = null, val available: Boolean = true,
    val connectionError: String? = null, val hasApiKey: Boolean = false,
    val providerMode: String = "api", val chatGptConnected: Boolean = false,
    val chatGptEmail: String? = null, val chatGptModels: List<String> = emptyList(),
    val chatGptBusy: Boolean = false, val chatGptError: String? = null,
)

interface StudioController {
    val state: StateFlow<StudioState>
    fun select(id: String)
    fun create(goal: String): Boolean
    fun configure(baseUrl: String, model: String, apiKey: String): Boolean
    fun setReasoningEffort(effort: ReasoningEffort) = Unit
    fun setSessionReasoningEffort(effort: ReasoningEffort?) = Unit
    fun send(message: String): Boolean
    fun savePlan(plan: StudioPlan): Boolean = false
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
