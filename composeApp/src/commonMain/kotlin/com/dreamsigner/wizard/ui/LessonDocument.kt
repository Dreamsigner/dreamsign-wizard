package com.dreamsigner.wizard.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dreamsigner.wizard.ui.math.LearningText

internal enum class LessonBlockKind { PARAGRAPH, HEADING, CODE, BULLET, CALLOUT }
internal data class LessonBlock(val kind: LessonBlockKind, val text: String, val language: String = "")

/** A deliberately small authoring format: no HTML execution, remote images or inferred headings. */
internal fun parseLessonDocument(source: String): List<LessonBlock> {
    val lines = source.replace("\r\n", "\n").lines()
    val blocks = mutableListOf<LessonBlock>()
    val paragraph = mutableListOf<String>()
    fun flush() {
        if (paragraph.isNotEmpty()) {
            blocks += LessonBlock(LessonBlockKind.PARAGRAPH, paragraph.joinToString("\n"))
            paragraph.clear()
        }
    }
    var index = 0
    while (index < lines.size) {
        val line = lines[index]
        when {
            line.startsWith("```") -> {
                flush()
                val start = index
                val code = mutableListOf<String>()
                val language = line.removePrefix("```").trim()
                index++
                while (index < lines.size && !lines[index].startsWith("```")) code += lines[index++]
                if (index == lines.size) {
                    // Preserve a malformed fence literally rather than swallowing the remaining lesson.
                    blocks += LessonBlock(LessonBlockKind.PARAGRAPH, lines.drop(start).joinToString("\n"))
                } else blocks += LessonBlock(LessonBlockKind.CODE, code.joinToString("\n"), language)
            }
            line.isBlank() -> flush()
            line.startsWith("## ") || line.startsWith("### ") -> {
                flush(); blocks += LessonBlock(LessonBlockKind.HEADING, line.substringAfter(' ').trim())
            }
            line.startsWith("- ") -> {
                flush(); blocks += LessonBlock(LessonBlockKind.BULLET, line.removePrefix("- "))
            }
            line.startsWith("> ") -> {
                flush(); blocks += LessonBlock(LessonBlockKind.CALLOUT, line.removePrefix("> "))
            }
            else -> paragraph += line
        }
        index++
    }
    flush()
    return blocks
}

/** Reading measure and explicit document structure are shared by the course, lesson and help views. */
@Composable
internal fun LessonDocument(text: String, typesetMath: Boolean, modifier: Modifier = Modifier) {
    val blocks = remember(text) { parseLessonDocument(text) }
    val body = TextStyle(color = Ink, fontSize = 17.sp, lineHeight = 28.sp, letterSpacing = 0.sp)
    Column(modifier.widthIn(max = 680.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        blocks.forEachIndexed { index, block ->
            when (block.kind) {
                LessonBlockKind.HEADING -> LearningText(block.text,
                    Modifier.padding(top = if (index == 0) 0.dp else 12.dp).semantics { heading() },
                    style = body.copy(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold))
                LessonBlockKind.CODE -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (block.language.isNotBlank()) Text(block.language, color = Graphite, fontSize = 13.sp)
                    Surface(color = Ink, shape = LearningShape) {
                        Text(block.text, Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(16.dp),
                            color = White, fontFamily = FontFamily.Monospace, fontSize = 14.sp, lineHeight = 23.sp,
                            letterSpacing = 0.sp)
                    }
                }
                LessonBlockKind.CALLOUT -> Surface(color = Paper, shape = LearningShape) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("!", color = ActionRed, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                        LearningText(block.text, style = body, recognizeLegacyMath = typesetMath)
                    }
                }
                LessonBlockKind.BULLET -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("•", color = ActionRed, fontSize = 17.sp)
                    LearningText(block.text, Modifier.weight(1f), body, recognizeLegacyMath = typesetMath)
                }
                LessonBlockKind.PARAGRAPH -> LearningText(block.text, style = body, recognizeLegacyMath = typesetMath)
            }
        }
    }
}
