@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package com.duolingo.app.ui

import com.duolingo.app.LearningAppTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.graphics.asSkiaBitmap
import com.duolingo.app.data.generated.GeneratedCourses
import com.duolingo.app.media.*
import com.duolingo.app.model.*
import com.duolingo.app.storage.ProgressStore
import com.duolingo.app.viewmodel.AppViewModel
import java.io.IOException
import java.io.File
import org.jetbrains.skia.Image
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

/** Real composables, controlled media events, and memory-only drafts. Never opens a device. */
class AudioPracticeUiTest {
    private val models = mutableListOf<AppViewModel>()

    @AfterTest fun releaseModelsAfterEveryOutcome() {
        models.forEach { it.close() }
        models.clear()
    }

    @Test fun actualSpokenInstructionsRenderOffscreenAtCompactWidth() = runDesktopComposeUiTest(width = 400, height = 900, testTimeout = 60.seconds) {
        assertTrue(java.awt.GraphicsEnvironment.isHeadless(), "UI checks must not require a desktop or display server")
        val actual = GeneratedCourses.unitsFor(SubjectCategory.LANGUAGES).flatMap { it.lessons }
            .flatMap { it.exercises }.filterIsInstance<Exercise.SpokenProduction>().first()
        val model = open(actual, FakeMedia())
        onNodeWithText("Ta production orale").assertIsDisplayed()
        onNodeWithText(actual.prompt).assertIsDisplayed()
        onNodeWithText("Terminer ma réécoute et mon bilan").assertIsNotEnabled()
        val output = File("build/reports/ui-snapshots/spoken-compact.png")
        check(output.parentFile.isDirectory || output.parentFile.mkdirs())
        Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap()).use { image ->
            checkNotNull(image.encodeToData()).use { output.writeBytes(it.bytes) }
        }
        model.close()
    }
    private class MemoryProgress : ProgressStore {
        private var json: String? = null
        override fun read() = json
        override fun write(value: String) { json = value }
    }
    private class FakePlayer : PlatformAudioPlayer {
        var plays = 0
        var complete: (() -> Unit)? = null
        override fun play(bytes: ByteArray, onComplete: () -> Unit, onError: (String) -> Unit) {
            plays++; complete = onComplete
        }
        override fun stop() { complete = null }
        fun finish() { val callback = complete; complete = null; callback?.invoke() }
    }
    private class FakeRecorder : PlatformAudioRecorder {
        var complete: ((ByteArray) -> Unit)? = null
        var cancels = 0
        override fun start(maxDurationSeconds: Int, onStarted: () -> Unit, onComplete: (ByteArray) -> Unit, onError: (String) -> Unit) {
            complete = onComplete; onStarted()
        }
        override fun stop() {
            val callback = complete; complete = null
            callback?.invoke(pcmRecordingToWav(ByteArray(RECORDING_RATE * 2 * 6)))
        }
        override fun cancel() { cancels++; complete = null }
    }
    private class FakeMedia : PracticeMedia {
        val player = FakePlayer()
        val recorder = FakeRecorder()
        val recordings = ConcurrentHashMap<String, ByteArray>()
        var failLoad = false
        @Volatile var failSave = false
        var background: (() -> Unit)? = null
        override fun createPlayer() = player
        override fun createRecorder() = recorder
        override suspend fun loadResource(path: String): ByteArray {
            if (failLoad) throw IOException("Controlled missing resource")
            return byteArrayOf(1, 2, 3)
        }
        override fun saveRecording(id: String, bytes: ByteArray) {
            if (failSave) throw IOException("Controlled full disk")
            recordings[id] = bytes.copyOf()
        }
        override fun loadRecording(id: String) = recordings[id]?.copyOf()
        override fun bindPlayerLifecycle(player: PlatformAudioPlayer, onBackground: () -> Unit): () -> Unit = { }
        override fun bindRecorderLifecycle(recorder: PlatformAudioRecorder, onBackground: () -> Unit): () -> Unit {
            background = { recorder.cancel(); onBackground() }
            return { background = null }
        }
    }
    private val listening = Exercise.ListeningChoice("listen_test", "Quand part le train ?", "fixture.wav", "Le train part à midi.",
        listOf("À midi", "Le soir"), 0, "Le document indique midi.", "Écoute l’horaire.")
    private val speaking = Exercise.SpokenProduction(id = "speak_test", prompt = "Présente une activité.",
        maxDurationSeconds = 120, reviewCriteria = listOf("J’ai donné un exemple.", "J’ai relu mes idées à l’écoute."),
        exampleOutline = "Présenter puis illustrer.", explanation = "Auto-évaluation de pratique.", hint = "Développe un exemple.")

    private fun ComposeUiTest.open(exercise: Exercise, media: FakeMedia): AppViewModel {
        val lesson = Lesson("audio_lesson", "Atelier test", isLocked = false, exercises = listOf(exercise),
            learningNotes = "Consignes originales de la fixture de test.")
        val subject = Subject("audio", "TCF", "", "", SubjectCategory.LANGUAGES,
            listOf(LearningUnit("test_unit", 1, "Atelier", "", listOf(lesson))))
        val model = AppViewModel(MemoryProgress(), { "2026-10-20" }, listOf(subject))
        models += model
        model.startLesson(lesson)
        setContent { LearningAppTheme { CompositionLocalProvider(LocalPracticeMedia provides media) { LessonQuizScreen(model) } } }
        onNodeWithText("Passer aux exercices").performClick()
        return model
    }

    @Test fun listeningNeedsCompletedPlaybackAndStoppingDoesNotUnlockValidation() = runDesktopComposeUiTest(testTimeout = 60.seconds) {
        val media = FakeMedia()
        val model = open(listening, media)
        onNodeWithText("À midi").performScrollTo().performClick()
        onNodeWithText("Vérifier ma réponse").assertIsNotEnabled()
        onNodeWithText("Écouter le document").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5000) { media.player.plays == 1 }
        onNodeWithText("Arrêter la lecture").performScrollTo().performClick()
        onNodeWithText("Vérifier ma réponse").assertIsNotEnabled()
        onNodeWithText("Écouter le document").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5000) { media.player.plays == 2 }
        runOnIdle { media.player.finish() }
        waitUntil(timeoutMillis = 5000) { model.heardAudioPaths.value.isNotEmpty() }
        onNodeWithText("Vérifier ma réponse").assertIsEnabled().performClick()
        onNodeWithText("Bonne réponse").assertIsDisplayed()
        onNodeWithText(listening.transcript).assertExists()
        model.close()
    }

    @Test fun missingAudioIsVisibleAndTranscriptModeIsExplicitlyTracked() = runDesktopComposeUiTest(testTimeout = 60.seconds) {
        val media = FakeMedia().apply { failLoad = true }
        val model = open(listening, media)
        onNodeWithText("Écouter le document").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5000) { onAllNodesWithText("Le document audio n’a pas pu être chargé.", substring = true).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("À midi").performScrollTo().performClick()
        onNodeWithText("Vérifier ma réponse").assertIsNotEnabled()
        onNodeWithText("Accessibilité : travailler à partir de la transcription").performScrollTo().performClick()
        onNodeWithText(listening.transcript).assertExists()
        onNodeWithText("Vérifier ma réponse").assertIsEnabled().performClick()
        onNodeWithText("Continuer").performClick()
        runOnIdle { assertEquals(setOf("audio_lesson"), model.userStats.value.textModeCompletedLessonIds) }
        model.close()
    }

    @Test fun spokenCaptureSaveFailureRetryReplayAndReviewUseActualControls() = runDesktopComposeUiTest(testTimeout = 60.seconds) {
        val media = FakeMedia().apply { failSave = true }
        val model = open(speaking, media)
        val finishLabel = "Terminer ma réécoute et mon bilan"
        onNodeWithText(finishLabel).assertIsNotEnabled()
        onNodeWithText("Enregistrer ma réponse").performScrollTo().performClick()
        onNodeWithText("Arrêter et sauvegarder").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5000) { onAllNodesWithText("Réessayer la sauvegarde").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText(finishLabel).assertIsNotEnabled()
        runOnIdle { media.failSave = false }
        onNodeWithText("Réessayer la sauvegarde").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5000) { model.verifiedRecordingMillis.value == 6000L }
        onNodeWithText(finishLabel).assertIsNotEnabled()
        onNodeWithText("Réécouter ma réponse").performScrollTo().performClick()
        runOnIdle { media.player.finish() }
        waitUntil(timeoutMillis = 5000) { model.recordingReplayed.value }
        speaking.reviewCriteria.forEach { onNodeWithText(it).performScrollTo().performClick() }
        onNodeWithText(finishLabel).assertIsEnabled().performClick()
        onNodeWithText("Auto-évaluation terminée").assertExists()
        onNodeWithText("Continuer").performClick()
        runOnIdle { assertTrue(model.isLessonCompleted.value); assertEquals(1, media.recordings.size) }
        model.close()
    }

    @Test fun leavingForegroundCancelsCaptureAndKeepsCompletionDisabled() = runDesktopComposeUiTest(testTimeout = 60.seconds) {
        val media = FakeMedia()
        val model = open(speaking, media)
        onNodeWithText("Enregistrer ma réponse").performScrollTo().performClick()
        runOnIdle { media.background?.invoke() }
        onNodeWithText("Enregistrer ma réponse").assertExists()
        onNodeWithText("Terminer ma réécoute et mon bilan").assertIsNotEnabled()
        runOnIdle { assertTrue(media.recordings.isEmpty()); assertTrue(media.recorder.cancels > 0) }
        model.close()
    }
}
