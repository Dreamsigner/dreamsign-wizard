@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
package com.dreamsigner.wizard.ui

import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import com.dreamsigner.wizard.LearningAppTheme
import com.dreamsigner.wizard.data.SampleData
import org.jetbrains.skia.Image
import java.io.File
import kotlin.test.*

class LessonFavoritesUiTest {
    private val source = SampleData.sampleSubjects.flatMap { it.units }.first { it.id == "mit_18_01sc_2010" }
    private val course = source.copy(id = "favorite-test", sourceUrl = "", lessons = source.lessons.take(3).mapIndexed { i, lesson ->
        lesson.copy(id = "favorite-$i", title = listOf("Limites", "Dérivées", "Intégrales")[i],
            objective = listOf("Approcher une valeur", "Étudier le sinus", "Calculer une aire")[i],
            learningNotes = "Notes de la section $i", sourceUrls = emptyList())
    })

    @Test fun addingAndRemovingAFavoriteDoesNotUnlockOrStartPractice() = runDesktopComposeUiTest(width = 900, height = 850) {
        var starts = 0
        setContent { LearningAppTheme { Surface {
            var favorites by remember { mutableStateOf(emptySet<String>()) }
            CourseReaderScreen(course, { false }, { starts++ }, {}, searchableContents = true,
                favoriteLessonIds = favorites, onToggleFavorite = { id -> favorites = if (id in favorites) favorites - id else favorites + id },
                favoriteSaveStatus = "Progression enregistrée sur cet appareil.")
        } } }
        onNodeWithText("Sommaire").performClick()
        onNodeWithText("1. Limites").performClick()
        onNodeWithContentDescription("Ajouter aux favoris · Limites")
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            .performKeyInput { pressKey(Key.Enter) }
        onNodeWithContentDescription("Retirer des favoris · Limites").assertIsDisplayed().performClick()
        onNodeWithContentDescription("Ajouter aux favoris · Limites").assertIsDisplayed()
        onNodeWithText("Pratiquer · Limites").assertIsNotEnabled()
        runOnIdle { assertEquals(0, starts) }
    }

    @Test fun favoritesCombineWithSearchAndJumpToOriginalLockedLessonAtBothWidths() {
        listOf(500, 900).forEach { width -> runDesktopComposeUiTest(width = width, height = 850) {
            setContent { LearningAppTheme { Surface {
                CourseReaderScreen(course, { false }, {}, {}, searchableContents = true,
                    favoriteLessonIds = setOf("favorite-0", "favorite-2"), onToggleFavorite = {})
            } } }
            onNodeWithText("Sommaire").performClick()
            onNodeWithText("Favoris uniquement").performClick().assertIsOn()
            onNodeWithText("1. Limites").assertIsDisplayed()
            onNodeWithText("2. Dérivées").assertDoesNotExist()
            onNodeWithText("3. Intégrales").assertIsDisplayed()
            onNodeWithText("Rechercher une leçon").performTextInput("AIRE")
            onNodeWithText("1 leçon").assertIsDisplayed()
            val file = File("build/reports/ui-snapshots/lesson-favorites-$width.png"); file.parentFile.mkdirs()
            Image.makeFromBitmap(onNode(isRoot() and hasAnyDescendant(hasText("Sommaire du cours"))).captureToImage().asSkiaBitmap()).use { image -> image.encodeToData()!!.use { file.writeBytes(it.bytes) } }
            onNodeWithText("3. Intégrales").performClick()
            onNodeWithText("Notes de la section 2").assertIsDisplayed()
            onNodeWithText("Pratiquer · Intégrales").assertIsNotEnabled()
        } }
    }

    @Test fun emptyFavoritesExplainHowToAddOneAndCanBeUnfiltered() = runDesktopComposeUiTest(width = 500, height = 850) {
        setContent { LearningAppTheme { Surface {
            CourseReaderScreen(course, { false }, {}, {}, searchableContents = true, onToggleFavorite = {})
        } } }
        onNodeWithText("Sommaire").performClick()
        onNodeWithText("Favoris uniquement").performClick()
        onNodeWithText("Aucune leçon favorite dans ce cours.", substring = true).assertIsDisplayed()
        onNodeWithText("Rechercher une leçon").performTextInput("limites")
        onNodeWithText("Aucun favori ne correspond à cette recherche.").assertIsDisplayed()
        onNodeWithText("Favoris uniquement").performClick()
        onNodeWithText("1. Limites").assertIsDisplayed()
    }

    @Test fun favoritesAreAbsentWithoutMathCallback() = runDesktopComposeUiTest(width = 900, height = 700) {
        setContent { LearningAppTheme { Surface { CourseReaderScreen(course, { false }, {}, {}) } } }
        onNodeWithContentDescription("Ajouter aux favoris · Limites").assertDoesNotExist()
        onNodeWithText("Sommaire").performClick()
        onNodeWithText("Favoris uniquement").assertDoesNotExist()
    }
}
