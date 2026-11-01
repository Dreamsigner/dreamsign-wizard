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
private fun StudioPlanStep.toDraft() = StepDraft(title, prerequisites.joinToString(", ") { "${it + 1}" }, evidence, sources.joinToString(", ") { "${it + 1}" })

@Composable
internal fun StudioPlanDialog(session: StudioSession, controller: StudioController, onDismiss: () -> Unit, onDiagnosis: () -> Unit) {
    val state by controller.state.collectAsState()
    val proposal = session.planProposal
    val steps = remember(session.id, proposal) { mutableStateListOf<StepDraft>().apply {
        addAll((proposal?.plan ?: session.plan)?.steps?.map { it.toDraft() } ?: listOf(StepDraft()))
    } }
    var error by remember(session.id) { mutableStateOf<String?>(null) }
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
                    TextButton(onClick = {
                        if (!controller.requestDiagnosisConclusion()) error = "Réponds d'abord à au moins deux QCM de diagnostic et termine la question en cours."
                        else error = null
                    }, enabled = !state.busy && state.storageError == null) { Text("Conclure le diagnostic") }
                    session.assessment?.let { assessment ->
                        Text("Diagnostic de la séance", style = MaterialTheme.typography.subtitle1)
                        Text(assessment.summary)
                        assessment.strands.forEach { strand ->
                            Text("${strand.strand} · acquis observé : ${strand.floor} · limite : ${strand.ceiling} · sources : ${strand.sources.joinToString(", ") { (it + 1).toString() }}")
                            strand.misconception?.takeIf { it.isNotBlank() }?.let { Text("Confusion observée : $it") }
                        }
                    }
                    if (session.plan?.decision == "accepted") Text("Ton plan accepté reste actif jusqu’à l’acceptation d’un nouveau plan.")
                    TextButton(onClick = {
                        if (!controller.requestGeneratedPlan()) error = "Impossible de proposer un plan pour le moment. Poursuis le diagnostic et termine la question en cours."
                        else error = null
                    }, enabled = !state.busy && state.storageError == null) { Text("Proposer un plan") }
                    if (state.busy) Text("Préparation de la réponse du tuteur…")
                    state.error?.let { Text(it, color = MaterialTheme.colors.error) }
                    proposal?.let { generated ->
                        Text("Diagnostic proposé à examiner", style = MaterialTheme.typography.subtitle1)
                        generated.diagnosis.forEach { observation ->
                            Text("${observation.observation} — sources : ${observation.sources.joinToString(", ") { (it + 1).toString() }}")
                        }
                        Text("Vérifie et corrige la proposition avant de l’accepter.")
                        TextButton(onClick = { if (!controller.discardPlanProposal()) error = "Impossible de retirer la proposition." }, enabled = !state.busy) { Text("Retirer la proposition") }
                    }
                    Text("Sources : numéros des messages ci-dessous. Prérequis : numéros d’étapes précédentes, séparés par des virgules.")
                    session.messages.forEachIndexed { index, message ->
                        val visible = when (message.text) {
                            STUDIO_QUIZ_REQUEST -> "QCM demandé"
                            STUDIO_PLAN_REQUEST -> "Plan pédagogique demandé"
                            STUDIO_PROBLEM_REQUEST -> "Problème de pratique demandé"
                            STUDIO_DIAGNOSIS_REQUEST -> "Conclusion du diagnostic demandée"
                            STUDIO_GRAPH_REQUEST -> "Extraction de connaissances demandée"
                            else -> if (message.text.startsWith(STUDIO_PRACTICE_ATTEMPT_PREFIX)) "Tentative envoyée"
                                else message.text.ifBlank { message.quiz?.question ?: message.problem?.statement ?: "Question du tuteur" }
                        }
                        Text("${index + 1}. ${if (message.role == "user") "Toi" else "Tuteur"} : $visible")
                    }
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
                    TextButton(onClick = { if (save("proposed")) onDismiss() else error = "Renseigne chaque objectif, preuve et source valide ; les prérequis doivent précéder l’étape." }, enabled = !state.busy) { Text("Enregistrer sans accepter") }
                    TextButton(onClick = {
                        if (steps.size == 1 && steps.first() == StepDraft()) onDiagnosis()
                        else if (save("diagnosis")) onDiagnosis()
                        else error = "Corrige le plan ou ferme cette fenêtre pour poursuivre le diagnostic."
                    }, enabled = !state.busy) { Text("Préparer une demande de diagnostic") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Fermer") }
                    TextButton(onClick = { if (save("accepted")) onDismiss() else error = "Renseigne chaque objectif, preuve et source valide ; les prérequis doivent précéder l’étape." }, enabled = !state.busy) { Text("Accepter") }
                }
            }
        }
    }
}
