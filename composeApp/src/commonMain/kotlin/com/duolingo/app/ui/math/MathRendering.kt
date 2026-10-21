package com.duolingo.app.ui.math

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Platform renderer hook. The default renderer preserves a useful accessible text alternative. */
fun interface MathRenderer {
    @Composable
    fun Formula(latex: String, sourceText: String, description: String, modifier: Modifier, style: TextStyle, display: Boolean)

    /** Native Text placeholders keep inline equations in the same wrapping and baseline layout. */
    @Composable
    fun inlineContent(latex: String, style: TextStyle): InlineTextContent? = null
}

private val FallbackMathRenderer = MathRenderer { latex, sourceText, description, modifier, style, display ->
    Text(
        text = sourceText,
        modifier = modifier.semantics { contentDescription = description },
        style = style,
    )
}

private val LocalMathRenderer = staticCompositionLocalOf { FallbackMathRenderer }

/** Install a platform renderer around the UI subtree that needs typeset math. */
@Composable
fun MathRenderingProvider(renderer: MathRenderer, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalMathRenderer provides renderer, content = content)
}

@Composable
fun LearningFormula(
    latex: String,
    modifier: Modifier = Modifier,
    description: String = mathAccessibilityDescription(latex),
    style: TextStyle = TextStyle.Default,
    display: Boolean = true,
    alreadyLatex: Boolean = false,
) {
    val normalized = if (alreadyLatex) latex else normalizeMathFormula(latex)
    if (normalized == null) {
        Text(
            text = latex,
            modifier = modifier.semantics { contentDescription = description },
            style = style,
        )
    } else {
        LocalMathRenderer.current.Formula(normalized, latex, description, modifier, style, display)
    }
}

/** Displays `$...$` inline math and `$$...$$` display math using the active platform renderer. */
@Composable
fun LearningText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    recognizeLegacyMath: Boolean = false,
) {
    val segments = parseLearningText(text, recognizeLegacyMath)
    if (segments.size == 1 && segments.single() is LearningTextSegment.Text) {
        Text(text = text, modifier = modifier, style = style)
        return
    }
    val accessibleAlternative = segments.joinToString(separator = "") { segment ->
        when (segment) {
            is LearningTextSegment.Text -> segment.value
            is LearningTextSegment.Math -> mathAccessibilityDescription(segment.latex)
            is LearningTextSegment.Strong -> segment.value
            is LearningTextSegment.Code -> segment.value
        }
    }
    val blocks = mutableListOf<List<LearningTextSegment>>()
    var paragraph = mutableListOf<LearningTextSegment>()
    segments.forEach { segment ->
        if (segment is LearningTextSegment.Math && (segment.display || "\\begin{bmatrix}" in segment.latex)) {
            if (paragraph.isNotEmpty()) blocks += paragraph.toList()
            blocks += listOf(segment.copy(display = true))
            paragraph = mutableListOf()
        } else paragraph += segment
    }
    if (paragraph.isNotEmpty()) blocks += paragraph

    val semanticsModifier = modifier.semantics {
        this.text = AnnotatedString(text)
        contentDescription = accessibleAlternative
    }
    if (blocks.size == 1 && blocks.single().none { it is LearningTextSegment.Math && it.display }) {
        InlineMathParagraph(blocks.single(), semanticsModifier, style)
        return
    }
    Column(modifier.clearAndSetSemantics {
        this.text = AnnotatedString(text)
        contentDescription = accessibleAlternative
    }) {
        blocks.forEach { block ->
            val formula = block.singleOrNull() as? LearningTextSegment.Math
            if (formula?.display == true) {
                Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
                    LearningFormula(formula.latex, style = style, display = true, alreadyLatex = true)
                }
            } else InlineMathParagraph(block, Modifier, style)
        }
    }
}

@Composable
private fun InlineMathParagraph(segments: List<LearningTextSegment>, modifier: Modifier, style: TextStyle) {
    val content = mutableMapOf<String, InlineTextContent>()
    // Compose calls cannot occur inside buildAnnotatedString's non-composable builder.
    segments.forEachIndexed { index, segment ->
        if (segment is LearningTextSegment.Math) {
            LocalMathRenderer.current.inlineContent(segment.latex, style)?.let { content["math-$index"] = it }
        }
    }
    val paragraph = buildAnnotatedString {
        segments.forEachIndexed { index, segment ->
            when (segment) {
                is LearningTextSegment.Text -> append(segment.value)
                is LearningTextSegment.Strong -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(segment.value) }
                is LearningTextSegment.Code -> withStyle(SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    background = Color(0xFFEEEAE2),
                    color = Color(0xFF141414),
                )) { append(segment.value) }
                is LearningTextSegment.Math -> {
                    val id = "math-$index"
                    if (id in content) appendInlineContent(id, segment.latex) else append(segment.latex)
                }
            }
        }
    }
    Text(paragraph, modifier, style = style, inlineContent = content)
}
