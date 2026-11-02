package com.dreamsigner.wizard.ui.studio

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dreamsigner.wizard.ui.*
import com.dreamsigner.wizard.ui.math.LearningText

internal data class StudioProblemDraft(val answer: String = "", val thinking: String = "")

@Composable
internal fun StudioProblemCard(
    problem: StudioProblem,
    draft: StudioProblemDraft,
    onDraftChange: (StudioProblemDraft) -> Unit,
    onHint: () -> Unit,
    onSubmit: () -> Unit,
    onSurrender: () -> Unit,
    onNext: () -> Unit,
    enabled: Boolean,
) {
    Column(Modifier.fillMaxWidth().testTag("studio-problem"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Problème de pratique", color = Ink, style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold)
        LessonDocument(problem.statement, typesetMath = true)
        if (problem.shownHints > 0) {
            Surface(color = Paper, shape = LearningShape, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Indices", color = Graphite, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    problem.hints.take(problem.shownHints).forEachIndexed { index, hint ->
                        LearningText("${index + 1}. $hint", Modifier.testTag("studio-problem-hint-$index"), recognizeLegacyMath = true)
                    }
                }
            }
        }
        val attempt = problem.attempt
        val result = problem.result
        if (result == null && attempt == null) {
            Divider(color = Concrete)
            OutlinedTextField(draft.answer, { onDraftChange(draft.copy(answer = it)) }, enabled = enabled,
                modifier = Modifier.fillMaxWidth().testTag("studio-problem-answer"), label = { Text("Ta réponse") }, minLines = 1, maxLines = 5, shape = LearningShape)
            OutlinedTextField(draft.thinking, { onDraftChange(draft.copy(thinking = it)) }, enabled = enabled,
                modifier = Modifier.fillMaxWidth().testTag("studio-problem-thinking"), label = { Text("Ton raisonnement (facultatif)") }, minLines = 1, maxLines = 5, shape = LearningShape)
            if (problem.shownHints < problem.hints.size) TextButton(onClick = onHint, enabled = enabled, modifier = Modifier.heightIn(min = 48.dp).testTag("studio-problem-hint")) {
                Text(if (problem.shownHints == 0) "Voir un indice" else "Indice suivant")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onSurrender, enabled = enabled, modifier = Modifier.testTag("studio-problem-surrender")) { Text("Passer") }
                Button(onClick = onSubmit, enabled = enabled && draft.answer.isNotBlank() && draft.answer.length <= 4_000 && draft.thinking.length <= 3_000,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("studio-problem-submit")) { Text("Valider ma tentative") }
            }
        } else {
            Divider(color = Concrete)
            attempt?.let {
                Text("Ta réponse", color = Graphite, fontWeight = FontWeight.SemiBold)
                LessonDocument(it.answer, typesetMath = true)
                if (it.thinking.isNotBlank()) {
                    Text("Ton raisonnement", color = Graphite, fontWeight = FontWeight.SemiBold)
                    LessonDocument(it.thinking, typesetMath = true)
                }
            }
            if (result == null) Text("Évaluation en cours…", modifier = Modifier.testTag("studio-problem-pending"))
            else {
                val outcome = when (result.outcome) {
                    "solved" -> "Résolu"
                    "solvedWithHints" -> "Résolu avec indices"
                    "surrendered" -> "Problème arrêté"
                    else -> "À reprendre"
                }
                Divider(color = Concrete)
                Text(outcome, color = Ink, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("studio-problem-result"))
                LessonDocument(result.verdict, typesetMath = true)
                Text("Solution", color = Ink, style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold)
                LessonDocument(problem.modelSolution, typesetMath = true, modifier = Modifier.testTag("studio-problem-solution"))
                if (result.retest) Text("Une nouvelle tentative sera utile.")
                Button(onClick = onNext, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("studio-problem-next")) { Text("Problème suivant") }
            }
        }
    }
}
