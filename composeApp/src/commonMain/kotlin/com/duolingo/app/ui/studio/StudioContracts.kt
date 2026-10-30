package com.duolingo.app.ui.studio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.runtime.staticCompositionLocalOf

data class StudioMessage(val role: String, val text: String)
data class StudioSession(val id: String, val goal: String, val messages: List<StudioMessage>)
data class StudioState(
    val sessions: List<StudioSession> = emptyList(), val selectedId: String? = null,
    val baseUrl: String = "", val model: String = "", val busy: Boolean = false,
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
    fun send(message: String): Boolean
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
