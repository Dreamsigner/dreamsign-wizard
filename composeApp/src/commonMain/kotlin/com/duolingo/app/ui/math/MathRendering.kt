package com.duolingo.app.ui.math

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp

/** Platform renderer hook. The default renderer preserves a useful accessible text alternative. */
fun interface MathRenderer {
    @Composable
    fun Formula(latex: String, sourceText: String, description: String, modifier: Modifier, style: TextStyle, display: Boolean)
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
) {
    val normalized = normalizeMathFormula(latex)
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
@OptIn(ExperimentalLayoutApi::class)
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
        }
    }
    Box(modifier.clearAndSetSemantics {
        this.text = AnnotatedString(text)
        contentDescription = accessibleAlternative
    }) {
        FlowRow {
            segments.forEach { segment ->
                when (segment) {
                    is LearningTextSegment.Text -> Text(segment.value, style = style)
                    is LearningTextSegment.Math -> {
                        LearningFormula(
                            latex = segment.latex,
                            modifier = if (segment.display) Modifier.fillMaxWidth().padding(vertical = 4.dp) else Modifier,
                            style = style,
                            display = segment.display,
                        )
                    }
                }
            }
        }
    }
}
