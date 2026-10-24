@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package com.duolingo.app.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.test.*
import com.duolingo.app.LearningAppTheme
import com.duolingo.app.model.Exercise
import com.duolingo.app.model.LearningUnit
import com.duolingo.app.model.Lesson
import com.duolingo.app.model.Subject
import com.duolingo.app.model.SubjectCategory
import com.duolingo.app.storage.ProgressStore
import com.duolingo.app.ui.math.DesktopMathRenderer
import com.duolingo.app.ui.math.MathRenderingProvider
import com.duolingo.app.ui.share.*
import com.duolingo.app.viewmodel.AppViewModel
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import java.io.File

class ChallengeShareTest {
    @Test
    fun shareDtoAcceptsOnlySelfContainedQuizFormatsAndOmitsPrivateFields() {
        val lesson = Lesson("lesson", "Unités", exercises = emptyList())
        val exercise = Exercise.MathChallenge(
            id = "math", prompt = "Calcule \$v=12\\,\\mathrm{m/s}\$", formula = "\\frac{\\mathrm{m}}{\\mathrm{s}^2}",
            options = listOf("12", "6"), correctOptionIndex = 1, explanation = "PRIVATE_EXPLANATION", hint = "PRIVATE_HINT",
        )
        val card = challengeShareCard(lesson, exercise)
        assertNotNull(card)
        val text = card.asShareText()
        assertTrue("m/s²" in text || "m/s puissance 2" in text)
        assertFalse("\$" in text)
        assertFalse("\\frac" in text)
        assertFalse("PRIVATE_EXPLANATION" in text)
        assertFalse("PRIVATE_HINT" in text)
        assertFalse("correctOptionIndex" in text)
        assertNull(challengeShareCard(lesson, Exercise.Translation("translation", "Traduire", "a sentence", listOf("a"), listOf("a"), "explication", "indice")))
        assertNull(challengeShareCard(lesson, exercise.copy(prompt = "x".repeat(421))))
    }

    @Test
    fun renderedCardCapturesAsFullImageAndShowsActionsAtCompactAndWideWidths() {
        listOf(360, 1000).forEach { width ->
            runDesktopComposeUiTest(width = width, height = if (width == 360) 480 else 700, testTimeout = 60.seconds) {
                val service = RecordingShareService(shouldSave = true)
                val clipboard = FakeClipboard()
                val card = ChallengeShareCard(
                    "Cinématique", "Quelle distance parcourt un objet ?",
                    listOf("12 m", "24 m", "36 m", "48 m"),
                    formula = "\\frac{\\mathrm{m}}{\\mathrm{s}^2}",
                )
                setContent {
                    LearningAppTheme {
                        MathRenderingProvider(DesktopMathRenderer) {
                            CompositionLocalProvider(LocalChallengeShareService provides service, LocalClipboardManager provides clipboard) {
                                ChallengeShareDialog(card) {}
                            }
                        }
                    }
                }
                onNodeWithText("Partager un défi").assertIsDisplayed()
                onNodeWithText("Quelle distance parcourt un objet ?").assertExists()
                onNodeWithText("PRIVATE_HINT").assertDoesNotExist()
                onNodeWithText("Enregistrer PNG").performClick()
                waitUntil(timeoutMillis = 5_000L) { service.savedImage != null }
                val image = assertNotNull(service.savedImage)
                assertTrue(image.width > 200)
                assertTrue(image.height > 400)
                val png = assertNotNull(service.savedPng)
                assertTrue(png.size > 1_000)
                Image.makeFromEncoded(png).use { decoded ->
                    assertEquals(image.width, decoded.width)
                    assertEquals(image.height, decoded.height)
                }
                onNodeWithText("Image PNG enregistrée.").assertExists()
                onNodeWithText("Copier le texte").performClick()
                onNodeWithText("Question copiée dans le presse-papiers.").assertExists()
                assertTrue(assertNotNull(clipboard.copiedText).text.contains("m/s"))
                assertFalse(clipboard.copiedText?.text.orEmpty().contains("\\frac"))
                if (width == 360) {
                    val cardTop = onNodeWithTag("challenge-share-card").fetchSemanticsNode().boundsInRoot.top
                    val lastOptionBottom = onNodeWithText("48 m").fetchSemanticsNode().boundsInRoot.bottom
                    assertTrue(image.height >= (lastOptionBottom - cardTop).toInt(), "The export must include the final option below the preview viewport")
                    val output = File("build/reports/ui-snapshots/challenge-export-360.png")
                    check(output.parentFile.isDirectory || output.parentFile.mkdirs())
                    output.writeBytes(png)
                }
                saveSnapshot("challenge-share-$width")
            }
        }
    }

