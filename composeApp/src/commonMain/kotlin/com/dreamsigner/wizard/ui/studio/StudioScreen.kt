package com.dreamsigner.wizard.ui.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dreamsigner.wizard.ui.*

@Composable
fun StudioScreen() {
    val controller = LocalStudioController.current
    val state by controller.state.collectAsState()
    var paneOpen by remember { mutableStateOf(true) }
    var showPlan by remember { mutableStateOf(false) }
    var diagnosisDraft by remember { mutableStateOf(false) }
    var showSessions by remember { mutableStateOf(false) }
    var showGoal by remember { mutableStateOf(false) }
    var newKind by remember { mutableStateOf(StudioSessionKind.TEACHING) }
    var showGraph by remember { mutableStateOf(false) }

    if (showGraph) StudioKnowledgeGraphDialog(state, controller, onDismiss = { showGraph = false })
    if (showSessions) SessionPickerDialog(
        state = state,
        onSelect = { controller.select(it); showSessions = false },
        onNewGoal = { kind -> newKind = kind; showSessions = false; showGoal = true },
        onPlan = { showSessions = false; showPlan = true },
        onDismiss = { showSessions = false },
    )
    if (showGoal) NewGoalDialog(
        enabled = state.available && !state.busy,
        kind = newKind,
        onCreate = { goal, kind -> if (controller.create(goal, kind)) { showGoal = false; true } else false },
        onDismiss = { showGoal = false },
    )

    val selectedSession = state.sessions.firstOrNull { it.id == state.selectedId }
    if (showPlan && selectedSession != null) StudioPlanDialog(selectedSession, controller, onDismiss = { showPlan = false }, onDiagnosis = { diagnosisDraft = true; showPlan = false })

    BoxWithConstraints(Modifier.fillMaxSize().background(Paper)) {
        val wide = maxWidth >= 800.dp
        val session = state.sessions.firstOrNull { it.id == state.selectedId }
        Column(Modifier.fillMaxSize().padding(horizontal = if (wide) 28.dp else 16.dp, vertical = if (wide) 22.dp else 14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Studio IA", style = if (wide) MaterialTheme.typography.h4 else MaterialTheme.typography.h5, color = Ink)
                    if (wide) Text("Un tuteur qui part de ce que tu sais déjà.", color = Graphite, fontSize = 14.sp)
                }
                TextButton(onClick = { showGraph = true }, enabled = state.available, modifier = Modifier.testTag("studio-graph")) { Text("Graphe") }
            }
            if (state.storageError != null) StatusNote(state.storageError!!, warning = true, Modifier.padding(top = 10.dp))
            if (!state.available && !state.error.isNullOrBlank()) {
                StatusNote(state.error!!, warning = true, Modifier.padding(top = 10.dp), tag = "studio-unavailable")
            }

            if (wide) {
                Row(Modifier.weight(1f).fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    if (paneOpen) SessionSidebar(
                        state = state,
                        modifier = Modifier.width(270.dp).fillMaxHeight(),
                        onSelect = controller::select,
                        onCreate = { kind -> newKind = kind; showGoal = true },
                        onPlan = { showPlan = true },
                    )
                    StudioWorkspace(
                        state = state, session = session, controller = controller,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        onSessions = { paneOpen = !paneOpen }, diagnosisDraft = diagnosisDraft, onDiagnosisConsumed = { diagnosisDraft = false },
                    )
                }
            } else {
                StudioWorkspace(
                    state = state, session = session, controller = controller,
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 14.dp),
                    compactGoal = state.sessions.isEmpty(), onSessions = { showSessions = true }, diagnosisDraft = diagnosisDraft, onDiagnosisConsumed = { diagnosisDraft = false },
                )
            }
        }
    }
}

