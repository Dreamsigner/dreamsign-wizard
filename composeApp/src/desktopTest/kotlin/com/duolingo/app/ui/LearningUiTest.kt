@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package com.duolingo.app.ui

import androidx.compose.material.MaterialTheme
import com.duolingo.app.LearningAppTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.*
import com.duolingo.app.model.Exercise
import com.duolingo.app.model.LearningUnit
import com.duolingo.app.model.Lesson
import com.duolingo.app.model.Subject
import com.duolingo.app.model.SubjectCategory
import com.duolingo.app.model.UserStats
import com.duolingo.app.storage.ProgressStore
import com.duolingo.app.viewmodel.AppViewModel
import com.duolingo.app.ui.math.MathRenderingProvider
import com.duolingo.app.ui.math.DesktopMathRenderer
import kotlinx.serialization.json.Json
import org.jetbrains.skia.Image
import java.io.File
import kotlin.time.Duration.Companion.seconds
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Real Compose screens, original fixtures, no production catalogue, files, audio or microphone.
 * Versioned 1.9.3 harness (not the v2 testing API introduced in Compose 1.11):
 * https://github.com/JetBrains/compose-multiplatform-core/blob/v1.9.3/compose/ui/ui-test/src/skikoMain/kotlin/androidx/compose/ui/test/ComposeUiTest.skiko.kt
 * https://github.com/JetBrains/compose-multiplatform-core/blob/v1.9.3/compose/ui/ui-test/src/desktopMain/kotlin/androidx/compose/ui/test/ComposeUiTest.desktop.kt
 * Requires ui-test:1.9.3 and the project's existing kotlin-test-junit dependency.
 */
class LearningUiTest {
    @Test
    fun publishedWindowsCatalogueAndMatrixExerciseRenderWithTheirActualContent() = runDesktopComposeUiTest(width = 1400, height = 900, testTimeout = 60.seconds) {
        val model = AppViewModel(MemoryProgressStore(), today = { "2026-10-20" })
        try {
            setContent { FixtureTheme { DuolingoApp(model) } }
            onNodeWithText("Mes cours").assertIsDisplayed()
            onNodeWithContentDescription("Série de jours").assertExists()
            onNodeWithContentDescription("Vies restantes").assertExists()
            saveSnapshot("published-catalogue-wide")
            val linear = model.subjects.value.flatMap { it.units }.first { it.id == "mit_18_06sc_2011" }
            runOnIdle { model.startLesson(linear.lessons.first()) }
            onNodeWithText("Passer aux exercices").performClick()
            val first = linear.lessons.first().exercises.first() as Exercise.MathChallenge
            onNodeWithText(first.options[first.correctOptionIndex]).performScrollTo().performClick()
            onNodeWithText("Vérifier ma réponse").performClick()
            onNodeWithText("Continuer").performClick()
            onNodeWithText(linear.lessons.first().exercises[1].prompt).assertExists()
            saveSnapshot("published-matrix-exercise-wide")
        } finally { model.close() }
    }

    @Test
    fun theCourseReaderShowsLockedSectionsWithoutAwardingProgress() = runDesktopComposeUiTest(width = 1400, height = 900, testTimeout = 60.seconds) {
        val first = quizLesson()
        val locked = first.copy(id = "locked", title = "Section encore verrouillée", requiresPreviousLesson = true,
            learningNotes = "Le texte de cette seconde section reste lisible avant de réussir la première.")
        val fixture = Fixture(listOf(first, locked))
        try {
            setContent { FixtureTheme { DuolingoApp(fixture.model) } }
            onNodeWithText("Lire le cours et les supports").performScrollTo().performClick()
            onNodeWithText("Cours et supports").assertIsDisplayed()
            onNodeWithText("Sommaire").performClick()
            onNodeWithText("2. ${locked.title}").performClick()
            onNodeWithText(locked.learningNotes).assertIsDisplayed()
            onNodeWithText("Pratiquer · ${locked.title}").assertIsNotEnabled()
            saveSnapshot("course-reader-wide")
            onNodeWithContentDescription("Fermer le cours").performClick()
            onNodeWithText("Mes cours").assertIsDisplayed()
            runOnIdle {
                assertEquals(0, fixture.model.userStats.value.xp)
                assertTrue(fixture.model.userStats.value.completedLessonIds.isEmpty())
            }
        } finally { fixture.model.close() }
    }

