package com.dreamsigner.wizard.ui.studio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dreamsigner.wizard.ui.*
import com.dreamsigner.wizard.ui.math.LearningText

/** Local draft stays in the conversation while a failed submission is retried. */
internal data class StudioQuizDraft(
    val selectedIds: List<String> = emptyList(),
    val dontKnow: Boolean = false,
    val thinking: String = "",
    val confidence: Int? = null,
)

@Composable
internal fun StudioQuizCard(
    quiz: StudioQuiz,
    answer: StudioQuizAnswer?,
    draft: StudioQuizDraft,
    onDraftChange: (StudioQuizDraft) -> Unit,
    onSubmit: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val answered = answer != null
    val selected = answer?.selectedIds ?: draft.selectedIds
    val dontKnow = answer?.dontKnow ?: draft.dontKnow
    Column(modifier.fillMaxWidth().testTag("studio-quiz"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Question", color = Graphite, style = MaterialTheme.typography.overline)
        LessonDocument(quiz.question, typesetMath = true)
        Text(if (quiz.multiSelect) "Choisis une ou plusieurs réponses." else "Choisis une réponse.", color = Graphite, fontSize = 14.sp)
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            quiz.options.forEach { option ->
                val isSelected = option.id in selected
                val selectionModifier = if (quiz.multiSelect) {
                    Modifier.toggleable(value = isSelected, enabled = enabled && !answered, role = Role.Checkbox) { checked ->
                        val ids = if (checked) (draft.selectedIds + option.id).distinct() else draft.selectedIds - option.id
                        onDraftChange(draft.copy(selectedIds = ids, dontKnow = false))
                    }
                } else {
                    Modifier.selectable(selected = isSelected, enabled = enabled && !answered, role = Role.RadioButton) {
                        onDraftChange(draft.copy(selectedIds = listOf(option.id), dontKnow = false))
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxWidth().testTag("studio-quiz-option-${option.id}").then(selectionModifier),
                    shape = LearningShape,
                    color = if (isSelected) Paper else White,
                    border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) Ink else Concrete),
                ) {
                    Row(Modifier.heightIn(min = 52.dp).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (quiz.multiSelect) Checkbox(isSelected, onCheckedChange = null, enabled = enabled && !answered)
                        else RadioButton(isSelected, onClick = null, enabled = enabled && !answered)
                        Spacer(Modifier.width(8.dp))
                        LearningText(option.label, Modifier.weight(1f), style = TextStyle(color = Ink, fontSize = 16.sp, lineHeight = 23.sp), recognizeLegacyMath = true)
                        if (answered && option.id in quiz.correctIds) Text("✓ Correcte", color = Ink, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
            if (!answered) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("studio-quiz-dont-know")
                        .selectable(selected = dontKnow, enabled = enabled, role = Role.RadioButton) {
                            onDraftChange(draft.copy(selectedIds = emptyList(), dontKnow = !dontKnow))
                        }.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(dontKnow, onClick = null, enabled = enabled)
                    Spacer(Modifier.width(8.dp))
                    Text("Je ne sais pas", color = Ink)
                }
            }
        }
        if (answered) {
            Text(if (answer!!.correct) "Bonne réponse" else if (answer.dontKnow) "Réponse à découvrir" else "À revoir",
                color = Ink, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("studio-quiz-result"))
            if (answer.dontKnow) Text("Ta réponse : Je ne sais pas", color = Graphite)
            if (answer.thinking.isNotBlank()) {
                Text("Ton raisonnement", color = Graphite, fontWeight = FontWeight.SemiBold)
                LessonDocument(answer.thinking, typesetMath = true)
            }
            answer.confidence?.let { Text("Confiance : $it %", color = Graphite, fontSize = 13.sp) }
            Text("Explication", color = Ink, fontWeight = FontWeight.SemiBold)
            LessonDocument(quiz.explanation, typesetMath = true, modifier = Modifier.testTag("studio-quiz-explanation"))
        } else {
            OutlinedTextField(
                value = draft.thinking,
                onValueChange = { onDraftChange(draft.copy(thinking = it)) },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().testTag("studio-quiz-thinking"),
                label = { Text("Ton raisonnement (facultatif)") },
                placeholder = { Text("Explique comment tu as réfléchi…") },
                minLines = 1, maxLines = 5, shape = LearningShape,
            )
            var showConfidence by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { showConfidence = true }, enabled = enabled, modifier = Modifier.testTag("studio-quiz-confidence")) {
                    Text("Confiance : ${draft.confidence?.let { "$it %" } ?: "non précisée"}")
                }
                DropdownMenu(expanded = showConfidence, onDismissRequest = { showConfidence = false }) {
                    listOf(null, 0, 25, 50, 75, 100).forEach { level ->
                        DropdownMenuItem(onClick = { onDraftChange(draft.copy(confidence = level)); showConfidence = false }) {
                            Text(level?.let { "$it %" } ?: "Non précisée")
                        }
                    }
                }
            }
            Button(
                onClick = onSubmit,
                enabled = enabled && (draft.dontKnow || draft.selectedIds.isNotEmpty()) && draft.thinking.length <= 3_000,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("studio-quiz-submit"),
            ) { Text("Valider ma réponse") }
        }
    }
}