/** Settings entry point; the Atelier and Paramètres share exactly the same provider dialog. */
@Composable
fun StudioConnectionSettings() {
    val controller = LocalStudioController.current
    val state by controller.state.collectAsState()
    var showConnection by remember { mutableStateOf(false) }
    if (showConnection) StudioConnectionDialog(controller, state, onDismiss = { showConnection = false })

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Connexion IA", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold, color = Ink)
        Text("Connecte ton abonnement ChatGPT ou configure un fournisseur avec une clé API.", color = Graphite, fontSize = 14.sp)
        if (state.providerMode == "chatgpt" && state.chatGptConnected) {
            Text("ChatGPT connecté : ${state.chatGptEmail.orEmpty()} · ${state.model}", color = Graphite, fontSize = 13.sp)
        }
        if (state.providerMode == "chatgpt" && state.chatGptConnected) {
            Text("Les fournisseurs à clé API restent disponibles dans Connexion IA.", color = Graphite, fontSize = 13.sp)
        } else if (state.baseUrl.isNotBlank() && state.model.isNotBlank()) {
            Text("${state.baseUrl} · ${state.model}", color = Graphite, fontSize = 13.sp)
            if (!state.hasApiKey && state.baseUrl.startsWith("https://")) {
                Text("La clé doit être ressaisie après chaque démarrage.", color = ActionRed, fontSize = 13.sp)
            }
        } else {
            Text("Aucun fournisseur configuré.", color = Graphite, fontSize = 13.sp)
        }
        OutlinedButton(
            onClick = { showConnection = true }, enabled = state.available && !state.busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = LearningShape,
        ) { Text("Configurer le fournisseur") }
        val session = state.sessions.firstOrNull { it.id == state.selectedId }
        if (session != null) {
            Text("Réglages de la séance : ${session.goal}", fontWeight = FontWeight.SemiBold, color = Ink)
            var showModels by remember(session.id) { mutableStateOf(false) }
            var showEfforts by remember(session.id) { mutableStateOf(false) }
            val models = if (state.providerMode == "chatgpt") state.chatGptModels else if (state.apiModelsUrl == state.baseUrl) state.apiModels else emptyList()
            Box {
                OutlinedButton(onClick = { showModels = true }, enabled = !state.busy && state.storageError == null, modifier = Modifier.testTag("studio-session-model")) {
                    Text("Modèle : ${session.modelChoice?.model ?: state.model.ifBlank { "non configuré" }}")
                }
                DropdownMenu(expanded = showModels, onDismissRequest = { showModels = false }) {
                    DropdownMenuItem(onClick = { controller.setSessionModel(null); showModels = false }) { Text("Suivre le modèle par défaut") }
                    (models + listOfNotNull(state.model.takeIf { it.isNotBlank() })).distinct().forEach { candidate ->
                        DropdownMenuItem(onClick = { controller.setSessionModel(candidate); showModels = false }) { Text(candidate) }
                    }
                }
            }
            Box {
                OutlinedButton(onClick = { showEfforts = true }, enabled = !state.busy && state.storageError == null, modifier = Modifier.testTag("studio-session-effort")) {
                    Text("Réflexion : ${session.reasoningEffort?.label ?: "Défaut du tuteur (${state.reasoningEffort.label})"}")
                }
                DropdownMenu(expanded = showEfforts, onDismissRequest = { showEfforts = false }) {
                    DropdownMenuItem(onClick = { controller.setSessionReasoningEffort(null); showEfforts = false }) { Text("Défaut du tuteur (${state.reasoningEffort.label})") }
                    ReasoningEffort.entries.forEach { effort -> DropdownMenuItem(onClick = { controller.setSessionReasoningEffort(effort); showEfforts = false }) { Text(effort.label) } }
                }
            }
        }
        if (state.providerMode == "chatgpt" && state.chatGptConnected) {
            Text("Utilise ton forfait ChatGPT", color = Graphite, fontSize = 13.sp)
            TextButton(onClick = controller::openChatGptUsage) { Text("Gérer l’usage") }
        }

    }
}

