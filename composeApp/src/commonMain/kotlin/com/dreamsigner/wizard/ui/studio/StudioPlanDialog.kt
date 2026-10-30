package com.dreamsigner.wizard.ui.studio

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

private data class StepDraft(val title: String = "", val prerequisites: String = "", val evidence: String = "", val sources: String = "")

@Composable
internal fun StudioPlanDialog(session: StudioSession, controller: StudioController, onDismiss: () -> Unit, onDiagnosis: () -> Unit) {
    val steps = remember(session.id) { mutableStateListOf<StepDraft>().apply {
        addAll(session.plan?.steps?.map { StepDraft(it.title, it.prerequisites.joinToString(", ") { n -> "${n + 1}" }, it.evidence, it.sources.joinToString(", ") { n -> "${n + 1}" }) } ?: listOf(StepDraft()))
    } }
    var error by remember { mutableStateOf<String?>(null) }
    fun numbers(value: String): List<Int>? {
        if (value.isBlank()) return emptyList()
        return value.split(',').map { it.trim().toIntOrNull()?.minus(1) ?: return null }
    }
    fun save(decision: String): Boolean {
        val parsed = steps.map { draft ->
            StudioPlanStep(draft.title.trim(), numbers(draft.prerequisites) ?: return false,
                draft.evidence.trim(), numbers(draft.sources) ?: return false)
        }
        return controller.savePlan(StudioPlan(parsed, decision))
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.medium, elevation = 8.dp) {
            Column(Modifier.fillMaxWidth().height(440.dp).padding(16.dp)) {
                Text("Mon plan pédagogique", style = MaterialTheme.typography.h6)
                Spacer(Modifier.height(12.dp))
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Construis ou corrige le plan à partir du diagnostic. Le plan accepté guidera les prochaines réponses du tuteur ; aucun acquis n’est déduit automatiquement.")
                Text("Sources : numéros des messages ci-dessous. Prérequis : numéros d’étapes précédentes, séparés par des virgules.")
                session.messages.forEachIndexed { index, message -> Text("${index + 1}. ${if (message.role == "user") "Toi" else "Tuteur"} : ${message.text}") }
                steps.forEachIndexed { index, step ->
                    Text("Étape ${index + 1}", style = MaterialTheme.typography.subtitle1)
                    OutlinedTextField(step.title, { steps[index] = step.copy(title = it) }, label = { Text("Objectif de l’étape") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(step.prerequisites, { steps[index] = step.copy(prerequisites = it) }, label = { Text("Prérequis : étapes précédentes") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(step.evidence, { steps[index] = step.copy(evidence = it) }, label = { Text("Preuve attendue") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(step.sources, { steps[index] = step.copy(sources = it) }, label = { Text("Messages sources du diagnostic") }, modifier = Modifier.fillMaxWidth())
                    if (steps.size > 1 && index == steps.lastIndex) TextButton(onClick = { steps.removeAt(index) }) { Text("Retirer la dernière étape") }
                }
                if (steps.size < 20) TextButton(onClick = { steps.add(StepDraft()) }) { Text("Ajouter une étape") }
                error?.let { Text(it, color = MaterialTheme.colors.error) }
                TextButton(onClick = { if (save("proposed")) onDismiss() else error = "Renseigne chaque objectif, preuve et source valide ; les prérequis doivent précéder l’étape." }) { Text("Enregistrer sans accepter") }
                    TextButton(onClick = {
                        if (steps.size == 1 && steps.first() == StepDraft()) onDiagnosis()
                        else if (save("diagnosis")) onDiagnosis()
                        else error = "Corrige le plan ou ferme cette fenêtre pour poursuivre le diagnostic."
                    }) { Text("Préparer une demande de diagnostic") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Fermer") }
                    TextButton(onClick = { if (save("accepted")) onDismiss() else error = "Renseigne chaque objectif, preuve et source valide ; les prérequis doivent précéder l’étape." }) { Text("Accepter") }
                }
            }
        }
    }
}
