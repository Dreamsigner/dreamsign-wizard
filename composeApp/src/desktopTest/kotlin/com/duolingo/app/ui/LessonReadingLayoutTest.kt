@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package com.duolingo.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import com.duolingo.app.LearningAppTheme
import com.duolingo.app.data.SampleData
import com.duolingo.app.data.CourseResources
import com.duolingo.app.ui.math.DesktopMathRenderer
import com.duolingo.app.ui.math.MathRenderingProvider
import org.jetbrains.skia.Image
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LessonReadingLayoutTest {
    @Test fun videoActionOpensTheSpecificVideoInsteadOfItsMitProvenancePage() = runDesktopComposeUiTest(width = 1100, height = 500) {
        val video = CourseResources.forSession("mit_18_06sc_2011", 1).first { it.kind == "video" && it.url.startsWith("https://www.youtube.com/watch?v=") }
        var opened: String? = null
        val handler = object : UriHandler { override fun openUri(uri: String) { opened = uri } }
        setContent { LearningAppTheme { Surface(color = White) {
            CompositionLocalProvider(LocalUriHandler provides handler) { ReferenceLinks(listOf(video)) }
        } } }
        onNode(hasText("Vidéo · YouTube") and hasClickAction()).performClick()
        runOnIdle {
            assertEquals(video.url, opened)
            assertTrue(opened != video.sourceUrl)
        }
    }

    @Test fun shellNotesHaveRealHeadingsAndLiteralBashCodeAtDesktopAndCompactWidths() {
        listOf(1100, 500).forEach { width ->
            runDesktopComposeUiTest(width = width, height = 1000) {
                val lesson = SampleData.sampleSubjects.flatMap { it.units }.first { it.id == "mit_missing_semester_2026" }.lessons.first()
                setContent { LearningAppTheme { MathRenderingProvider(DesktopMathRenderer) {
                    Surface(color = White) {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) { LessonNotes(lesson) }
                    }
                } } }
                onNodeWithText("Comprendre ce que fait le shell").assertExists()
                onNodeWithText("pwd\nprintf '%s\\n' 'bleu' 'rouge' 'bleu' | sort | uniq -c").assertExists()
                save("lesson-shell-$width")
                onNodeWithText("À retenir").performScrollTo().assertIsDisplayed()
            }
        }
    }

    @Test fun actualCalculusNotesRenderTheirCompleteFractionsAtTwoWidths() {
        listOf(1100, 500).forEach { width ->
            listOf(0, 1).forEach { index ->
                runDesktopComposeUiTest(width = width, height = 1000) {
                    val lesson = SampleData.sampleSubjects.flatMap { it.units }.first { it.id == "mit_18_01sc_2010" }.lessons[index]
                    setContent { LearningAppTheme { MathRenderingProvider(DesktopMathRenderer) {
                        Surface(color = White) {
                            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) { LessonNotes(lesson, typesetMath = true) }
                        }
                    } } }
                    val formulas = com.duolingo.app.ui.math.parseLearningText(lesson.learningNotes, true)
                        .filterIsInstance<com.duolingo.app.ui.math.LearningTextSegment.Math>()
                    formulas.forEach { formula ->
                        assertTrue(com.duolingo.app.ui.math.renderMathBitmap(formula.latex, 24f, Ink, formula.display) != null, formula.latex)
                    }
                    // Explicit TeX display blocks must reach the renderer, including ones without a backslash.
                    onNodeWithText(if (index == 0) "y=f(a)+f'(a)(x-a)" else "(x^{n})'=nx^{n-1}", substring = false).assertDoesNotExist()
                    save("lesson-calculus-fractions-${index + 1}-$width")
                }
            }
        }
    }

    @Test fun linearAlgebraPreviewDisplaysAuthoredMatrixBlocksAndWorkedExample() = runDesktopComposeUiTest(width = 1100, height = 1000) {
        val lesson = SampleData.sampleSubjects.flatMap { it.units }.first { it.id == "mit_18_06sc_2011" }.lessons[1]
        setContent { LearningAppTheme { MathRenderingProvider(DesktopMathRenderer) {
            Surface(color = White) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) { LessonNotes(lesson, typesetMath = true) }
            }
        } } }
        onNodeWithText("Des vecteurs aux sous-espaces").assertExists()
        onNodeWithText("Exemple résolu").assertExists()
        save("lesson-linear-preview")
    }

    private fun ComposeUiTest.save(name: String) {
        val output = File("build/reports/ui-snapshots/$name.png")
        output.parentFile.mkdirs()
        Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap()).use { image ->
            image.encodeToData()!!.use { output.writeBytes(it.bytes) }
        }
    }
}