@Composable
private fun SessionSidebar(
    state: StudioState,
    modifier: Modifier,
    onSelect: (String) -> Unit,
    onCreate: (StudioSessionKind) -> Unit,
    onPlan: () -> Unit,
) {
    Surface(modifier, shape = LearningShape, color = White, elevation = 1.dp) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Nouvelle séance", style = MaterialTheme.typography.h6, color = Ink)
            Row {
                TextButton(onClick = { onCreate(StudioSessionKind.TEACHING) }, enabled = state.available && !state.busy) { Text("+ Apprendre") }
                TextButton(onClick = { onCreate(StudioSessionKind.PRACTICE) }, enabled = state.available && !state.busy) { Text("+ Pratique") }
            }
            if (state.selectedId != null) TextButton(onClick = onPlan, enabled = !state.busy && state.storageError == null, modifier = Modifier.testTag("studio-plan")) { Text("Plan de la séance") }
            Divider(color = Concrete)
            Text("Séances enregistrées", style = MaterialTheme.typography.subtitle1, color = Ink)
            var filter by remember { mutableStateOf<StudioSessionKind?>(null) }
            Row {
                listOf(null to "Toutes", StudioSessionKind.TEACHING to "Cours", StudioSessionKind.PRACTICE to "Pratique").forEach { (kind, label) ->
                    TextButton(onClick = { filter = kind }, contentPadding = PaddingValues(horizontal = 4.dp)) { Text(label, color = if (filter == kind) ActionRed else Graphite, fontSize = 12.sp) }
                }
            }
            val visibleSessions = state.sessions.filter { filter == null || it.kind == filter }
            if (visibleSessions.isEmpty()) {
                Text("Tes échanges réapparaîtront ici.", color = Graphite, fontSize = 14.sp)
            } else {
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(visibleSessions, key = { it.id }) { item ->
                        val selected = item.id == state.selectedId
                        TextButton(
                            onClick = { onSelect(item.id) }, enabled = !state.busy,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .background(if (selected) ActionRed.copy(alpha = 0.08f) else White, LearningShape)
                                .then(if (selected) Modifier.border(1.dp, ActionRed, LearningShape) else Modifier),
                            shape = LearningShape,
                        ) {
                            Text(item.goal, modifier = Modifier.fillMaxWidth(), maxLines = 2, overflow = TextOverflow.Ellipsis,
                                color = if (selected) ActionRed else Ink, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalForm(enabled: Boolean, onCreate: (String, StudioSessionKind) -> Boolean) {
    var kind by remember { mutableStateOf(StudioSessionKind.TEACHING) }
    var goal by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row {
            TextButton(onClick = { kind = StudioSessionKind.TEACHING }) { Text("Apprendre", color = if (kind == StudioSessionKind.TEACHING) ActionRed else Graphite) }
            TextButton(onClick = { kind = StudioSessionKind.PRACTICE }) { Text("Pratique", color = if (kind == StudioSessionKind.PRACTICE) ActionRed else Graphite) }
        }
        OutlinedTextField(
            value = goal, onValueChange = { goal = it }, enabled = enabled,
            modifier = Modifier.fillMaxWidth(), label = { Text("Qu’aimerais-tu apprendre ?") },
            placeholder = { Text("Ex. comprendre les valeurs propres") },
            minLines = 2, maxLines = 4, shape = LearningShape,
        )
        Button(
            onClick = { if (onCreate(goal.trim(), kind)) goal = "" }, enabled = enabled && goal.isNotBlank(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = LearningShape,
            colors = ButtonDefaults.buttonColors(backgroundColor = ActionRed, contentColor = White),
        ) { Text("Commencer") }
        Text("Python · préparer le TCF Canada · revoir les fractions", color = Graphite, fontSize = 12.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun StudioWorkspace(
    state: StudioState,
    session: StudioSession?,
    controller: StudioController,
    modifier: Modifier,
    compactGoal: Boolean = false,
    onSessions: () -> Unit, diagnosisDraft: Boolean, onDiagnosisConsumed: () -> Unit,
) {
    Surface(modifier.testTag("studio-workspace"), shape = LearningShape, color = White, elevation = 1.dp) {
        if (session == null) {
            if (compactGoal) {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text("Commence par une question précise", style = MaterialTheme.typography.h5, color = Ink)
                    Text("L’IA répondra à partir de cet objectif et de cette séance.", color = Graphite)
                    GoalForm(enabled = state.available && !state.busy, onCreate = { goal, kind -> controller.create(goal, kind) })
                    if (state.available) Text("Tes autres cours ne sont pas envoyés au fournisseur.", color = Graphite, fontSize = 13.sp)
                }
            } else {
                Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center) {
                    TextButton(onClick = onSessions, modifier = Modifier.testTag("studio-sessions-toggle")) { Text("Séances") }
                    Text("Commence par un objectif précis", style = MaterialTheme.typography.h5, color = Ink)
                    Text("Par exemple : comprendre les valeurs propres, apprendre les bases de Python ou préparer une tâche du TCF Canada.",
                        Modifier.padding(top = 10.dp), color = Graphite, fontSize = 16.sp, lineHeight = 24.sp)
                    if (state.available) Text("L’objectif, les messages et le plan accepté de cette séance seront envoyés au fournisseur configuré.",
                        Modifier.padding(top = 18.dp), color = Graphite, fontSize = 14.sp)
                }
            }
        } else {
            Conversation(state, session, controller, onSessions, diagnosisDraft, onDiagnosisConsumed)
        }
    }
}

@Composable
private fun Conversation(state: StudioState, session: StudioSession, controller: StudioController, onSessions: () -> Unit, diagnosisDraft: Boolean, onDiagnosisConsumed: () -> Unit) {
    var draft by remember(session.id) { mutableStateOf("") }
    val quizDrafts = remember(session.id) { mutableStateMapOf<Int, StudioQuizDraft>() }
    val problemDrafts = remember(session.id) { mutableStateMapOf<Int, StudioProblemDraft>() }
    var showClarification by remember(session.id) { mutableStateOf(false) }
    val activeQuizIndex = session.messages.indexOfLast { it.quiz != null && it.quizAnswer == null }
    val hasOpenQuiz = activeQuizIndex >= 0
    val activeProblemIndex = session.messages.indexOfLast { it.problem != null && it.problem.result == null }
    val hasOpenProblem = activeProblemIndex >= 0
    val hasOpenExercise = hasOpenQuiz || hasOpenProblem
    LaunchedEffect(session.id, activeQuizIndex) { showClarification = false }
    LaunchedEffect(diagnosisDraft) { if (diagnosisDraft) { draft = "Peux-tu approfondir le diagnostic de mes prérequis avant de poursuivre le plan ?"; onDiagnosisConsumed() } }
    val messagesState = rememberLazyListState()
    LaunchedEffect(session.id, session.messages.size) {
        if (session.messages.isNotEmpty()) messagesState.animateScrollToItem(session.messages.lastIndex)
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onSessions, modifier = Modifier.testTag("studio-sessions-toggle")) { Text("Séances") }
            Text(session.goal, Modifier.weight(1f).testTag("studio-current-goal"), style = MaterialTheme.typography.h6, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Divider(Modifier.padding(top = 4.dp), color = Concrete)
        if (session.messages.isEmpty()) {
            Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("À toi de jouer", style = MaterialTheme.typography.h6, color = Ink)
                Text("Décris ce que tu sais déjà ou pose ta première question.", Modifier.padding(top = 6.dp), color = Graphite)
            }
        } else {
            LazyColumn(
                state = messagesState, modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 4.dp).testTag("studio-conversation-messages"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(session.messages.size, key = { index -> "${session.id}-$index" }) { index ->
                    val message = session.messages[index]
                    val learner = message.role != "assistant"
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (learner) Arrangement.End else Arrangement.Start) {
                        Surface(
                            modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(if (message.quiz != null) 1f else 0.88f),
                            color = if (message.quiz != null) White else if (learner) ColorStudioLearner else Paper,
                            border = if (message.quiz != null) androidx.compose.foundation.BorderStroke(1.dp, Concrete) else null,
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                val displayText = when (message.text) {
                                    STUDIO_QUIZ_REQUEST -> "QCM demandé"
                                    STUDIO_PREDICT_REQUEST -> "Prédiction demandée"
                                    STUDIO_CHECK_REQUEST -> "Vérification de compréhension demandée"
                                    STUDIO_PLAN_REQUEST -> "Plan pédagogique demandé"
                                    STUDIO_PROBLEM_REQUEST -> "Problème de pratique demandé"
                                    STUDIO_DIAGNOSIS_REQUEST -> "Conclusion du diagnostic demandée"
                                    STUDIO_GRAPH_REQUEST -> "Extraction de connaissances demandée"
                                    else -> message.text
                                }.let { if (it.startsWith(STUDIO_PRACTICE_ATTEMPT_PREFIX)) "Tentative envoyée" else it }
                                if (displayText.isNotBlank()) LessonDocument(displayText, typesetMath = true)
                                message.quiz?.let { quiz ->
                                    val quizDraft = quizDrafts[index] ?: StudioQuizDraft()
                                    StudioQuizCard(
                                        quiz = quiz, answer = message.quizAnswer, draft = quizDraft,
                                        onDraftChange = { quizDrafts[index] = it },
                                        onSubmit = {
                                            controller.submitQuiz(index, quizDraft.selectedIds, quizDraft.dontKnow, quizDraft.thinking, quizDraft.confidence)
                                        },
                                        enabled = state.available && !state.busy && state.storageError == null,
                                    )
                                }
                                message.problem?.let { problem ->
                                    val problemDraft = problemDrafts[index] ?: StudioProblemDraft()
                                    StudioProblemCard(problem, problemDraft, onDraftChange = { problemDrafts[index] = it },
                                        onHint = { controller.revealPracticeHint(index) },
                                        onSubmit = { controller.submitPracticeAttempt(index, problemDraft.answer, problemDraft.thinking) },
                                        onSurrender = { controller.surrenderPracticeProblem(index) },
                                        onNext = { controller.requestPracticeProblem() },
                                        enabled = state.available && !state.busy && state.storageError == null)
                                }
                            }
                        }
                    }
                }
            }
        }
        if (!state.error.isNullOrBlank()) {
            Column(Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }.padding(bottom = 8.dp)) {
                Text(state.error!!, color = ActionRed, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = controller::retry, enabled = !state.busy) { Text("Réessayer") }
                }
            }
        }
        if (hasOpenQuiz) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { showClarification = !showClarification }, enabled = !state.busy,
                    modifier = Modifier.testTag("studio-quiz-clarification")) {
                    Text(if (showClarification) "Retour au QCM" else "Clarifier")
                }

            }
        }
        if (!hasOpenExercise || showClarification) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = draft, onValueChange = { draft = it }, enabled = !state.busy && state.available,
                modifier = Modifier.weight(1f).testTag("studio-draft"), label = { Text("Ton raisonnement ou ta question") },
                placeholder = { Text("Écris à ton tuteur…") }, minLines = 1, maxLines = 4,
                shape = LearningShape,
            )
            Button(
                onClick = { if (controller.send(draft)) draft = "" },
                enabled = draft.isNotBlank() && !state.busy && state.available,
                modifier = Modifier.heightIn(min = 48.dp).testTag("studio-send"), shape = LearningShape,
                colors = ButtonDefaults.buttonColors(backgroundColor = ActionRed, contentColor = White),
            ) { Text(if (state.busy) "Réponse…" else "Envoyer") }
            if (state.busy) TextButton(onClick = controller::cancel, modifier = Modifier.heightIn(min = 48.dp)) { Text("Annuler") }
        }
        else if (state.busy) TextButton(onClick = controller::cancel, modifier = Modifier.heightIn(min = 48.dp)) { Text("Annuler") }
    }
}