    @Test
    fun imageSaveCancellationIsReportedWithoutClaimingSuccess() = runDesktopComposeUiTest(width = 420, height = 700) {
        val service = RecordingShareService(shouldSave = false)
        setContent {
            LearningAppTheme {
                CompositionLocalProvider(LocalChallengeShareService provides service) {
                    ChallengeShareDialog(ChallengeShareCard("Leçon", "Question ?", listOf("Un choix"))) {}
                }
            }
        }
        onNodeWithText("Enregistrer PNG").performClick()
        waitUntil(timeoutMillis = 5_000L) { service.attempted }
        onNodeWithText("Enregistrement annulé.").assertExists()
        onNodeWithText("Image PNG enregistrée.").assertDoesNotExist()
    }

    @Test
    fun failedImageSaveCanBeRetried() = runDesktopComposeUiTest(width = 500, height = 700) {
        val service = RecordingShareService(shouldSave = true, failFirstAttempt = true)
        setContent {
            LearningAppTheme {
                CompositionLocalProvider(LocalChallengeShareService provides service) {
                    ChallengeShareDialog(ChallengeShareCard("Leçon", "Question ?", listOf("Un choix"))) {}
                }
            }
        }
        onNodeWithText("Enregistrer PNG").performClick()
        waitUntil(timeoutMillis = 5_000L) { service.attempts == 1 }
        onNodeWithText("Impossible de créer ou d’enregistrer l’image.").assertExists()
        onNodeWithText("Enregistrer PNG").performClick()
        waitUntil(timeoutMillis = 5_000L) { service.savedImage != null }
        onNodeWithText("Image PNG enregistrée.").assertExists()
    }

    @Test
    fun quizShareOpensCardAndClosingItPreservesTheLearnersSelection() = runDesktopComposeUiTest(width = 1000, height = 800) {
        val lesson = Lesson(
            id = "share-lesson", title = "Quiz partagé", isLocked = false, requiresPreviousLesson = false,
            exercises = listOf(Exercise.MultipleChoice("share-question", "Quel choix ?", listOf("Réponse A", "Réponse B"), 1, "Explication secrète", "Indice secret")),
        )
        val model = AppViewModel(MemoryStore(), today = { "2026-10-24" }, catalogue = listOf(
            Subject("share-subject", "Matière", "", "", SubjectCategory.MATHEMATICS, listOf(LearningUnit("share-unit", 1, "Unité", "", listOf(lesson))))
        ))
        model.startLesson(lesson)
        try {
            setContent { LearningAppTheme { MathRenderingProvider(DesktopMathRenderer) { LessonQuizScreen(model) } } }
            onNodeWithText("Passer aux exercices").performClick()
            onNodeWithText("Réponse B").performClick()
            assertEquals(1, model.selectedOptionIndex.value)
            onNodeWithText("Partager ce défi").performClick()
            onNodeWithText("Partager un défi").assertIsDisplayed()
            onNodeWithText("Explication secrète").assertDoesNotExist()
            onNodeWithText("Indice secret").assertDoesNotExist()
            onNodeWithText("Fermer").performClick()
            assertEquals(1, model.selectedOptionIndex.value)
            onNodeWithText("Vérifier ma réponse").assertIsEnabled()
        } finally { model.close() }
    }

    private class RecordingShareService(private val shouldSave: Boolean, private val failFirstAttempt: Boolean = false) : ChallengeShareService {
        override val canSaveImage = true
        var savedImage: ImageBitmap? = null
        var savedPng: ByteArray? = null
        var attempted = false
        var attempts = 0
        override suspend fun saveImage(image: ImageBitmap): Boolean {
            attempted = true
            attempts++
            if (failFirstAttempt && attempts == 1) error("temporary encoder failure")
            if (shouldSave) {
                savedImage = image
                savedPng = Image.makeFromBitmap(image.asSkiaBitmap()).use { source ->
                    source.encodeToData(EncodedImageFormat.PNG)?.use { it.bytes.copyOf() }
                }
            }
            return shouldSave
        }
    }

    private class MemoryStore : ProgressStore {
        private var value: String? = null
        override fun read(): String? = value
        override fun write(value: String) { this.value = value }
    }

    private class FakeClipboard : ClipboardManager {
        var copiedText: AnnotatedString? = null
        override fun getText(): AnnotatedString? = copiedText
        override fun setText(annotatedString: AnnotatedString) { copiedText = annotatedString }
    }

    private fun ComposeUiTest.saveSnapshot(name: String) {
        val output = File("build/reports/ui-snapshots/$name.png")
        check(output.parentFile.isDirectory || output.parentFile.mkdirs())
        val image = Image.makeFromBitmap(onNodeWithTag("challenge-share-dialog").captureToImage().asSkiaBitmap())
        try {
            val encoded = checkNotNull(image.encodeToData()) { "PNG encoding failed" }
            try { output.writeBytes(encoded.bytes) } finally { encoded.close() }
        } finally { image.close() }
    }
}