    @Test
    fun objectivePrecedesQuestionAndExplanationHasItsOwnButton() = runDesktopComposeUiTest(width = 1400, height = 900, testTimeout = 60.seconds) {
        val lesson = quizLesson()
        val fixture = Fixture(listOf(lesson))
        try {
            fixture.model.startLesson(lesson)
            setContent { FixtureTheme { LessonQuizScreen(fixture.model) } }
            onNodeWithText("Passer aux exercices").performClick()
            val objective = onNodeWithText("Objectif : ${lesson.objective}").fetchSemanticsNode().boundsInRoot
            val question = onNodeWithText(PROMPT).fetchSemanticsNode().boundsInRoot
            val option = onNodeWithText("Trois").fetchSemanticsNode().boundsInRoot
            assertTrue(objective.bottom <= question.top)
            assertTrue(question.bottom <= option.top)
            onNodeWithText("Quatre").performClick()
            onNodeWithText("Vérifier ma réponse").performClick()
            onNodeWithText(lesson.exercises.first().explanation).assertDoesNotExist()
            onNodeWithText("Pourquoi cette réponse ?").performScrollTo().performClick()
            onNodeWithText(lesson.exercises.first().explanation).assertExists()
            saveSnapshot("question-feedback-wide")
        } finally { fixture.model.close() }
    }

    @Test
    fun theChosenPublishedCourseSurvivesModelRecreation() = runDesktopComposeUiTest(width = 1400, height = 900, testTimeout = 60.seconds) {
        val store = MemoryProgressStore()
        var model by mutableStateOf(AppViewModel(store, today = { "2026-10-20" }))
        try {
            setContent { FixtureTheme { key(model) { DuolingoApp(model) } } }
            val subject = model.subjects.value.first { it.category == SubjectCategory.MATHEMATICS }
            val chosen = subject.units.last()
            onNode(hasText(chosen.title) and hasClickAction()).performScrollTo().performClick()
            val continueLabel = "Continuer · ${chosen.lessons.first().title}"
            onNodeWithText(continueLabel).performScrollTo().assertIsDisplayed()
            runOnIdle {
                model.close()
                model = AppViewModel(store, today = { "2026-10-20" })
            }
            onNodeWithText(continueLabel).assertExists()
            runOnIdle {
                assertEquals(0, model.userStats.value.xp)
                assertTrue(model.userStats.value.completedLessonIds.isEmpty())
            }
        } finally {
            model.close()
        }
    }

    @Test
    fun notesIdentifyTheSourceNumberAndTheAdaptedExam() = runComposeUiTest(testTimeout = 60.seconds) {
        val exam = quizLesson().copy(sourceSessionNumber = 22, sessionKind = "exam", sourceTitle = "Exam 1")
        val fixture = Fixture(listOf(exam))
        setContent { FixtureTheme { DuolingoApp(fixture.model) } }
        onNodeWithText("Entraînement à l’examen · 22").assertIsDisplayed()
        onNodeWithText("Continuer · ${exam.title}").performScrollTo().performClick()
        onNodeWithText("Entraînement à l’examen · 22").assertIsDisplayed()
        onNodeWithText("Source : Exam 1").assertExists()
        onNodeWithText("Passer aux exercices").assertIsEnabled()
    }

    @OptIn(androidx.compose.ui.InternalComposeUiApi::class)
    @Test
    fun keyboardEnterActivatesTheFocusedNavigationControl() = runDesktopComposeUiTest(width = 1400, height = 900, testTimeout = 60.seconds) {
        val fixture = Fixture(listOf(quizLesson()))
        setContent { FixtureTheme { DuolingoApp(fixture.model) } }
        val profile = onNodeWithText("Profil")
        profile.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        profile.assertIsFocused()
        // The event is delivered to the Compose scene, never the operating system.
        // Constructor and helper are pinned to the official Compose 1.9.3 sources:
        // https://github.com/JetBrains/compose-multiplatform-core/blob/v1.9.3/compose/ui/ui/src/skikoMain/kotlin/androidx/compose/ui/input/key/KeyEvent.skiko.kt
        profile.performKeyPress(KeyEvent(Key.Enter, KeyEventType.KeyDown))
        profile.performKeyPress(KeyEvent(Key.Enter, KeyEventType.KeyUp))
        onNodeWithText("Mon parcours").assertIsDisplayed()
        onNodeWithText("Profil").assertIsSelected()
    }

