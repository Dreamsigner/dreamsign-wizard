@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package com.dreamsigner.wizard.ui.math

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.skia.Image
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class MathParagraphLayoutTest {
    @Test
    fun studioStyleAnswerWithMathAndCodeIsLaidOutAtTwoWidths() {
        val answer = "# Loi de Newton\n\n1. La force \\(F=ma\\) dépend de la masse.\n\n\\[F=2\\,\\mathrm{kg}\\times 3\\,\\mathrm{m/s^2}=6\\,\\mathrm{N}\\]\n\n```python\nforce = mass * acceleration\n```"
        listOf(360, 1000).forEach { width ->
            runDesktopComposeUiTest(width = width, height = 600) {
                setContent { MaterialTheme { Surface {
                    MathRenderingProvider(DesktopMathRenderer) {
                        com.dreamsigner.wizard.ui.LessonDocument(answer, typesetMath = true, modifier = Modifier.padding(12.dp))
                    }
                } } }
                onNodeWithText("Loi de Newton").assertIsDisplayed()
                onNodeWithText("force = mass * acceleration").assertIsDisplayed()
                save("studio-scientific-answer-$width")
            }
        }
    }

    @Test
    fun answerFractionsAndUnitsRemainReadableWithoutOverlappingRows() {
        val digits = requireNotNull(renderMathBitmap("1234", 18f, Color.Black, false))
        val fraction = requireNotNull(renderMathBitmap("\\frac{12}{34}", 18f, Color.Black, false))
        assertTrue(fraction.height >= digits.height * 2, "Both fraction rows need room for readable digits")
        listOf(360, 1000).forEach { width ->
            runDesktopComposeUiTest(width = width, height = 560) {
                setContent { MaterialTheme { Surface {
                    MathRenderingProvider(DesktopMathRenderer) {
                        com.dreamsigner.wizard.ui.OptionsList(listOf("12 m/s", "9,8 m/s²", "1/2 m/s", "12 \$\\frac{\\mathrm{m}}{\\mathrm{s}}\$", "1/2", "(x+1)/(x-1)"), 0, false, {}, typesetMath = true)
                    }
                } } }
                val rows = listOf("12 m/s", "9,8 m/s²", "1/2 m/s", "12 \$\\frac{\\mathrm{m}}{\\mathrm{s}}\$", "1/2", "(x+1)/(x-1)").map { option ->
                    onNodeWithText(option).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
                }
                rows.zipWithNext().forEach { (first, second) ->
                    assertTrue(first.bottom <= second.top, "Fractions must not overlap the following answer")
                }
                save("linear-units-readable-fractions-$width")
            }
        }
    }

    @Test
    fun inlineCommandsHaveCodeTypographyAndAVisibleBackground() = runDesktopComposeUiTest(width = 900, height = 300) {
        setContent { MaterialTheme { Surface {
            LearningText("Utiliser `pwd` pour afficher le répertoire courant.", Modifier.padding(24.dp).testTag("code-copy"),
                TextStyle(fontSize = 17.sp, lineHeight = 28.sp))
        } } }
        val layouts = mutableListOf<TextLayoutResult>()
        onNodeWithTag("code-copy").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val code = layouts.single().layoutInput.text.spanStyles.single().item
        assertTrue(code.fontFamily == FontFamily.Monospace)
        assertTrue(code.background != Color.Unspecified && code.background.alpha > 0f)
    }

    @Test
    fun indexedVectorsStayInsideOneNormallyWrappingParagraph() = runDesktopComposeUiTest(width = 1100, height = 500) {
        setContent {
            MaterialTheme {
                Surface {
                    Column(Modifier.padding(24.dp)) {
                        MathRenderingProvider(DesktopMathRenderer) {
                            LearningText(
                                "La transformation envoie (x₁,x₂,x₃) sur (x₁,x₂−x₁,x₃−x₂). Pour retrouver x à partir de b, on accumule : x₁=b₁, x₂=b₁+b₂ et x₃=b₁+b₂+b₃.",
                                Modifier.width(900.dp).testTag("paragraph"),
                                TextStyle(fontSize = 16.sp, lineHeight = 25.sp),
                                recognizeLegacyMath = true,
                            )
                        }
                    }
                }
            }
        }
        val bounds = onNodeWithTag("paragraph").getUnclippedBoundsInRoot()
        val height = bounds.bottom - bounds.top
        save("indexed-paragraph")
        val layouts = mutableListOf<TextLayoutResult>()
        onNodeWithTag("paragraph").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(layouts.size == 1, "Inline formulas must be placeholders in one native text layout")
        assertTrue(height < 85.dp, "Inline indices must share normal text lines; actual height $height")
    }

    private fun ComposeUiTest.save(name: String) {
        val output = File("build/reports/ui-snapshots/$name.png")
        output.parentFile.mkdirs()
        Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap()).use { image ->
            image.encodeToData()!!.use { output.writeBytes(it.bytes) }
        }
    }
}

