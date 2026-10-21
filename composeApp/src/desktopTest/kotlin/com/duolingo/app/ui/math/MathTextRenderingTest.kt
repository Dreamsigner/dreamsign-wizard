package com.duolingo.app.ui.math

import org.scilab.forge.jlatexmath.TeXConstants
import org.scilab.forge.jlatexmath.TeXFormula
import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MathTextRenderingTest {
    @Test
    fun malformedLatexReturnsNullForVisibleTextFallback() {
        assertNull(renderMathBitmap("\\unknownCommand{broken", 24f, androidx.compose.ui.graphics.Color.Black, display = true))
    }

    @Test
    fun coreGreekMathSymbolsRenderWithoutOptionalLanguageFontModules() {
        val bitmap = renderMathBitmap("\\alpha + \\sum_{i=1}^{n} i", 24f, androidx.compose.ui.graphics.Color.Black, display = true)
        assertTrue(bitmap != null)
    }

    @Test
    fun fractionsRootsAndMatricesProduceVisiblePixels() {
        val icon = TeXFormula("\\frac{x_1^2 + \\sqrt{y}}{2} = \\begin{bmatrix}1 & 0\\\\0 & 1\\end{bmatrix}")
            .createTeXIcon(TeXConstants.STYLE_DISPLAY, 28f)
        val image = BufferedImage(icon.iconWidth, icon.iconHeight, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        try {
            icon.paintIcon(null, graphics, 0, 0)
        } finally {
            graphics.dispose()
        }

        val renderedPixels = (0 until image.height).sumOf { y ->
            (0 until image.width).count { x -> (image.getRGB(x, y) ushr 24) != 0 }
        }
        assertTrue(icon.iconWidth > 0 && icon.iconHeight > 0)
        assertTrue(renderedPixels > 20)
    }
}
