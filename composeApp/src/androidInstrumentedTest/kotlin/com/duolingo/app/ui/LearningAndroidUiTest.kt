@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package com.duolingo.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.duolingo.app.LearningAppTheme
import com.duolingo.app.media.LocalPracticeMedia
import com.duolingo.app.media.PlatformAudioPlayer
import com.duolingo.app.media.PlatformAudioRecorder
import com.duolingo.app.media.PracticeMedia
import com.duolingo.app.model.Exercise
import com.duolingo.app.model.LearningUnit
import com.duolingo.app.model.Lesson
import com.duolingo.app.model.Subject
import com.duolingo.app.model.SubjectCategory
import com.duolingo.app.model.UserStats
import com.duolingo.app.storage.ProgressStore
import com.duolingo.app.viewmodel.AppViewModel
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Android instrumentation against real composables in the debug-manifest ComponentActivity.
 * Does not launch MainActivity, initialize platform storage, request permissions or use media.
 * https://developer.android.com/develop/ui/compose/testing#setup
 * Compose Multiplatform 1.9.3 maps Android Compose UI to 1.9.4.
 */
@RunWith(AndroidJUnit4::class)
class LearningAndroidUiTest {
    @get:Rule
    val compose = createComposeRule()

    private val models = mutableListOf<AppViewModel>()

    @After
    fun closeFixtures() {
        // Explicitly release VM background scopes even if a UI assertion fails.
        // All persistence here is the fixture's volatile String, never SharedPreferences.
        models.forEach { it.close() }
        models.clear()
    }

    @Test
    fun notesWrongRetryAndCorrectAnswerCompleteTheLessonOnAndroid() {
        val fixture = showFixture()
        compose.onNodeWithText("Continuer · Addition instrumentée").performScrollTo().performClick()
        compose.onNodeWithText("Comprendre avant de pratiquer").assertIsDisplayed()
        compose.onNodeWithText(NOTES).assertExists()
        compose.onNodeWithText(PROMPT).assertDoesNotExist()
        compose.onNodeWithText("Passer aux exercices").performClick()
        compose.onNodeWithText(PROMPT).assertIsDisplayed()
        compose.onNodeWithText("Vérifier ma réponse").assertIsNotEnabled()

        compose.onNodeWithText("Cinq").performScrollTo().performClick()
        compose.onNodeWithText("Vérifier ma réponse").performClick()
        compose.onNodeWithText("À corriger · consulte l’explication puis réessaie").assertIsDisplayed()
        compose.onNodeWithText("Continuer").assertDoesNotExist()
        compose.onNodeWithText("Six").assertIsNotEnabled()
        compose.runOnIdle {
            assertEquals(4, fixture.model.userStats.value.hearts)
            assertEquals(0, fixture.model.userStats.value.xp)
        }

        compose.onNodeWithText("Réessayer").performClick()
        compose.onNodeWithText("Vérifier ma réponse").assertIsNotEnabled()
        compose.onNodeWithText("Six").performScrollTo().performClick()
        compose.onNodeWithText("Vérifier ma réponse").performClick()
        compose.onNodeWithText("Bonne réponse").assertIsDisplayed()
        compose.onNodeWithText("Continuer").performClick()
        compose.onNodeWithText("Une leçon de plus !").assertIsDisplayed()
        compose.onNodeWithText("+20 XP. Ta progression avance.").assertExists()
        compose.runOnIdle {
            assertEquals(20, fixture.model.userStats.value.xp)
            assertEquals(setOf(LESSON_ID), fixture.model.userStats.value.completedLessonIds)
            assertNotNull(fixture.store.read())
            assertEquals(20, fixture.store.savedStats().xp)
        }
    }

    @Test
    fun courseProfileNavigationAndHelpPreserveTheAttempt() {
        val fixture = showFixture()
        compose.onNodeWithText("Profil").performClick()
        compose.onNodeWithText("Mon parcours").assertIsDisplayed()
        compose.onNodeWithText("Profil").assertIsSelected()
        compose.onNodeWithText("Cours").performClick()
        compose.onNodeWithText("Mes cours").assertIsDisplayed()
        compose.onNodeWithText("Cours").assertIsSelected()

        compose.onNodeWithText("Continuer · Addition instrumentée").performScrollTo().performClick()
        compose.onNodeWithText("Passer aux exercices").performClick()
        compose.onNodeWithText("Six").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Ouvrir le cours et l’indice").performClick()
        compose.onNodeWithText("Le cours et l’indice").assertIsDisplayed()
        compose.onNodeWithText(NOTES).assertExists()
        compose.onNodeWithText("Indice pour cet exercice").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(HINT).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Revenir à la leçon").performClick()
        compose.onNodeWithText(PROMPT).assertIsDisplayed()
        compose.onNodeWithText("Six").assertIsSelected()
        compose.onNodeWithText("Vérifier ma réponse").assertIsEnabled()
        compose.runOnIdle {
            assertEquals(5, fixture.model.userStats.value.hearts)
            assertEquals(false, fixture.model.isAnswerChecked.value)
            assertEquals(0, fixture.model.userStats.value.xp)
        }
    }