@Composable
private fun StudioConnectionDialog(controller: StudioController, state: StudioState, onDismiss: () -> Unit) {
    var baseUrl by remember(state.baseUrl) { mutableStateOf(state.baseUrl) }
    var model by remember(state.apiModel) { mutableStateOf(state.apiModel) }
    var apiKey by remember { mutableStateOf("") }
    var showModels by remember { mutableStateOf(false) }
    var showEfforts by remember { mutableStateOf(false) }
    var modelSearch by remember { mutableStateOf("") }
    var showApiModels by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Connexion IA") },
        text = {
            Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Niveau de réflexion du tuteur", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold)
                Box {
                    OutlinedButton(onClick = { showEfforts = true }, enabled = state.available && !state.busy) {
                        Text("Niveau de réflexion du tuteur : ${state.reasoningEffort.label}")
                    }
                    DropdownMenu(expanded = showEfforts, onDismissRequest = { showEfforts = false }) {
                        ReasoningEffort.entries.forEach { effort ->
                            DropdownMenuItem(onClick = { controller.setReasoningEffort(effort); showEfforts = false }) { Text(effort.label) }
                        }
                    }
                }
                Text("Automatique laisse le fournisseur décider. Les autres niveaux dépendent du modèle et du fournisseur.", color = Graphite, fontSize = 13.sp)
                Divider(color = Concrete)
                Text("Abonnement ChatGPT", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold)
                if (state.chatGptConnected) {
                    Text("Connecté : ${state.chatGptEmail.orEmpty()}", color = Graphite)
                    Box {
                        OutlinedButton(onClick = { showModels = true }, enabled = state.chatGptModels.isNotEmpty() && !state.busy) {
                            Text("Modèle : ${if (state.providerMode == "chatgpt") state.model.ifBlank { "Choisir" } else "Choisir"}")
                        }
                        DropdownMenu(expanded = showModels, onDismissRequest = { showModels = false }) {
                            state.chatGptModels.forEach { availableModel ->
                                DropdownMenuItem(onClick = { controller.selectChatGptModel(availableModel); showModels = false }) { Text(availableModel) }
                            }
                        }
                    }
                    TextButton(onClick = controller::disconnectChatGpt, enabled = !state.busy) { Text("Déconnecter ce compte") }
                } else {
                    Button(onClick = controller::connectChatGpt, enabled = !state.chatGptBusy && !state.busy) {
                        Text(if (state.chatGptBusy) "Connexion en cours…" else "Continuer avec ChatGPT")
                    }
                }
                Text("Utilise les modèles disponibles dans ton forfait après ton accord dans le navigateur. Ton forfait et l’API à clé ont des accès distincts.", color = Graphite)
                Text("La connexion et ses jetons sont enregistrés dans un fichier local protégé. Déconnecter ce compte efface les jetons locaux.", color = Graphite, fontSize = 13.sp)
                if (!state.chatGptError.isNullOrBlank()) Text(state.chatGptError!!, color = ActionRed, fontSize = 13.sp)
                Divider(color = Concrete)
                Text("Modèles par tâche", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold)
                Text("Fournisseur actif : ${if (state.providerMode == "chatgpt") "ChatGPT" else "API"}. Le choix propre à une séance reste prioritaire. Laisse vide pour hériter.", color = Graphite, fontSize = 13.sp)
                Text("Disponibilité, prix et niveaux de réflexion pris en charge sont inconnus : vérifie-les auprès du fournisseur.", color = Graphite, fontSize = 13.sp)
                StudioModelSlot.entries.forEach { slot ->
                    val label = if (slot == StudioModelSlot.STRONG) "Principal : diagnostic, plan et pratique" else "Secondaire : graphe"
                    StudioRoutingEditor(label, state.modelSlots[slot], state) { model, effort -> controller.setModelSlot(slot, model, effort) }
                }
                StudioModelRole.entries.forEach { role ->
                    val label = when (role) {
                        StudioModelRole.DIAGNOSTIC -> "Diagnostic"
                        StudioModelRole.TEACHING_PLAN -> "Plan pédagogique"
                        StudioModelRole.PRACTICE -> "Pratique"
                        StudioModelRole.GRAPH_EXTRACTION -> "Extraction du graphe"
                    }
                    StudioRoutingEditor("Exception : $label", state.modelOverrides[role], state) { model, effort -> controller.setModelOverride(role, model, effort) }
                }
                Divider(color = Concrete)
                Text("Autre fournisseur avec clé API", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold)
                Text("L’objectif, les messages et le plan accepté de la séance active sont transmis au fournisseur choisi.", color = Graphite)
                OutlinedTextField(
                    value = baseUrl, onValueChange = { baseUrl = it; controller.cancelApiModelDiscovery() }, enabled = state.available && !state.busy,
                    modifier = Modifier.fillMaxWidth(), label = { Text("URL de base") },
                    placeholder = { Text("https://api.openai.com/v1") }, singleLine = true, shape = LearningShape,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
                OutlinedTextField(
                    value = apiKey, onValueChange = { apiKey = it; controller.cancelApiModelDiscovery() }, enabled = state.available && !state.busy,
                    modifier = Modifier.fillMaxWidth().testTag("studio-api-key"), label = { Text("Clé API") },
                    singleLine = true, shape = LearningShape,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
                Text("La clé n’est pas enregistrée sur le disque ; elle reste en mémoire jusqu’à la fermeture de l’application.", color = Graphite, fontSize = 13.sp)
                OutlinedButton(onClick = { controller.discoverApiModels(baseUrl, apiKey) }, enabled = state.available && !state.busy && !state.apiModelsBusy && baseUrl.isNotBlank()) {
                    Text(if (state.apiModelsBusy) "Chargement des modèles…" else "Charger les modèles disponibles")
                }
                if (!state.apiModelsError.isNullOrBlank()) Text(state.apiModelsError!!, color = ActionRed, fontSize = 13.sp)
                if (state.apiModelsUrl == baseUrl.trim().trimEnd('/') && state.apiModels.isNotEmpty()) {
                    Text("Liste fournie par le service ; vérifie que le modèle prend en charge Chat Completions.", color = Graphite, fontSize = 13.sp)
                    OutlinedTextField(value = modelSearch, onValueChange = { modelSearch = it }, modifier = Modifier.fillMaxWidth(),
                        label = { Text("Rechercher un modèle") }, singleLine = true)
                    Box {
                        OutlinedButton(onClick = { showApiModels = true }, enabled = !state.busy) { Text("Choisir dans les modèles disponibles") }
                        DropdownMenu(expanded = showApiModels, onDismissRequest = { showApiModels = false }) {
                            state.apiModels.filter { it.contains(modelSearch, ignoreCase = true) }.take(100).forEach { candidate ->
                                DropdownMenuItem(onClick = { model = candidate; showApiModels = false }) { Text(candidate) }
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = model, onValueChange = { model = it }, enabled = state.available && !state.busy,
                    modifier = Modifier.fillMaxWidth(), label = { Text("Modèle") },
                    placeholder = { Text("Nom du modèle fourni par le service") }, singleLine = true, shape = LearningShape,
                )
                if (!state.connectionError.isNullOrBlank()) Text(state.connectionError!!, color = ActionRed, fontSize = 13.sp)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (controller.configure(baseUrl.trim(), model.trim(), apiKey)) {
                        apiKey = ""
                        onDismiss()
                    }
                }, enabled = state.available && !state.busy && baseUrl.isNotBlank() && model.isNotBlank(),
                modifier = Modifier.heightIn(min = 48.dp), shape = LearningShape,
            ) { Text("Enregistrer la connexion API") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !state.busy) { Text("Fermer") } },
    )
}

@Composable
private fun StudioRoutingEditor(label: String, setting: StudioModelSetting?, state: StudioState, onSave: (String, ReasoningEffort?) -> Unit) {
    var model by remember(setting) { mutableStateOf(setting?.model?.model.orEmpty()) }
    var effort by remember(setting) { mutableStateOf(setting?.reasoningEffort) }
    var expanded by remember { mutableStateOf(false) }
    OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text(label) }, singleLine = true,
        enabled = state.available && !state.busy, modifier = Modifier.fillMaxWidth())
    Box {
        OutlinedButton(onClick = { expanded = true }, enabled = state.available && !state.busy) { Text("Réflexion : ${effort?.label ?: "Hériter"}") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(onClick = { effort = null; expanded = false }) { Text("Hériter") }
            ReasoningEffort.entries.forEach { candidate ->
                DropdownMenuItem(onClick = { effort = candidate; expanded = false }) { Text(candidate.label) }
            }
        }
    }
    if (setting?.model != null && (setting.model.providerMode != state.providerMode || (state.providerMode == "api" && setting.model.baseUrl != state.baseUrl))) {
        Text("Ce réglage appartient à une autre connexion. Enregistre-le à nouveau pour utiliser le fournisseur actif.", color = ActionRed, fontSize = 13.sp)
    }
    TextButton(onClick = { onSave(model, effort) }, enabled = state.available && !state.busy) { Text("Enregistrer : $label") }
}