    @Test
    fun everyPublishedCourseOpensItsActualFirstLessonIndependently() = runDesktopComposeUiTest(width = 1400, height = 900, testTimeout = 120.seconds) {
        val store = MemoryProgressStore()
        // Default catalogue is the generated, published content; only persistence is replaced.
        val model = AppViewModel(store, today = { "2026-10-20" })
        try {
            setContent { FixtureTheme { DuolingoApp(model) } }
            val subjects = model.subjects.value
            assertEquals(3, subjects.size)
            subjects.forEach { subject ->
                runOnIdle { model.selectSubject(subject.id) }
                subject.units.forEachIndexed { index, course ->
                    // Lazy rows outside the viewport have no node yet. Scroll the leading
                    // course list to compose the target instead of looking for it directly.
                    onAllNodes(hasScrollToIndexAction()).onFirst()
                        .performScrollToNode(hasText(course.title) and hasClickAction())
                    onNode(hasText(course.title) and hasClickAction()).performScrollTo().performClick()
                    val first = course.lessons.first()
                    onNodeWithText("Continuer · ${first.title}").performScrollTo().performClick()
                    onNodeWithText("Comprendre avant de pratiquer").assertIsDisplayed()
                    onNodeWithText(first.learningNotes).assertExists()
                    onNodeWithText("Passer aux exercices").assertIsEnabled()
                    if (index == 0) saveSnapshot("published-notes-${subject.category.name.lowercase()}")
                    onNodeWithContentDescription("Quitter la leçon").performClick()
                    runOnIdle {
                        assertEquals(0, model.userStats.value.xp)
                        assertTrue(model.userStats.value.completedLessonIds.isEmpty())
                    }
                }
            }
        } finally {
            model.close()
        }
    }

    @Test
    fun profileAndCoursesNavigationUsesActualScreens() = runComposeUiTest(testTimeout = 60.seconds) {
        val fixture = Fixture(listOf(quizLesson()))
        setContent { FixtureTheme { DuolingoApp(fixture.model) } }

        onNodeWithText("Mes cours").assertIsDisplayed()
        onNodeWithText("Profil").performClick()
        onNodeWithText("Mon parcours").assertIsDisplayed()
        onNodeWithText("XP accumulés").assertExists()
        onNodeWithText("Profil").assertIsSelected()
        onNodeWithText("Cours").performClick()
        onNodeWithText("Mes cours").assertIsDisplayed()
        onNodeWithText("Cours").assertIsSelected()
    }

