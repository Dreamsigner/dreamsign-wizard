package com.dreamsigner.wizard.ui.studio

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
internal fun StudioKnowledgeGraphDialog(state: StudioState, controller: StudioController, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    var deleteProposition by remember { mutableStateOf<KnowledgeProposition?>(null) }
    var deleteReasoning by remember { mutableStateOf<KnowledgeReasoning?>(null) }
    val canEdit = !state.busy && state.storageError == null && state.graphError == null
    fun sources(values: List<KnowledgeSource>): String = values.joinToString(" ; ") { source ->
        if (source.documentName != null) return@joinToString "Document : ${source.documentName} (${source.sessionId.takeLast(8)})"
        val goal = state.sessions.firstOrNull { it.id == source.sessionId }?.goal ?: "Séance ${source.sessionId}"
        "$goal — message ${source.messageIndex + 1}"
    }
    fun reasoning(value: KnowledgeReasoning, graph: KnowledgeGraph): String {
        val names = graph.propositions.associate { it.id to it.statement }
        return value.premises.joinToString(" + ") { names[it] ?: it } + " → " + (names[value.conclusion] ?: value.conclusion)
    }
    fun result(success: Boolean) { localError = if (success) null else "Action impossible pour le moment. Consulte le message d’erreur ou réessaie." }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.medium, elevation = 8.dp) {
            Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).padding(16.dp)) {
                Text("Graphe de connaissances", style = MaterialTheme.typography.h6)
                Column(Modifier.weight(1f, fill = false).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Les propositions et raisonnements sont à vérifier. Ils ne représentent pas une évaluation de tes acquis.")
                    TextButton(onClick = { result(controller.requestGraphExtraction()) }, enabled = canEdit && state.graphProposal == null && state.selectedId != null) {
                        Text("Extraire les connaissances de cette séance")
                    }
                    Text("Importer des notes (TXT, Markdown ou PDF texte) envoie leur contenu au fournisseur IA configuré. Les propositions doivent être vérifiées avant acceptation.")
                    TextButton(onClick = { result(controller.importNotes()) }, enabled = canEdit && state.graphProposal == null && state.selectedId != null) { Text("Choisir des notes à importer") }
                    if (state.busy) Text("Préparation en cours…")
                    state.graphError?.let { Text(it, color = MaterialTheme.colors.error) }
                    if (state.graphError != null) TextButton(onClick = { result(controller.refreshKnowledgeGraph()) }) { Text("Recharger le graphe") }
                    state.error?.let { Text(it, color = MaterialTheme.colors.error) }
                    state.storageError?.let { Text(it, color = MaterialTheme.colors.error) }
                    localError?.let { Text(it, color = MaterialTheme.colors.error) }
                    state.graphProposal?.let { proposal ->
                        Text("Proposition à examiner", style = MaterialTheme.typography.subtitle1)
                        Text("Vérifie les énoncés, les liens et leurs sources avant d’accepter.")
                        val oldPropositions = proposal.base.propositions.associateBy { it.id }
                        val oldReasonings = proposal.base.reasonings.associateBy { it.id }
                        val propositions = proposal.graph.propositions.filter { oldPropositions[it.id] != it }
                        val reasonings = proposal.graph.reasonings.filter { oldReasonings[it.id] != it }
                        if (propositions.isEmpty() && reasonings.isEmpty()) Text("Aucun ajout ou changement proposé.")
                        propositions.forEach { item ->
                            Text(if (item.id in oldPropositions) "Proposition — sources mises à jour" else "Nouvelle proposition", style = MaterialTheme.typography.subtitle2)
                            Text(item.statement)
                            Text("Sources : ${sources(item.sources)}", style = MaterialTheme.typography.caption)
                        }
                        reasonings.forEach { item ->
                            Text(if (item.id in oldReasonings) "Raisonnement — sources mises à jour" else "Nouveau raisonnement", style = MaterialTheme.typography.subtitle2)
                            Text(reasoning(item, proposal.graph))
                            Text("Sources : ${sources(item.sources)}", style = MaterialTheme.typography.caption)
                        }
                        TextButton(onClick = { result(controller.acceptGraphProposal()) }, enabled = canEdit) { Text("Accepter la proposition") }
                        TextButton(onClick = { result(controller.discardGraphProposal()) }, enabled = !state.busy && state.storageError == null) { Text("Rejeter la proposition") }
                    }
                    Divider()
                    OutlinedTextField(
                        value = query, onValueChange = { query = it },
                        label = { Text("Rechercher dans le graphe") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("studio-graph-search"),
                        trailingIcon = if (query.isNotEmpty()) ({ TextButton(onClick = { query = "" }) { Text("Effacer") } }) else null,
                    )
                    val search = query.trim()
                    val propositions = state.knowledgeGraph.propositions.filter { it.statement.contains(search, ignoreCase = true) }
                    val reasonings = state.knowledgeGraph.reasonings.filter { reasoning(it, state.knowledgeGraph).contains(search, ignoreCase = true) }
                    if (search.isNotEmpty() && propositions.isEmpty() && reasonings.isEmpty()) {
                        Text("Aucun résultat. Essaie un autre terme ou efface la recherche.")
                    }
                    Text("Propositions enregistrées", style = MaterialTheme.typography.subtitle1)
                    if (state.knowledgeGraph.propositions.isEmpty()) Text("Aucune proposition enregistrée.")
                    propositions.forEach { item ->
                        key(item.id) {
                            var editing by remember { mutableStateOf(false) }
                            var draft by remember(item.statement) { mutableStateOf(item.statement) }
                            if (editing) {
                                OutlinedTextField(draft, { draft = it }, label = { Text("Énoncé de la proposition") }, modifier = Modifier.fillMaxWidth())
                                TextButton(onClick = {
                                    val saved = controller.updateGraphProposition(item.id, draft.trim())
                                    result(saved)
                                    if (saved) editing = false
                                }, enabled = canEdit && draft.isNotBlank()) { Text("Enregistrer la modification") }
                                TextButton(onClick = { draft = item.statement; editing = false }) { Text("Annuler la modification") }
                            } else {
                                Text(item.statement)
                                TextButton(onClick = { editing = true }, enabled = canEdit) { Text("Modifier la proposition") }
                            }
                            Text("Sources : ${sources(item.sources)}", style = MaterialTheme.typography.caption)
                            TextButton(onClick = { deleteProposition = item }, enabled = canEdit) { Text("Supprimer la proposition") }
                            Divider()
                        }
                    }
                    Text("Raisonnements enregistrés", style = MaterialTheme.typography.subtitle1)
                    if (state.knowledgeGraph.reasonings.isEmpty()) Text("Aucun raisonnement enregistré.")
                    reasonings.forEach { item ->
                        Text(reasoning(item, state.knowledgeGraph))
                        Text("Sources : ${sources(item.sources)}", style = MaterialTheme.typography.caption)
                        TextButton(onClick = { deleteReasoning = item }, enabled = canEdit) { Text("Supprimer le raisonnement") }
                        Divider()
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Fermer") }
                }
            }
        }
    }
    deleteProposition?.let { item ->
        AlertDialog(onDismissRequest = { deleteProposition = null }, title = { Text("Supprimer cette proposition ?") },
            text = { Text("Les raisonnements qui la mentionnent seront également supprimés.\n\n${item.statement}") },
            confirmButton = { TextButton(onClick = { result(controller.deleteGraphProposition(item.id)); deleteProposition = null }) { Text("Supprimer") } },
            dismissButton = { TextButton(onClick = { deleteProposition = null }) { Text("Annuler") } })
    }
    deleteReasoning?.let { item ->
        AlertDialog(onDismissRequest = { deleteReasoning = null }, title = { Text("Supprimer ce raisonnement ?") },
            text = { Text(reasoning(item, state.knowledgeGraph)) },
            confirmButton = { TextButton(onClick = { result(controller.deleteGraphReasoning(item.id)); deleteReasoning = null }) { Text("Supprimer") } },
            dismissButton = { TextButton(onClick = { deleteReasoning = null }) { Text("Annuler") } })
    }
}
