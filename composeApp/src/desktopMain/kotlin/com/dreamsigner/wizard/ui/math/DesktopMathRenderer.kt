package com.dreamsigner.wizard.ui.math

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
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
    override fun inlineContent(latex: String, style: TextStyle): InlineTextContent? {
        val density = LocalDensity.current
        val pixelSize = with(density) { if (style.fontSize == TextStyle.Default.fontSize) 16.sp.toPx() else style.fontSize.toPx() }
        val bitmap = remember(latex, pixelSize, style.color) { renderMathBitmap(latex, pixelSize, style.color, false) } ?: return null
        return InlineTextContent(
            Placeholder(
                with(density) { bitmap.width.toSp() },
                with(density) { bitmap.height.toSp() },
                PlaceholderVerticalAlign.TextCenter,
            ),
        ) { Image(bitmap, contentDescription = null, modifier = Modifier.fillMaxSize()) }
    }

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
                modifier = Modifier.size(with(density) { bitmap.width.toDp() }, with(density) { bitmap.height.toDp() }),
            )
        }
    }
}

internal fun renderMathBitmap(latex: String, sizePx: Float, color: Color, display: Boolean) = try {
    val formula = TeXFormula(latex)
    val renderedColor = if (color == Color.Unspecified) Color.Black else color
    formula.setColor(java.awt.Color(renderedColor.red, renderedColor.green, renderedColor.blue, renderedColor.alpha))
    val icon = formula.createTeXIcon(
        // Text-style fractions shrink both rows to script size, making units such
        // as m/s unreadable in answer choices. The placeholder takes the full
        // bitmap height, so full-size fractions can safely expand their line.
        if (display || latex.contains("\\frac")) TeXConstants.STYLE_DISPLAY else TeXConstants.STYLE_TEXT,
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