@Composable
private fun SessionPickerDialog(
    state: StudioState,
    onSelect: (String) -> Unit,
    onNewGoal: (StudioSessionKind) -> Unit,
    onPlan: () -> Unit,
    onDismiss: () -> Unit,
) {
    var filter by remember { mutableStateOf<StudioSessionKind?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choisir une séance") },
        text = {
            Column {
                Row {
                    listOf(null to "Toutes", StudioSessionKind.TEACHING to "Cours", StudioSessionKind.PRACTICE to "Pratique").forEach { (kind, label) ->
                        TextButton(onClick = { filter = kind }, contentPadding = PaddingValues(horizontal = 4.dp)) { Text(label, color = if (filter == kind) ActionRed else Graphite) }
                    }
                }
                LazyColumn(Modifier.heightIn(max = 300.dp)) {
                items(state.sessions.filter { filter == null || it.kind == filter }, key = { it.id }) { session ->
                    TextButton(onClick = { onSelect(session.id) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(session.goal, modifier = Modifier.fillMaxWidth(), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                }
            }
        },
        confirmButton = { Row {
            TextButton(onClick = { onNewGoal(StudioSessionKind.TEACHING) }, enabled = state.available && !state.busy) { Text("+ Apprendre") }
            TextButton(onClick = { onNewGoal(StudioSessionKind.PRACTICE) }, enabled = state.available && !state.busy) { Text("+ Pratique") }
        } },
        dismissButton = { Row { if (state.selectedId != null) TextButton(onClick = onPlan, enabled = !state.busy, modifier = Modifier.testTag("studio-plan")) { Text("Plan") }; TextButton(onClick = onDismiss) { Text("Fermer") } } },
    )
}

@Composable
private fun NewGoalDialog(enabled: Boolean, kind: StudioSessionKind, onCreate: (String, StudioSessionKind) -> Boolean, onDismiss: () -> Unit) {
    var goal by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (kind == StudioSessionKind.TEACHING) "Nouvelle séance d’apprentissage" else "Nouvelle séance de pratique") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Une question précise aide ton tuteur à partir de ton niveau.", color = Graphite)
                OutlinedTextField(
                    value = goal, onValueChange = { goal = it }, enabled = enabled,
                    modifier = Modifier.fillMaxWidth(), label = { Text("Qu’aimerais-tu apprendre ?") },
                    placeholder = { Text("Ex. comprendre les valeurs propres") }, minLines = 2, maxLines = 4,
                    shape = LearningShape,
                )
            }
        },
        confirmButton = {
            Button(onClick = { if (onCreate(goal.trim(), kind)) onDismiss() }, enabled = enabled && goal.isNotBlank(), modifier = Modifier.heightIn(min = 48.dp), shape = LearningShape) { Text("Commencer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

@Composable
private fun StatusNote(message: String, warning: Boolean, modifier: Modifier = Modifier, tag: String? = null) {
    Surface(
        modifier = modifier.then(if (tag == null) Modifier else Modifier.testTag(tag)),
        color = if (warning) ActionRed.copy(alpha = 0.07f) else Paper,
        shape = LearningShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (warning) ActionRed.copy(alpha = 0.3f) else Concrete),
    ) {
        Text(message, Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)
            .semantics { liveRegion = LiveRegionMode.Polite }, color = if (warning) ActionRed else Ink, fontSize = 14.sp)
    }
}

private val ColorStudioLearner = androidx.compose.ui.graphics.Color(0xFFF0EDE6)
