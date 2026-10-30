@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package com.duolingo.app.ui.studio

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import com.duolingo.app.LearningAppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.skia.Image
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class StudioUiTest {
    @Test
    fun chatGptSignInIsVisibleInCompactConnectionDialog() = runDesktopComposeUiTest(width = 360, height = 480) {
        val controller = MemoryStudioController()
        setContent {
            LearningAppTheme {
                CompositionLocalProvider(LocalStudioController provides controller) { StudioScreen() }
            }
        }
        onNodeWithText("Connexion IA").performClick()
        onNodeWithText("Continuer avec ChatGPT").assertIsDisplayed()
        val file = File("build/reports/ui-snapshots/studio-connection-360.png")
        check(file.parentFile.isDirectory || file.parentFile.mkdirs())
        Image.makeFromBitmap(onAllNodes(isRoot()).onLast().captureToImage().asSkiaBitmap()).use { image ->
            image.encodeToData()!!.use { file.writeBytes(it.bytes) }
        }
    }

    @Test
    fun compactEmptyWorkspaceCanScrollToAndCreateItsFirstGoal() = runDesktopComposeUiTest(width = 360, height = 480) {
        val controller = MemoryStudioController()
        setContent {
            LearningAppTheme {
                CompositionLocalProvider(LocalStudioController provides controller) { StudioScreen() }
            }
        }

        onNodeWithText("Qu’aimerais-tu apprendre ?").performTextInput("Réviser les fractions")
        onNodeWithText("Commencer").performScrollTo().assertIsDisplayed().performClick()
        onNodeWithTag("studio-current-goal").assertTextEquals("Réviser les fractions")
        assertEquals("Réviser les fractions", controller.state.value.sessions.single().goal)
    }

    @Test
    fun learnersCanCreateChooseAndSendWithoutLosingDraftAfterFailure() = runDesktopComposeUiTest(width = 1000, height = 800) {
        val controller = MemoryStudioController()
        setContent {
            LearningAppTheme {
                CompositionLocalProvider(LocalStudioController provides controller) { StudioScreen() }
            }
        }

        onNodeWithText("Qu’aimerais-tu apprendre ?").performTextInput("Comprendre les séries")
        onNodeWithText("Commencer").performClick()
        onNodeWithTag("studio-current-goal").assertTextEquals("Comprendre les séries")
        onNodeWithTag("studio-draft").performTextInput("Je pense à une somme infinie")

        onNodeWithText("Envoyer").performClick()
        onNodeWithText("La connexion a échoué.").assertIsDisplayed()
        assertEquals("Je pense à une somme infinie", onNodeWithTag("studio-draft")
            .fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
        runOnIdle { assertEquals("Je pense à une somme infinie", controller.lastMessage); controller.acceptSend = true }
        onNodeWithText("Envoyer").performClick()
        onNodeWithText("Partons de ton raisonnement.").assertIsDisplayed()
        assertEquals("", onNodeWithTag("studio-draft").fetchSemanticsNode().config[SemanticsProperties.EditableText].text)

        onNodeWithText("Qu’aimerais-tu apprendre ?").performTextInput("Bases de Python")
        onNodeWithText("Commencer").performClick()
        onAllNodesWithText("Bases de Python").assertCountEquals(2)
        onNodeWithText("Comprendre les séries").performClick()
        onNodeWithText("Je pense à une somme infinie").assertIsDisplayed()
    }

    @Test
    fun providerDialogMasksAndClearsKeyAndUsesTheConfiguredModelSummary() = runDesktopComposeUiTest(width = 360, height = 480) {
        val controller = MemoryStudioController()
        setContent {
            LearningAppTheme {
                CompositionLocalProvider(LocalStudioController provides controller) { StudioScreen() }
            }
        }

        onNodeWithText("Connexion IA").performClick()
        onNodeWithText("URL de base").performTextInput("https://provider.example/v1")
        onNodeWithText("Modèle").performTextInput("local-model")
        onNodeWithTag("studio-api-key").performTextInput("SECRET_UI_KEY")
        val keySemantics = onNodeWithTag("studio-api-key").fetchSemanticsNode().config
        assertEquals(Unit, keySemantics[SemanticsProperties.Password])
        val transformedKey = keySemantics[SemanticsProperties.EditableText].text
        assertEquals("•".repeat("SECRET_UI_KEY".length), transformedKey)
        assertFalse("SECRET_UI_KEY" in transformedKey)
        onNodeWithText("Enregistrer").performClick()

        runOnIdle {
            assertEquals("SECRET_UI_KEY", controller.configuredKey)
            assertEquals("https://provider.example/v1", controller.state.value.baseUrl)
            assertEquals("local-model", controller.state.value.model)
        }
        onNodeWithText("https://provider.example/v1 · local-model").assertIsDisplayed()
        onNodeWithText("Connexion IA").performClick()
        assertEquals("", onNodeWithTag("studio-api-key").fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
        onNodeWithText("Fermer").performClick()
    }

    @Test
    fun busyStateDisablesSendingAndConfigurationAndOffersCancellation() = runDesktopComposeUiTest(width = 360, height = 480) {
        val controller = MemoryStudioController(initialSession = true)
        controller.state.value = controller.state.value.copy(busy = true)
        setContent {
            LearningAppTheme {
                CompositionLocalProvider(LocalStudioController provides controller) { StudioScreen() }
            }
        }

        onNodeWithText("Connexion IA").assertIsNotEnabled()
        onNodeWithTag("studio-draft").assertIsNotEnabled()
        onNodeWithTag("studio-send").assertIsNotEnabled()
        onNodeWithText("Annuler").performClick()
        runOnIdle { assertEquals(1, controller.cancelCount) }
    }

    @Test
    fun sampleConversationRendersAtPhoneAndDesktopSizes() {
        listOf(360 to 480, 1000 to 800).forEach { (width, height) ->
            runDesktopComposeUiTest(width = width, height = height) {
                val controller = MemoryStudioController(initialSession = true, sampleMessages = true)
                setContent {
                    LearningAppTheme {
                        CompositionLocalProvider(LocalStudioController provides controller) { StudioScreen() }
                    }
                }
                onNodeWithTag("studio-workspace").assertIsDisplayed()
                onNodeWithTag("studio-conversation-messages")
                    .performScrollToNode(hasText("On étudie la limite de ses sommes partielles."))
                onNodeWithText("On étudie la limite de ses sommes partielles.").assertIsDisplayed()
                onNodeWithTag("studio-conversation-messages")
                    .performScrollToNode(hasText("Qu’est-ce qu’une série convergente ?"))
                onNodeWithText("Qu’est-ce qu’une série convergente ?").assertIsDisplayed()
                onNodeWithTag("studio-draft").assertIsDisplayed()
                val file = File("build/reports/ui-snapshots/studio-$width.png")
                check(file.parentFile.isDirectory || file.parentFile.mkdirs())
                Image.makeFromBitmap(onNodeWithTag("studio-workspace").captureToImage().asSkiaBitmap()).use { image ->
                    image.encodeToData()!!.use { file.writeBytes(it.bytes) }
                }
                val screenFile = File("build/reports/ui-snapshots/studio-screen-$width.png")
                Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap()).use { image ->
                    image.encodeToData()!!.use { screenFile.writeBytes(it.bytes) }
                }
            }
        }
    }

    @Test
    fun unsupportedPlatformExplainsAndDisablesProviderAndGoalActions() = runDesktopComposeUiTest(width = 360, height = 480) {
        val controller = MemoryStudioController().apply {
            state.value = state.value.copy(available = false, error = "Le tuteur IA est disponible uniquement dans l’application Windows.")
        }
        setContent {
            LearningAppTheme {
                CompositionLocalProvider(LocalStudioController provides controller) { StudioScreen() }
            }
        }
        onNodeWithTag("studio-unavailable").assertIsDisplayed()
        onNodeWithText("Connexion IA").assertIsNotEnabled()
        onNodeWithText("Commencer").assertIsNotEnabled()
    }

    private class MemoryStudioController(initialSession: Boolean = false, sampleMessages: Boolean = false) : StudioController {
        private var nextId = 1
        private val startingSessions = if (initialSession) listOf(
            StudioSession(
                id = "session-0", goal = "Comprendre les séries",
                messages = if (sampleMessages) listOf(
                    StudioMessage("user", "Qu’est-ce qu’une série convergente ?"),
                    StudioMessage("assistant", "Une série est la somme des termes d’une suite.\n\n## L’idée clé\nOn étudie la limite de ses sommes partielles."),
                ) else emptyList(),
            ),
        ) else emptyList()

        override val state = MutableStateFlow(
            StudioState(
                sessions = startingSessions,
                selectedId = startingSessions.firstOrNull()?.id,
                baseUrl = if (initialSession) "http://127.0.0.1:8080/v1" else "",
                model = if (initialSession) "local-model" else "",
            ),
        )
        var acceptSend = false
        var lastMessage: String? = null
        var configuredKey: String? = null
        var cancelCount = 0
        override fun select(id: String) { state.value = state.value.copy(selectedId = id, error = null) }
        override fun create(goal: String): Boolean {
            if (goal.isBlank() || !state.value.available) return false
            val session = StudioSession("session-${nextId++}", goal, emptyList())
            state.value = state.value.copy(sessions = listOf(session) + state.value.sessions, selectedId = session.id, error = null)
            return true
        }
        override fun configure(baseUrl: String, model: String, apiKey: String): Boolean {
            if (baseUrl.isBlank() || model.isBlank() || !state.value.available) return false
            configuredKey = apiKey
            state.value = state.value.copy(baseUrl = baseUrl, model = model)
            return true
        }
        override fun send(message: String): Boolean {
            lastMessage = message
            if (!acceptSend) {
                state.value = state.value.copy(error = "La connexion a échoué.")
                return false
            }
            val session = state.value.sessions.firstOrNull { it.id == state.value.selectedId } ?: return false
            val updated = session.copy(messages = session.messages + StudioMessage("user", message) + StudioMessage("assistant", "Partons de ton raisonnement."))
            state.value = state.value.copy(sessions = state.value.sessions.map { if (it.id == updated.id) updated else it }, error = null)
            return true
        }
        override fun retry() = Unit
        override fun cancel() { cancelCount++; state.value = state.value.copy(busy = false) }
    }
}
