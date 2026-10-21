package com.duolingo.app.ui.math

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import org.jetbrains.skia.Image as SkiaImage
import org.scilab.forge.jlatexmath.TeXConstants
import org.scilab.forge.jlatexmath.TeXFormula
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/** Desktop renderer that rasterizes TeX offscreen, so Compose screenshots include the equation. */
object DesktopMathRenderer : MathRenderer {
    @Composable
    override fun Formula(latex: String, sourceText: String, description: String, modifier: Modifier, style: TextStyle, display: Boolean) {
        val density = LocalDensity.current
        val pixelSize = with(density) {
            if (style.fontSize == TextStyle.Default.fontSize) 22.sp.toPx() else style.fontSize.toPx()
        }
        val foreground = style.color
        val bitmap = remember(latex, description, pixelSize, foreground, display, density.density, density.fontScale) {
            renderMathBitmap(latex, pixelSize, foreground, display)
        }
        if (bitmap == null) {
            androidx.compose.material.Text(
                text = sourceText,
                modifier = modifier.semantics {
                    contentDescription = description.ifBlank { sourceText }
                },
                style = style,
            )
            return
        }
        Box(modifier.semantics {
            contentDescription = description.ifBlank { mathAccessibilityDescription(latex) }
        }, contentAlignment = Alignment.Center) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
            )
        }
    }
}

internal fun renderMathBitmap(latex: String, sizePx: Float, color: Color, display: Boolean) = try {
    val formula = TeXFormula(latex)
    val renderedColor = if (color == Color.Unspecified) Color.Black else color
    formula.setColor(java.awt.Color(renderedColor.red, renderedColor.green, renderedColor.blue, renderedColor.alpha))
    val icon = formula.createTeXIcon(
        if (display) TeXConstants.STYLE_DISPLAY else TeXConstants.STYLE_TEXT,
        sizePx,
    )
    val canvas = BufferedImage(icon.iconWidth.coerceAtLeast(1), icon.iconHeight.coerceAtLeast(1), BufferedImage.TYPE_INT_ARGB)
    val graphics = canvas.createGraphics()
    try {
        icon.paintIcon(null, graphics, 0, 0)
    } finally {
        graphics.dispose()
    }
    ByteArrayOutputStream().use { output ->
        ImageIO.write(canvas, "png", output)
        SkiaImage.makeFromEncoded(output.toByteArray()).toComposeImageBitmap()
    }
} catch (_: Exception) {
    null
}
