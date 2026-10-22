@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
package com.duolingo.app.ui

import androidx.compose.material.Surface
import androidx.compose.ui.test.*
import androidx.compose.ui.graphics.asSkiaBitmap
import com.duolingo.app.LearningAppTheme
import com.duolingo.app.data.SampleData
import org.jetbrains.skia.Image
import java.io.File
import kotlin.test.*

class CourseContentsSearchTest {
    private val source = SampleData.sampleSubjects.flatMap { it.units }.first { it.id == "mit_18_01sc_2010" }
    private val course = source.copy(sourceUrl = "", lessons = source.lessons.take(3).mapIndexed { i, lesson ->
        lesson.copy(id = "search-$i", title = listOf("Limites", "Dérivées", "Intégrales")[i],
            objective = listOf("Approcher une valeur", "Étudier le sinus", "Calculer une aire")[i],
            learningNotes = "Notes de la section $i", sourceUrls = emptyList())
    })

    @Test fun searchIgnoresFrenchAccentsAndCasePreservesSourceOrderAndMatchesAllWords() {
        assertEquals(listOf(0, 1, 2), matchingLessonIndices(course, "  "))
        assertEquals(listOf(1), matchingLessonIndices(course, "DERIVEES sinus"))
        assertEquals(emptyList(), matchingLessonIndices(course, "derivees aire"))
        assertEquals(listOf(2), matchingLessonIndices(course, "aire"))
    }

    @Test fun filteredResultJumpsToOriginalLockedSectionWithoutStartingExercises() = runDesktopComposeUiTest(width = 1100, height = 800) {
        var starts = 0
        setContent { LearningAppTheme { Surface {
            CourseReaderScreen(course, { false }, { starts++ }, {}, searchableContents = true)
        } } }
        onNodeWithText("Sommaire").performClick()
        onNodeWithText("Rechercher une leçon").performTextInput("integrales")
        onNodeWithText("3. Intégrales").performClick()
        onNodeWithText("Notes de la section 2").assertIsDisplayed()
        onNodeWithText("Pratiquer · Intégrales").assertIsNotEnabled()
        runOnIdle { assertEquals(0, starts) }
    }

    @Test fun emptyResultsCanBeClearedAtBothWidths() {
        listOf(500, 1100).forEach { width ->
            runDesktopComposeUiTest(width = width, height = 800) {
                setContent { LearningAppTheme { Surface { CourseReaderScreen(course, { false }, {}, {}, searchableContents = true) } } }
                onNodeWithText("Sommaire").performClick()
                onNodeWithText("Rechercher une leçon").performTextInput("introuvable")
                onNodeWithText("Aucune leçon trouvée.", substring = true).assertIsDisplayed()
                onNodeWithText("Effacer").performClick()
                onNodeWithText("1. Limites").assertIsDisplayed()
                onNodeWithText("3 leçons").assertIsDisplayed()
                onNodeWithText("Rechercher une leçon").performTextInput("derivees")
                onNodeWithText("2. Dérivées").assertIsDisplayed()
                val file = File("build/reports/ui-snapshots/course-search-$width.png"); file.parentFile.mkdirs()
                Image.makeFromBitmap(onNode(isRoot() and hasAnyDescendant(hasText("Sommaire du cours"))).captureToImage().asSkiaBitmap()).use { image -> image.encodeToData()!!.use { file.writeBytes(it.bytes) } }
            }
        }
    }

    @Test fun searchIsAbsentFromExistingNonMathReaderByDefault() = runDesktopComposeUiTest(width = 900, height = 600) {
        setContent { LearningAppTheme { Surface { CourseReaderScreen(course, { true }, {}, {}) } } }
        onNodeWithText("Sommaire").performClick()
        onNodeWithText("Rechercher une leçon").assertDoesNotExist()
        onNodeWithText("2. Dérivées").assertExists()
    }
}