    @Test
    fun nativeViewportRendersContentAndNavigationWithoutOpeningProductionStorage() {
        val fixture = showFixture()
        compose.onNodeWithText("Mes cours").assertIsDisplayed()
        compose.onNodeWithText("Cours").assertIsDisplayed()
        compose.onNodeWithText("Profil").assertIsDisplayed()
        compose.onNodeWithText("Continuer · Addition instrumentée").performScrollTo().assertIsDisplayed()
        // Native Android capture API; no Computer Use or screenshot-driven input.
        // Keep the image in memory: no permissions, user files or external storage writes.
        val image = compose.onRoot().captureToImage()
        assertTrue(image.width > 0)
        assertTrue(image.height > 0)
        compose.runOnIdle {
            assertEquals(0, fixture.model.userStats.value.xp)
            assertEquals(null, fixture.store.read())
        }
    }

    private fun showFixture(): Fixture {
        val fixture = Fixture()
        models += fixture.model
        compose.setContent { InstrumentedFixtureTheme { DuolingoApp(fixture.model) } }
        return fixture
    }

    private class MemoryProgressStore : ProgressStore {
        @Volatile private var value: String? = null
        override fun read(): String? = value
        override fun write(value: String) { this.value = value }
        fun savedStats(): UserStats = Json.decodeFromString(checkNotNull(read()))
    }

    private class Fixture {
        val store = MemoryProgressStore()
        private val exercise = Exercise.MultipleChoice(
            id = "android_ui_addition", prompt = PROMPT,
            options = listOf("Cinq", "Six"), correctOptionIndex = 1,
            explanation = "Trois objets plus trois objets donnent six objets.", hint = HINT
        )
        private val lesson = Lesson(
            id = LESSON_ID, title = "Addition instrumentée", xpReward = 20,
            isLocked = false, exercises = listOf(exercise),
            objective = "Lire une note puis corriger une petite addition.",
            learningNotes = NOTES, requiresPreviousLesson = false
        )
        private val catalogue = listOf(Subject(
            id = "android_ui_subject", title = "Matière de test", icon = "",
            description = "Catalogue original isolé pour instrumentation.",
            category = SubjectCategory.MATHEMATICS,
            units = listOf(LearningUnit("android_ui_unit", 1, "Cours de test", "Une seule leçon originale.", listOf(lesson)))
        ))
        val model = AppViewModel(store, today = { "2026-10-20" }, catalogue = catalogue)
    }

    companion object {
        private const val LESSON_ID = "android_ui_lesson"
        private const val PROMPT = "Combien font trois plus trois ?"
        private const val NOTES = "Réunis trois objets avec trois autres objets : le groupe obtenu contient six objets."
        private const val HINT = "Compte trois objets puis ajoute trois objets."
    }
}

@Composable
private fun InstrumentedFixtureTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalUriHandler provides NoExternalLinks,
        LocalPracticeMedia provides NoNativeMedia
    ) { LearningAppTheme { content() } }
}

private object NoExternalLinks : UriHandler {
    override fun openUri(uri: String): Unit = error("Instrumentation fixture must not open external links: $uri")
}

/** Fail before any native audio/storage/permission call if a fixture accidentally uses media. */
private object NoNativeMedia : PracticeMedia {
    override fun createPlayer(): PlatformAudioPlayer = error("No player in this UI fixture")
    override fun createRecorder(): PlatformAudioRecorder = error("No microphone in this UI fixture")
    override suspend fun loadResource(path: String): ByteArray = error("No audio resources in this UI fixture")
    override fun saveRecording(id: String, bytes: ByteArray): Unit = error("No recording storage in this UI fixture")
    override fun loadRecording(id: String): ByteArray? = error("No recording storage in this UI fixture")
    override fun bindPlayerLifecycle(player: PlatformAudioPlayer, onBackground: () -> Unit): () -> Unit = error("No native lifecycle in this UI fixture")
    override fun bindRecorderLifecycle(recorder: PlatformAudioRecorder, onBackground: () -> Unit): () -> Unit = error("No native lifecycle in this UI fixture")
}
