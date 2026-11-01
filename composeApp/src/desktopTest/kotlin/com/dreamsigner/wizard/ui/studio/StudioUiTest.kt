@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package com.dreamsigner.wizard.ui.studio

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import com.dreamsigner.wizard.LearningAppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.skia.Image
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class StudioUiTest {
    @Test
    fun knowledgeGraphShowsSavedPropositionAndSourceAtCompactSize() = runDesktopComposeUiTest(width = 360, height = 480) {
        val controller = MemoryStudioController(initialSession = true)
        val sessionId = controller.state.value.sessions.single().id
        controller.state.value = controller.state.value.copy(knowledgeGraph = KnowledgeGraph(propositions = listOf(
            KnowledgeProposition("p1", "Une moitié vaut 1/2.", listOf(KnowledgeSource(sessionId, 0))),
        )))
        setContent { LearningAppTheme { CompositionLocalProvider(LocalStudioController provides controller) { StudioScreen() } } }
        onNodeWithTag("studio-graph").performClick()
        onNodeWithText("Graphe de connaissances").assertIsDisplayed()
        onNodeWithText("Une moitié vaut 1/2.").assertIsDisplayed()
        val file = File("build/reports/ui-snapshots/studio-graph-360.png")
        check(file.parentFile.isDirectory || file.parentFile.mkdirs())
        Image.makeFromBitmap(onAllNodes(isRoot()).onLast().captureToImage().asSkiaBitmap()).use { image ->
            image.encodeToData()!!.use { file.writeBytes(it.bytes) }
        }
    }

    @Test
    fun reviewedDiagnosticIsVisibleInPlanDialog() = runDesktopComposeUiTest(width = 360, height = 480) {
        val controller = MemoryStudioController(initialSession = true).apply { installAssessment() }
        setContent { LearningAppTheme { CompositionLocalProvider(LocalStudioController provides controller) { StudioScreen() } } }
        onNodeWithTag("studio-plan").performClick()
        onNodeWithText("Diagnostic de la séance").assertIsDisplayed()
        onNodeWithText("Les moitiés sont comprises.").assertIsDisplayed()
        onNodeWithText("Conclure le diagnostic").assertIsDisplayed()
        val file = File("build/reports/ui-snapshots/studio-diagnostic-360.png")
        check(file.parentFile.isDirectory || file.parentFile.mkdirs())
        Image.makeFromBitmap(onAllNodes(isRoot()).onLast().captureToImage().asSkiaBitmap()).use { image ->
            image.encodeToData()!!.use { file.writeBytes(it.bytes) }
        }
    }

    @Test
    fun planEditorOpensAtCompactSize() = runDesktopComposeUiTest(width = 360, height = 480) {
        val controller = MemoryStudioController(initialSession = true)
        setContent {
            LearningAppTheme {
                CompositionLocalProvider(LocalStudioController provides controller) { StudioScreen() }
            }
        }
        onNodeWithTag("studio-plan").performClick()
        onNodeWithText("Mon plan pédagogique").assertIsDisplayed()
        onNodeWithText("Préparer une demande de diagnostic").performScrollTo()
        val file = File("build/reports/ui-snapshots/studio-plan-360.png")
        check(file.parentFile.isDirectory || file.parentFile.mkdirs())
        Image.makeFromBitmap(onAllNodes(isRoot()).onLast().captureToImage().asSkiaBitmap()).use { image ->
            image.encodeToData()!!.use { file.writeBytes(it.bytes) }
        }
        onNodeWithText("Préparer une demande de diagnostic").assertIsDisplayed()
        onNodeWithText("Préparer une demande de diagnostic").performClick()
        assertEquals("Peux-tu approfondir le diagnostic de mes prérequis avant de poursuivre le plan ?",
            onNodeWithTag("studio-draft").fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
    }

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
        onNodeWithText("Enregistrer la connexion API").performClick()

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
                    .performScrollToIndex(1)
                if (width == 360) onNodeWithTag("studio-conversation-messages").performTouchInput { swipeUp() }
                onNodeWithText("On étudie la limite de ses sommes partielles.").assertIsDisplayed()
                onNodeWithTag("studio-conversation-messages")
                    .performScrollToIndex(0)
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
    fun quizRequiresAChoiceKeepsThinkingOnFailureAndRevealsTheCorrectionAfterSubmission() = runDesktopComposeUiTest(width = 1000, height = 800) {
        val controller = MemoryStudioController(initialSession = true)
        controller.installQuiz()
        setContent {
            LearningAppTheme {
                CompositionLocalProvider(LocalStudioController provides controller) { StudioScreen() }
            }
        }

        onNodeWithTag("studio-request-quiz").assertDoesNotExist()
        onNodeWithTag("studio-quiz-submit").performScrollTo().assertIsNotEnabled()
        onNodeWithTag("studio-quiz-explanation").assertDoesNotExist()
        onNodeWithTag("studio-quiz-option-b").performScrollTo().performClick().assertIsSelected()
        onNodeWithTag("studio-quiz-thinking").performScrollTo().performTextInput("Je divise le tout en deux parts égales.")
        onNodeWithTag("studio-quiz-clarification").performClick()
        onNodeWithTag("studio-draft").performTextInput("Pourquoi deux parts ?")
        onNodeWithTag("studio-quiz-clarification").performClick()
        onNodeWithTag("studio-quiz-option-b").performScrollTo().assertIsSelected()
        assertEquals("Je divise le tout en deux parts égales.", onNodeWithTag("studio-quiz-thinking").performScrollTo()
            .fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
        onNodeWithTag("studio-quiz-confidence").performScrollTo().performClick()
        onNodeWithText("75 %").performClick()
        onNodeWithTag("studio-quiz-submit").performScrollTo().performClick()
        onNodeWithTag("studio-quiz-option-b").performScrollTo().assertIsSelected()
        assertEquals("Je divise le tout en deux parts égales.", onNodeWithTag("studio-quiz-thinking").performScrollTo()
            .fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
        runOnIdle {
            assertEquals(listOf("b"), controller.quizSelection)
            assertEquals("Je divise le tout en deux parts égales.", controller.quizThinking)
            assertEquals(75, controller.quizConfidence)
            controller.acceptQuiz = true
        }
        onNodeWithTag("studio-quiz-submit").performScrollTo().performClick()
        onNodeWithTag("studio-quiz-result").performScrollTo().assertIsDisplayed()
        onNodeWithText("Bonne réponse").assertIsDisplayed()
        onNodeWithTag("studio-quiz-explanation").performScrollTo().assertIsDisplayed()
        onNodeWithText("Une moitié correspond à une des deux parts égales.").assertIsDisplayed()
    }

    @Test
    fun learnerCanRequestAQuizFromTheConversation() = runDesktopComposeUiTest(width = 1000, height = 800) {
        val controller = MemoryStudioController(initialSession = true)
        setContent {
            LearningAppTheme {
                CompositionLocalProvider(LocalStudioController provides controller) { StudioScreen() }
            }
        }
        onNodeWithTag("studio-request-quiz").assertIsEnabled().performClick()
        runOnIdle { assertEquals(1, controller.quizRequests) }
    }

    @Test
    fun quizSupportsDontKnowAndRendersAtPhoneAndDesktopSizes() {
        listOf(360 to 480, 1000 to 800).forEach { (width, height) ->
            runDesktopComposeUiTest(width = width, height = height) {
                val controller = MemoryStudioController(initialSession = true).apply { installQuiz(); acceptQuiz = true }
                setContent {
                    LearningAppTheme {
                        CompositionLocalProvider(LocalStudioController provides controller) { StudioScreen() }
                    }
                }
                val initialFile = File("build/reports/ui-snapshots/studio-quiz-$width-initial.png")
                check(initialFile.parentFile.isDirectory || initialFile.parentFile.mkdirs())
                Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap()).use { image ->
                    image.encodeToData()!!.use { initialFile.writeBytes(it.bytes) }
                }
                onNodeWithTag("studio-quiz-option-a").performScrollTo().assertIsDisplayed()
                val file = File("build/reports/ui-snapshots/studio-quiz-$width.png")
                check(file.parentFile.isDirectory || file.parentFile.mkdirs())
                Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap()).use { image ->
                    image.encodeToData()!!.use { file.writeBytes(it.bytes) }
                }
                onNodeWithTag("studio-quiz-dont-know").performScrollTo().performClick().assertIsSelected()
                onNodeWithTag("studio-quiz-thinking").performScrollTo().assertIsDisplayed()
                val reasoningFile = File("build/reports/ui-snapshots/studio-quiz-$width-reasoning.png")
                Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap()).use { image ->
                    image.encodeToData()!!.use { reasoningFile.writeBytes(it.bytes) }
                }
                onNodeWithTag("studio-quiz-submit").performScrollTo().assertIsEnabled().performClick()
                runOnIdle { assertEquals(true, controller.quizDontKnow); assertEquals(emptyList(), controller.quizSelection) }
                onNodeWithTag("studio-quiz-result").performScrollTo().assertIsDisplayed()
                onNodeWithText("Réponse à découvrir").assertIsDisplayed()
            }
        }
    }

    @Test
    fun practiceHidesCommittedSolutionUntilAnswerAtCompactSize() = runDesktopComposeUiTest(width = 360, height = 480) {
        val controller = MemoryStudioController(initialSession = true).apply { installProblem() }
        setContent { LearningAppTheme { CompositionLocalProvider(LocalStudioController provides controller) { StudioScreen() } } }
        onNodeWithTag("studio-problem-solution").assertDoesNotExist()
        onNodeWithTag("studio-problem-hint").performScrollTo().performClick()
        onNodeWithTag("studio-problem-hint-0").performScrollTo().assertIsDisplayed()
        onNodeWithTag("studio-problem-answer").performScrollTo().performTextInput("4")
        onNodeWithTag("studio-problem-thinking").performScrollTo().performTextInput("Deux paires font quatre")
        onNodeWithTag("studio-problem-submit").performScrollTo().performClick()
        onNodeWithTag("studio-problem-solution").performScrollTo().assertIsDisplayed()
        runOnIdle { assertEquals("Deux paires font quatre", controller.problemThinking) }
        val file = File("build/reports/ui-snapshots/studio-problem-360-result.png")
        check(file.parentFile.isDirectory || file.parentFile.mkdirs())
        Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap()).use { image ->
            image.encodeToData()!!.use { file.writeBytes(it.bytes) }
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
        var acceptQuiz = false
        var quizRequests = 0
        var quizSelection: List<String>? = null
        var quizDontKnow: Boolean? = null
        var quizThinking: String? = null
        var quizConfidence: Int? = null
        var problemThinking: String? = null
        fun installQuiz() {
            val session = state.value.sessions.single()
            val quiz = StudioQuiz("Quelle fraction représente une moitié ?", listOf(StudioQuizOption("a", "Un tiers"), StudioQuizOption("b", "Une moitié")),
                correctIds = listOf("b"), explanation = "Une moitié correspond à une des deux parts égales.")
            state.value = state.value.copy(sessions = listOf(session.copy(messages = listOf(StudioMessage("assistant", "À toi de répondre.", quiz = quiz)))))
        }
        fun installProblem() {
            val session = state.value.sessions.single()
            val problem = StudioProblem("Combien font 2 + 2 ?", "recall", "On réunit deux paires : 4.", listOf("Pense aux paires."))
            state.value = state.value.copy(sessions = listOf(session.copy(messages = listOf(StudioMessage("assistant", "", problem = problem)))))
        }
        fun installAssessment() {
            val session = state.value.sessions.single()
            val assessment = StudioDiagnosticAssessment(1, listOf(StudioDiagnosticStrand("Fractions", "Une moitié", "Les tiers", sources = listOf(0))), "Les moitiés sont comprises.")
            state.value = state.value.copy(sessions = listOf(session.copy(messages = listOf(StudioMessage("user", "Une moitié")), assessment = assessment)))
        }
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
        override fun requestQuiz(): Boolean { quizRequests++; return true }
        override fun revealPracticeHint(messageIndex: Int): Boolean {
            val session = state.value.sessions.single()
            val updated = session.copy(messages = session.messages.mapIndexed { index, message ->
                if (index == messageIndex) message.copy(problem = message.problem!!.copy(shownHints = 1)) else message
            })
            state.value = state.value.copy(sessions = listOf(updated))
            return true
        }
        override fun submitPracticeAttempt(messageIndex: Int, answer: String, thinking: String): Boolean {
            problemThinking = thinking
            val session = state.value.sessions.single()
            val updated = session.copy(messages = session.messages.mapIndexed { index, message ->
                if (index == messageIndex) message.copy(problem = message.problem!!.copy(
                    attempt = StudioProblemAttempt(answer, thinking, 1L),
                    result = StudioProblemResult("solvedWithHints", "Tu as trouvé la bonne réponse."),
                )) else message
            })
            state.value = state.value.copy(sessions = listOf(updated))
            return true
        }
        override fun submitQuiz(messageIndex: Int, selectedIds: List<String>, dontKnow: Boolean, thinking: String, confidence: Int?): Boolean {
            quizSelection = selectedIds
            quizDontKnow = dontKnow
            quizThinking = thinking
            quizConfidence = confidence
            if (!acceptQuiz) {
                state.value = state.value.copy(error = "La réponse n’a pas pu être enregistrée.")
                return false
            }
            val session = state.value.sessions.firstOrNull { it.id == state.value.selectedId } ?: return false
            val message = session.messages[messageIndex]
            val answer = StudioQuizAnswer(selectedIds, dontKnow, thinking, confidence, !dontKnow && selectedIds == message.quiz?.correctIds, 1L)
            val updated = session.copy(messages = session.messages.mapIndexed { index, item -> if (index == messageIndex) item.copy(quizAnswer = answer) else item })
            state.value = state.value.copy(sessions = state.value.sessions.map { if (it.id == updated.id) updated else it }, error = null)
            return true
        }
        override fun retry() = Unit
        override fun cancel() { cancelCount++; state.value = state.value.copy(busy = false) }
    }
}
