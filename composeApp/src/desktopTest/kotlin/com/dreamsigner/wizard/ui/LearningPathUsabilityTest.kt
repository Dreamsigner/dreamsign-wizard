@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
package com.dreamsigner.wizard.ui

import androidx.compose.material.Surface
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import com.dreamsigner.wizard.LearningAppTheme
import com.dreamsigner.wizard.model.*
import com.dreamsigner.wizard.storage.ProgressStore
import com.dreamsigner.wizard.viewmodel.AppScreen
import com.dreamsigner.wizard.viewmodel.AppViewModel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.skia.Image
import java.io.File
import kotlin.test.*

class LearningPathUsabilityTest {
    @Test
    fun productionCatalogueRendersAtCompactMediumAndWideWidthsWithWorkingNavigation() {
        listOf(360, 400, 700, 900, 1400).forEach { width ->
            val model = AppViewModel(MemoryStore(UserStats(xp = 1_234_567, streakDays = 128, hearts = 4, maxHearts = 5, lastStudyDate = "2026-10-20")), today = { "2026-10-20" })
            try {
                runDesktopComposeUiTest(width = width, height = 900, testTimeout = kotlin.time.Duration.parse("60s")) {
                    setContent { LearningAppTheme { DreamsignWizardApp(model, settingsActionCount = 2) } }
                    onNodeWithText("Mes cours").assertIsDisplayed()
                    onNodeWithText("Les leçons").assertExists()
                    onNodeWithContentDescription("Choisir une matière").assertExists()
                    onNodeWithText("1234567 XP").assertIsDisplayed()
                    onNodeWithText("128 j").assertIsDisplayed()
                    onNodeWithText("4/5").assertIsDisplayed()
                    val selectorBounds = onNodeWithContentDescription("Choisir une matière").fetchSemanticsNode().boundsInRoot
                    listOf("1234567 XP", "128 j", "4/5").forEach { label ->
                        val statBounds = onNodeWithText(label).fetchSemanticsNode().boundsInRoot
                        assertFalse(selectorBounds.overlaps(statBounds), "$label overlaps the subject selector at $width dp")
                        val windowBounds = onRoot().fetchSemanticsNode().boundsInRoot
                        assertTrue(statBounds.left >= windowBounds.left && statBounds.right <= windowBounds.right && statBounds.top >= windowBounds.top && statBounds.bottom <= windowBounds.bottom, "$label extends beyond the window at $width dp")
                    }
                    saveSnapshot("learning-path-$width")
                    onNode(hasText("Profil") and hasClickAction()).performClick()
                    onNodeWithText("Mon parcours").assertIsDisplayed()
                    onNode(hasText("Profil") and hasClickAction()).performClick()
                    onNodeWithText("Mon parcours").assertIsDisplayed()
                    onNode(hasText("Paramètres") and hasClickAction()).assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "2 actions requises"))
                    onNode(hasText("Paramètres") and hasClickAction()).performClick()
                    onNodeWithText("Données et progression").assertIsDisplayed()
                    onNode(hasText("Cours") and hasClickAction()).performClick()
                    onNodeWithText("Les leçons").assertExists()
                    onNode(hasText("Cours") and hasClickAction()).performClick()
                    onNodeWithText("Les leçons").assertExists()
                }
            } finally { model.close() }
        }
    }

    @Test
    fun courseCardsKeepPointerFeedbackQuietAndKeyboardFocusCapturable() = runDesktopComposeUiTest(width = 1400, height = 900) {
        val fixture = Fixture()
        lateinit var inputModes: InputModeManager
        try {
            setContent { inputModes = LocalInputModeManager.current; LearningAppTheme { DreamsignWizardApp(fixture.model) } }
            val courseChoice = onAllNodes(hasText("Cours suivant") and hasClickAction()).onFirst()
            courseChoice.performMouseInput { moveTo(center); click() }
            courseChoice.assertIsSelected()
            saveSnapshot("learning-course-choice-pointer")

            runOnIdle { inputModes.requestInputMode(InputMode.Keyboard) }
            courseChoice.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
                .performKeyInput { pressKey(Key.Enter) }
            courseChoice.assertIsSelected()
            saveSnapshot("learning-course-choice-keyboard")
        } finally { fixture.model.close() }
    }

    @Test
    fun courseReaderUsesPermanentDesktopContentsAndCompactDialogAtResponsiveWidths() {
        val source = com.dreamsigner.wizard.data.SampleData.sampleSubjects.flatMap { it.units }.first { it.id == "mit_18_01sc_2010" }
        val course = source.copy(sourceUrl = "", lessons = source.lessons.take(3).mapIndexed { index, lesson ->
            lesson.copy(id = "reader-$index", title = listOf("Limites", "Dérivées", "Intégrales")[index],
                objective = listOf("Approcher une valeur", "Étudier le sinus", "Calculer une aire")[index],
                learningNotes = "Notes de la section $index", sourceUrls = emptyList())
        })
        listOf(900, 1400).forEach { width -> runDesktopComposeUiTest(width = width, height = 900) {
            setContent { LearningAppTheme { Surface {
                CourseReaderScreen(course, { false }, {}, {}, searchableContents = true,
                    favoriteLessonIds = setOf("reader-0", "reader-2"), onToggleFavorite = {})
            } } }
            if (width < 1000) {
                onNodeWithText("Sommaire").performClick()
                onNodeWithText("Sommaire du cours").assertIsDisplayed()
            } else {
                onNodeWithText("Sommaire du cours").assertIsDisplayed()
                onNodeWithText("Sommaire").assertDoesNotExist()
            }
            onNodeWithText("Favoris uniquement").performClick().assertIsOn()
            onNodeWithText("Rechercher une leçon").performTextInput("aire")
            onNodeWithText("1 leçon").assertIsDisplayed()
            saveSnapshot("course-reader-layout-$width")
            onNodeWithText("3. Intégrales").performClick()
            onNodeWithText("Notes de la section 2").assertIsDisplayed()
            if (width >= 1000) onNodeWithText("Sommaire du cours").assertIsDisplayed()
        } }
    }

    @Test
    fun lessonSearchFiltersPreserveOrderAndLocksAndResetWhenCourseChanges() = runDesktopComposeUiTest(width = 1400, height = 1000, testTimeout = kotlin.time.Duration.parse("60s")) {
        val fixture = Fixture()
        try {
            setContent { LearningAppTheme { DreamsignWizardApp(fixture.model) } }
            onNodeWithText("Disponibles").performClick()
            onNodeWithText("Une étape ouverte").assertExists()
            onNodeWithText("Une étape terminée").assertDoesNotExist()
            onNodeWithText("Une étape verrouillée").assertDoesNotExist()

            onNodeWithText("Terminées").performClick()
            onNodeWithText("Une étape terminée").assertExists()
            onNodeWithText("Une étape ouverte").assertDoesNotExist()
            onNodeWithText("Toutes").performClick()

            onNodeWithContentDescription("Rechercher une leçon").performClick()
            onNodeWithText("Rechercher dans ce cours").performClick().performTextInput("réservoir")
            onNodeWithText("Une étape verrouillée").assertIsDisplayed().assertIsNotEnabled()
            onNodeWithText("Verrouillée · Terminer les prérequis").assertExists()

            onNodeWithText("Disponibles").performClick()
            onNodeWithText("Aucune leçon ne correspond").assertIsDisplayed()
            onNodeWithText("Réinitialiser").performClick()
            onNodeWithText("Rechercher dans ce cours").assertExists()
            onNodeWithText("Une étape ouverte").assertExists()
            onNodeWithText("Une étape verrouillée").assertExists()

            onNodeWithText("Toutes").performClick()
            onNodeWithText("Rechercher dans ce cours").performClick().performTextInput("réservoir")
            onNode(hasText("Cours suivant") and hasClickAction()).performClick().performClick()
            onNodeWithText("1 sur 1 leçons").assertExists()
            onNodeWithText("Rechercher dans ce cours").assertDoesNotExist()
            onNodeWithText("Une leçon suivante").assertExists()
        } finally { fixture.model.close() }
    }

    @Test
    fun navigationFadeThroughCanBeCapturedAtAControlledIntermediateFrame() = runDesktopComposeUiTest(width = 1400, height = 900) {
        val model = AppViewModel(MemoryStore(), today = { "2026-10-20" })
        try {
            mainClock.autoAdvance = false
            setContent { LearningAppTheme { DreamsignWizardApp(model) } }
            mainClock.advanceTimeByFrame()
            onNode(hasText("Profil") and hasClickAction()).performClick()
            mainClock.advanceTimeBy(100)
            saveSnapshot("learning-profile-transition-midpoint")
            mainClock.advanceTimeBy(150)
            onNodeWithText("Mon parcours").assertExists()
            mainClock.autoAdvance = true
        } finally { model.close() }
    }

    @Test
    fun filterCanBeActivatedFromKeyboardAndEmptyCatalogueHasReadableResetState() = runDesktopComposeUiTest(width = 900, height = 760) {
        val fixture = Fixture(emptyList())
        try {
            fixture.model.selectCourse("ui_path", "ui_course_a")
            setContent { LearningAppTheme { DreamsignWizardApp(fixture.model) } }
            onNodeWithText("Aucune leçon dans ce cours").assertIsDisplayed()
            onNodeWithText("Toutes").performSemanticsAction(SemanticsActions.RequestFocus) { it() }.performKeyInput { pressKey(Key.Enter) }
            onNodeWithText("Aucune leçon dans ce cours").assertIsDisplayed()
        } finally { fixture.model.close() }
    }

    private fun ComposeUiTest.saveSnapshot(name: String) {
        val output = File("build/reports/ui-snapshots/$name.png")
        check(output.parentFile.isDirectory || output.parentFile.mkdirs())
        Image.makeFromBitmap(onAllNodes(isRoot()).onLast().captureToImage().asSkiaBitmap()).use { image ->
            image.encodeToData()!!.use { output.writeBytes(it.bytes) }
        }
    }

    private class MemoryStore(initial: UserStats? = null) : ProgressStore {
        @Volatile private var value: String? = initial?.let { Json.encodeToString(it) }
        override fun read(): String? = value
        override fun write(value: String) { this.value = value }
    }

    private class Fixture(firstCourseLessons: List<Lesson> = listOf(completed, available, locked)) {
        private val catalogue = listOf(
            Subject("ui_path", "Parcours de test", "", "Des cours pour apprendre par étapes.", SubjectCategory.MATHEMATICS, listOf(
                LearningUnit("ui_course_a", 1, "Cours de départ", "", firstCourseLessons),
                LearningUnit("ui_course_b", 2, "Cours suivant", "", listOf(nextCourseLesson))
            )),
            Subject("ui_path_second", "Autre domaine", "", "Un autre domaine d’apprentissage.", SubjectCategory.COMPUTER_SCIENCE,
                listOf(LearningUnit("ui_course_second", 1, "Cours alternatif", "", listOf(nextCourseLesson.copy(id = "lesson_alt")))))
        )
        val model = AppViewModel(MemoryStore(UserStats(activeSubjectId = "ui_path", completedLessonIds = setOf(completed.id))),
            today = { "2026-10-20" }, catalogue = catalogue)
    }

    companion object {
        private val completed = lesson("lesson_done", "Une étape terminée", "", isCompleted = true, requiresPreviousLesson = false)
        private val available = lesson("lesson_open", "Une étape ouverte", "Un repère autour du bassin versant.", requiresPreviousLesson = false)
        private val locked = lesson("lesson_locked", "Une étape verrouillée", "Étudier le réservoir et son équilibre.")
        private val nextCourseLesson = lesson("lesson_next", "Une leçon suivante", "", requiresPreviousLesson = false)

        private fun lesson(id: String, title: String, objective: String, isCompleted: Boolean = false, requiresPreviousLesson: Boolean = true) = Lesson(
            id = id, title = title, xpReward = 10, isCompleted = isCompleted, isLocked = false,
            exercises = listOf(Exercise.MultipleChoice(id = "${id}_exercise", prompt = "Quel choix ?", options = listOf("A", "B"), correctOptionIndex = 0,
                explanation = "Le premier choix est correct.", hint = "Lis les choix.")), objective = objective,
            requiresPreviousLesson = requiresPreviousLesson
        )
    }
}