    @Test
    fun notesWrongRetryCorrectAndRestartAwardXpOnlyOnce() = runComposeUiTest(testTimeout = 60.seconds) {
        val lesson = quizLesson()
        val fixture = Fixture(listOf(lesson))
        var model by mutableStateOf(fixture.model)
        setContent { FixtureTheme { key(model) { DuolingoApp(model) } } }

        onNodeWithText("Continuer · ${lesson.title}").performScrollTo().performClick()
        onNodeWithText("Comprendre avant de pratiquer").assertIsDisplayed()
        onNodeWithText(NOTES).assertExists()
        onNodeWithText(PROMPT).assertDoesNotExist()
        onNodeWithText("Passer aux exercices").performClick()
        onNodeWithText(PROMPT).assertIsDisplayed()
        onNodeWithText("Vérifier ma réponse").assertIsNotEnabled()

        onNodeWithText("Trois").performScrollTo().performClick()
        onNodeWithText("Vérifier ma réponse").performClick()
        onNodeWithText("Cette question reviendra en fin de séance").assertIsDisplayed()
        onNodeWithText("Continuer").assertIsEnabled()
        onNodeWithText("Quatre").assertIsNotEnabled()
        runOnIdle {
            assertEquals(0, model.userStats.value.xp)
            assertEquals(4, model.userStats.value.hearts)
            assertTrue(model.userStats.value.completedLessonIds.isEmpty())
        }

        onNodeWithText("Continuer").performClick()
        onNodeWithText("Reprise des erreurs · 1 à corriger").assertIsDisplayed()
        onNodeWithText("Vérifier ma réponse").assertIsNotEnabled()
        onNodeWithText("Quatre").performScrollTo().performClick()
        onNodeWithText("Vérifier ma réponse").performClick()
        onNodeWithText("Bonne réponse").assertIsDisplayed()
        // XP belong to completed lessons, not to a correct answer awaiting Continue.
        runOnIdle { assertEquals(0, model.userStats.value.xp) }
        onNodeWithText("Continuer").performClick()
        onNodeWithText("Une leçon de plus !").assertIsDisplayed()
        onNodeWithText("+20 XP. Ta progression avance.").assertExists()
        runOnIdle {
            assertEquals(20, model.userStats.value.xp)
            assertEquals(setOf(lesson.id), model.userStats.value.completedLessonIds)
            assertEquals(20, fixture.store.savedStats().xp)
        }

        // Dispose the actual app, then create a new VM from the same in-memory JSON store.
        runOnIdle {
            model.close()
            model = fixture.newModel()
        }
        onNodeWithText("Mes cours").assertIsDisplayed()
        onNodeWithText("Profil").performClick()
        onNodeWithText("Mon parcours").assertIsDisplayed()
        runOnIdle {
            assertEquals(20, model.userStats.value.xp)
            assertEquals(4, model.userStats.value.hearts)
            assertEquals(setOf(lesson.id), model.userStats.value.completedLessonIds)
        }
        onNodeWithText("Cours").performClick()
        onNode(hasText(lesson.title) and hasClickAction()).performScrollTo().performClick()
        onNodeWithText("Passer aux exercices").performClick()
        onNodeWithText("Quatre").performScrollTo().performClick()
        onNodeWithText("Vérifier ma réponse").performClick()
        onNodeWithText("Continuer").performClick()
        onNodeWithText("Révision terminée. Tu consolides tes acquis sans ajouter de XP.").assertExists()
        runOnIdle {
            assertEquals(20, model.userStats.value.xp)
            assertEquals(20, fixture.store.savedStats().xp)
        }
    }

    @Test
    fun helpShowsLessonAndExerciseHintWithoutChangingTheAttempt() {
        withLesson(quizLesson()) { model ->
            onNodeWithText("Passer aux exercices").performClick()
            onNodeWithText("Quatre").performScrollTo().performClick()
            onNodeWithContentDescription("Ouvrir le cours et l’indice").performClick()
            onNodeWithText("Le cours et l’indice").assertIsDisplayed()
            onNodeWithText(NOTES).assertExists()
            onNodeWithText("Indice pour cet exercice").performScrollTo().assertIsDisplayed()
            onNodeWithText(HINT).performScrollTo().assertIsDisplayed()
            onNodeWithText("Revenir à la leçon").performClick()
            onNodeWithText(PROMPT).assertIsDisplayed()
            onNodeWithText("Quatre").assertIsSelected()
            onNodeWithText("Vérifier ma réponse").assertIsEnabled()
            runOnIdle {
                assertEquals(0, model.currentExerciseIndex.value)
                assertEquals(5, model.userStats.value.hearts)
                assertEquals(false, model.isAnswerChecked.value)
            }
        }
    }

    @Test
    fun readingPassageIsVisibleAndTheAnswerUsesItsMeaning() {
        val reading = Exercise.ReadingChoice(
            id = "ui_reading_question", prompt = "À quelle heure ouvre l’atelier ?",
            passage = "L’atelier ouvre à neuf heures. La bibliothèque ouvre à dix heures.",
            options = listOf("Neuf heures", "Dix heures"), correctOptionIndex = 0,
            explanation = "La première phrase donne neuf heures pour l’atelier.",
            hint = "Repère le sujet de chaque phrase."
        )
        withLesson(lesson("ui_reading", "Lire un horaire", reading)) { model ->
            onNodeWithText("Passer aux exercices").performClick()
            onNodeWithText(reading.passage).assertIsDisplayed()
            onNodeWithText("Vérifier ma réponse").assertIsNotEnabled()
            onNodeWithText("Neuf heures").performScrollTo().performClick()
            onNodeWithText("Vérifier ma réponse").performClick()
            onNodeWithText("Bonne réponse").assertIsDisplayed()
            onNodeWithText("Pourquoi cette réponse ?").performScrollTo().performClick()
            onNodeWithText(reading.explanation).assertExists()
            onNodeWithText("Continuer").performClick()
            onNodeWithText("Une leçon de plus !").assertIsDisplayed()
            runOnIdle { assertEquals(20, model.userStats.value.xp) }
        }
    }

    @Test
    fun writingRequiresWordBoundsAndEveryRubricItemAndEditingResetsReview() {
        val written = Exercise.WrittenProduction(
            id = "ui_written_question", prompt = "Écris une courte invitation originale.",
            minWords = 3, maxWords = 5,
            reviewCriteria = listOf("Sujet traité", "Phrase relue"),
            exampleResponse = "Viens demain à midi.",
            explanation = "La pratique est terminée après ta propre relecture ; aucun score officiel.",
            hint = "Compte les mots et relis la consigne."
        )
        withLesson(lesson("ui_written", "Écrire une invitation", written)) { model ->
            onNodeWithText("Passer aux exercices").performClick()
            onNodeWithText("Terminer ma relecture").assertIsNotEnabled()
            replaceDraft("un deux")
            reviewBoth()
            onNodeWithText("Terminer ma relecture").assertIsNotEnabled()

            replaceDraft("un deux trois") // Exactly the lower bound.
            onNodeWithText("Terminer ma relecture").assertIsNotEnabled()
            reviewBoth()
            onNodeWithText("Terminer ma relecture").assertIsEnabled()

            replaceDraft("un deux trois quatre cinq six") // Above the upper bound, even reviewed.
            reviewBoth()
            onNodeWithText("Terminer ma relecture").assertIsNotEnabled()
            replaceDraft("un deux trois quatre cinq") // Exactly the upper bound.
            reviewBoth()
            onNodeWithText("Terminer ma relecture").assertIsEnabled()

            replaceDraft("un deux trois quatre")
            onNodeWithContentDescription("Sujet traité").assertIsOff()
            onNodeWithContentDescription("Phrase relue").assertIsOff()
            onNodeWithText("Terminer ma relecture").assertIsNotEnabled()
            reviewBoth()
            onNodeWithText("Comparer avec un exemple rédigé").performScrollTo().performClick()
            onNodeWithText(written.exampleResponse).performScrollTo().assertIsDisplayed()
            onNodeWithText("Terminer ma relecture").performClick()
            onNodeWithText("Entraînement auto-évalué terminé").assertIsDisplayed()
            onNodeWithText("Continuer").performClick()
            onNodeWithText("Une leçon de plus !").assertIsDisplayed()
            runOnIdle {
                assertEquals("un deux trois quatre", model.userStats.value.writtenDrafts[written.id])
                assertEquals(20, model.userStats.value.xp)
            }
        }
    }

    @Test
    fun compactLayoutKeepsNavigationBelowContent() = runDesktopComposeUiTest(width = 400, height = 900, testTimeout = 60.seconds) {
        val fixture = Fixture(listOf(quizLesson()))
        setContent { FixtureTheme { DuolingoApp(fixture.model) } }
        val courses = onNodeWithText("Cours").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val profile = onNodeWithText("Profil").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val picker = onNode(hasText(COURSE) and hasClickAction()).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val next = onNodeWithText("Continuer · ${quizLesson().title}").performScrollTo().fetchSemanticsNode().boundsInRoot
        assertTrue(abs(courses.center.y - profile.center.y) < 2f, "Compact navigation should be horizontal")
        assertTrue(profile.center.x > courses.center.x)
        assertTrue(courses.top >= next.bottom, "Bottom navigation should follow content")
        assertTrue(picker.left < 40f && picker.right <= 400f, "Course picker should fit the compact content column")
        saveSnapshot("learning-compact")
        onNodeWithText("Profil").performClick()
        onNodeWithText("Mon parcours").assertIsDisplayed()
    }

    @Test
    fun wideLayoutPlacesNavigationAndCoursesInSeparateColumns() = runDesktopComposeUiTest(width = 1400, height = 900, testTimeout = 60.seconds) {
        val fixture = Fixture(listOf(quizLesson()))
        setContent { FixtureTheme { DuolingoApp(fixture.model) } }
        val courses = onNodeWithText("Cours").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val profile = onNodeWithText("Profil").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val course = onNode(hasText(COURSE) and hasClickAction()).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val next = onNodeWithText("Continuer · ${quizLesson().title}").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertTrue(profile.top >= courses.bottom, "Wide navigation should be vertical")
        assertTrue(abs(courses.center.x - profile.center.x) < 2f)
        assertTrue(course.left > courses.right, "Course column should follow navigation")
        assertTrue(next.left > course.right, "Lesson workspace should follow the course column")
        saveSnapshot("learning-wide")
        onNodeWithText("Profil").performClick()
        onNodeWithText("Mon parcours").assertIsDisplayed()
    }

    private fun withLesson(lesson: Lesson, block: suspend ComposeUiTest.(AppViewModel) -> Unit) {
        val fixture = Fixture(listOf(lesson))
        try {
            runComposeUiTest(testTimeout = 60.seconds) {
                fixture.model.startLesson(lesson)
                setContent { FixtureTheme { LessonQuizScreen(fixture.model) } }
                block(fixture.model)
            }
        } finally {
            fixture.model.close()
        }
    }

    private fun ComposeUiTest.saveSnapshot(name: String) {
        val output = File("build/reports/ui-snapshots/$name.png")
        check(output.parentFile.isDirectory || output.parentFile.mkdirs())
        val image = Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap())
        try {
            val encoded = checkNotNull(image.encodeToData()) { "PNG encoding failed" }
            try { output.writeBytes(encoded.bytes) } finally { encoded.close() }
        } finally { image.close() }
    }

    private fun ComposeUiTest.replaceDraft(text: String) {
        onNode(hasSetTextAction()).performScrollTo().performTextReplacement(text)
    }

    private fun ComposeUiTest.reviewBoth() {
        onNodeWithContentDescription("Sujet traité").performScrollTo().performClick()
        onNodeWithContentDescription("Phrase relue").performScrollTo().performClick()
    }

    private class MemoryProgressStore : ProgressStore {
        @Volatile private var value: String? = null
        override fun read(): String? = value
        override fun write(value: String) { this.value = value }
        fun savedStats(): UserStats = Json.decodeFromString(assertNotNull(read()))
    }

    private class Fixture(lessons: List<Lesson>) {
        val store = MemoryProgressStore()
        private val catalogue = listOf(
            Subject("ui_subject", "Matière miniature", "", "Catalogue original de test.", SubjectCategory.MATHEMATICS,
                listOf(LearningUnit("ui_unit", 1, COURSE, "Un seul cours pour les tests.", lessons)))
        )
        val model = newModel()
        fun newModel() = AppViewModel(store, today = { "2026-10-20" }, catalogue = catalogue)
    }

    companion object {
        private const val COURSE = "Cours miniature"
        private const val NOTES = "Deux groupes de deux objets donnent quatre objets. Relis la question avant de choisir ta réponse."
        private const val PROMPT = "Combien font deux plus deux ?"
        private const val HINT = "Compte deux objets, puis ajoute encore deux objets."

        private fun lesson(id: String, title: String, exercise: Exercise) = Lesson(
            id = id, title = title, xpReward = 20, isLocked = false,
            exercises = listOf(exercise), objective = "Comprendre puis pratiquer un petit exemple.",
            learningNotes = NOTES, requiresPreviousLesson = false
        )

        private fun quizLesson() = lesson("ui_quiz", "Addition miniature", Exercise.MultipleChoice(
            id = "ui_addition", prompt = PROMPT, options = listOf("Trois", "Quatre"),
            correctOptionIndex = 1, explanation = "Deux plus deux donnent quatre.", hint = HINT
        ))
    }
}

@Composable
private fun FixtureTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalUriHandler provides TestUriHandler) {
        LearningAppTheme { MathRenderingProvider(DesktopMathRenderer) { content() } }
    }
}

private object TestUriHandler : UriHandler {
    override fun openUri(uri: String) = error("UI fixtures must not open external resources: $uri")
}
