package com.duolingo.app.ui.studio

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
import com.duolingo.app.ui.*

@Composable
fun StudioScreen() {
    val controller = LocalStudioController.current
    val state by controller.state.collectAsState()
    var showConnection by remember { mutableStateOf(false) }
    var showSessions by remember { mutableStateOf(false) }
    var showGoal by remember { mutableStateOf(false) }

    if (showConnection) StudioConnectionDialog(controller, state, onDismiss = { showConnection = false })
    if (showSessions) SessionPickerDialog(
        state = state,
        onSelect = { controller.select(it); showSessions = false },
        onNewGoal = { showSessions = false; showGoal = true },
        onDismiss = { showSessions = false },
    )
    if (showGoal) NewGoalDialog(
        enabled = state.available && !state.busy,
        onCreate = { if (controller.create(it)) { showGoal = false; true } else false },
        onDismiss = { showGoal = false },
    )

    BoxWithConstraints(Modifier.fillMaxSize().background(Paper)) {
        val wide = maxWidth >= 800.dp
        val session = state.sessions.firstOrNull { it.id == state.selectedId }
        Column(Modifier.fillMaxSize().padding(horizontal = if (wide) 28.dp else 16.dp, vertical = if (wide) 22.dp else 14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Atelier IA", style = MaterialTheme.typography.h4, color = Ink)
                    Text("Un tuteur qui part de ce que tu sais déjà.", color = Graphite, fontSize = 14.sp)
                }
                OutlinedButton(
                    onClick = { showConnection = true }, enabled = state.available && !state.busy,
                    modifier = Modifier.heightIn(min = 48.dp), shape = LearningShape,
                ) { Text("Connexion IA", maxLines = 1) }
            }
            if (state.providerMode == "chatgpt" && state.chatGptConnected) {
                Text("ChatGPT · ${state.chatGptEmail.orEmpty()} · ${state.model}", Modifier.fillMaxWidth().padding(top = 4.dp),
                    color = Graphite, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else if (state.available && state.baseUrl.isNotBlank() && state.model.isNotBlank()) {
                Text(
                    "${state.baseUrl} · ${state.model}", Modifier.fillMaxWidth().padding(top = 4.dp),
                    color = Graphite, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            if (state.storageError != null) StatusNote(state.storageError!!, warning = true, Modifier.padding(top = 10.dp))
            if (!state.available && !state.error.isNullOrBlank()) {
                StatusNote(state.error!!, warning = true, Modifier.padding(top = 10.dp), tag = "studio-unavailable")
            }

            if (wide) {
                Row(Modifier.weight(1f).fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    SessionSidebar(
                        state = state,
                        modifier = Modifier.width(270.dp).fillMaxHeight(),
                        onSelect = controller::select,
                        onCreate = controller::create,
                    )
                    StudioWorkspace(
                        state = state, session = session, controller = controller,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            } else {
                if (state.sessions.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = { showSessions = true }, enabled = !state.busy,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = LearningShape,
                        ) {
                            Text(
                                session?.goal ?: "Choisir une séance",
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                        OutlinedButton(
                            onClick = { showGoal = true }, enabled = state.available && !state.busy,
                            modifier = Modifier.heightIn(min = 48.dp), shape = LearningShape,
                        ) { Text("Nouvel objectif") }
                    }
                }
                StudioWorkspace(
                    state = state, session = session, controller = controller,
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 14.dp),
                    compactGoal = state.sessions.isEmpty(),
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
    }
}

@Composable
private fun SessionSidebar(
    state: StudioState,
    modifier: Modifier,
    onSelect: (String) -> Unit,
    onCreate: (String) -> Boolean,
) {
    Surface(modifier, shape = LearningShape, color = White, elevation = 1.dp) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Un nouvel objectif", style = MaterialTheme.typography.h6, color = Ink)
            GoalForm(enabled = state.available && !state.busy, onCreate = onCreate)
            Divider(color = Concrete)
            Text("Séances enregistrées", style = MaterialTheme.typography.subtitle1, color = Ink)
            if (state.sessions.isEmpty()) {
                Text("Tes échanges réapparaîtront ici.", color = Graphite, fontSize = 14.sp)
            } else {
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(state.sessions, key = { it.id }) { item ->
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
private fun GoalForm(enabled: Boolean, onCreate: (String) -> Boolean) {
    var goal by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = goal, onValueChange = { goal = it }, enabled = enabled,
            modifier = Modifier.fillMaxWidth(), label = { Text("Qu’aimerais-tu apprendre ?") },
            placeholder = { Text("Ex. comprendre les valeurs propres") },
            minLines = 2, maxLines = 4, shape = LearningShape,
        )
        Button(
            onClick = { if (onCreate(goal.trim())) goal = "" }, enabled = enabled && goal.isNotBlank(),
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
                    GoalForm(enabled = state.available && !state.busy, onCreate = controller::create)
                    if (state.available) Text("Tes autres cours ne sont pas envoyés au fournisseur.", color = Graphite, fontSize = 13.sp)
                }
            } else {
                Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center) {
                    Text("Commence par un objectif précis", style = MaterialTheme.typography.h5, color = Ink)
                    Text("Par exemple : comprendre les valeurs propres, apprendre les bases de Python ou préparer une tâche du TCF Canada.",
                        Modifier.padding(top = 10.dp), color = Graphite, fontSize = 16.sp, lineHeight = 24.sp)
                    if (state.available) Text("L’objectif, les messages et le plan accepté de cette séance seront envoyés au fournisseur configuré.",
                        Modifier.padding(top = 18.dp), color = Graphite, fontSize = 14.sp)
                }
            }
        } else {
            Conversation(state, session, controller)
        }
    }
}

@Composable
private fun Conversation(state: StudioState, session: StudioSession, controller: StudioController) {
    var draft by remember(session.id) { mutableStateOf("") }
    var showPlan by remember(session.id) { mutableStateOf(false) }
    if (showPlan) StudioPlanDialog(session, controller, onDismiss = { showPlan = false }, onDiagnosis = {
        draft = "Peux-tu approfondir le diagnostic de mes prérequis avant de poursuivre le plan ?"
        showPlan = false
    })
    val messagesState = rememberLazyListState()
    LaunchedEffect(session.id, session.messages.size) {
        if (session.messages.isNotEmpty()) messagesState.animateScrollToItem(session.messages.lastIndex)
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(session.goal, Modifier.weight(1f).testTag("studio-current-goal"), style = MaterialTheme.typography.h5, color = Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            TextButton(onClick = { showPlan = true }, enabled = !state.busy && state.storageError == null,
                modifier = Modifier.testTag("studio-plan")) { Text("Plan") }
        }
        if (state.providerMode == "chatgpt" && state.chatGptConnected) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Utilise ton forfait ChatGPT", color = Graphite, fontSize = 13.sp)
                TextButton(onClick = controller::openChatGptUsage) { Text("Gérer l’usage") }
            }
        }
        Divider(Modifier.padding(top = 12.dp), color = Concrete)
        if (session.messages.isEmpty()) {
            Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("À toi de jouer", style = MaterialTheme.typography.h6, color = Ink)
                Text("Décris ce que tu sais déjà ou pose ta première question.", Modifier.padding(top = 6.dp), color = Graphite)
            }
        } else {
            LazyColumn(
                state = messagesState, modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 12.dp).testTag("studio-conversation-messages"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(session.messages.size, key = { index -> "${session.id}-$index" }) { index ->
                    val message = session.messages[index]
                    val learner = message.role != "assistant"
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (learner) Arrangement.End else Arrangement.Start) {
                        Surface(
                            modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(0.88f),
                            color = if (learner) ColorStudioLearner else Paper,
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            LessonDocument(message.text, typesetMath = true, modifier = Modifier.padding(14.dp))
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
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
    }
}

@Composable
private fun StudioConnectionDialog(controller: StudioController, state: StudioState, onDismiss: () -> Unit) {
    var baseUrl by remember(state.baseUrl) { mutableStateOf(state.baseUrl) }
    var model by remember(state.model) { mutableStateOf(state.model) }
    var apiKey by remember { mutableStateOf("") }
    var showModels by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Connexion IA") },
        text = {
            Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Abonnement ChatGPT", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold)
                if (state.chatGptConnected) {
                    Text("Connecté : ${state.chatGptEmail.orEmpty()}", color = Graphite)
                    Box {
                        OutlinedButton(onClick = { showModels = true }, enabled = state.chatGptModels.isNotEmpty() && !state.busy) {
                            Text("Modèle : ${state.model.ifBlank { "Choisir" }}")
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
                Text("Autre fournisseur avec clé API", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold)
                Text("L’objectif, les messages et le plan accepté de la séance active sont transmis au fournisseur choisi.", color = Graphite)
                OutlinedTextField(
                    value = baseUrl, onValueChange = { baseUrl = it }, enabled = state.available && !state.busy,
                    modifier = Modifier.fillMaxWidth(), label = { Text("URL de base") },
                    placeholder = { Text("https://api.openai.com/v1") }, singleLine = true, shape = LearningShape,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
                OutlinedTextField(
                    value = model, onValueChange = { model = it }, enabled = state.available && !state.busy,
                    modifier = Modifier.fillMaxWidth(), label = { Text("Modèle") },
                    placeholder = { Text("Nom du modèle fourni par le service") }, singleLine = true, shape = LearningShape,
                )
                OutlinedTextField(
                    value = apiKey, onValueChange = { apiKey = it }, enabled = state.available && !state.busy,
                    modifier = Modifier.fillMaxWidth().testTag("studio-api-key"), label = { Text("Clé API") },
                    singleLine = true, shape = LearningShape,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
                Text("La clé n’est pas enregistrée sur le disque ; elle reste en mémoire jusqu’à la fermeture de l’application.", color = Graphite, fontSize = 13.sp)
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
            ) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !state.busy) { Text("Fermer") } },
    )
}

@Composable
private fun SessionPickerDialog(
    state: StudioState,
    onSelect: (String) -> Unit,
    onNewGoal: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choisir une séance") },
        text = {
            LazyColumn(Modifier.heightIn(max = 360.dp)) {
                items(state.sessions, key = { it.id }) { session ->
                    TextButton(onClick = { onSelect(session.id) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(session.goal, modifier = Modifier.fillMaxWidth(), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onNewGoal, enabled = state.available && !state.busy) { Text("Nouvel objectif") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Fermer") } },
    )
}

@Composable
private fun NewGoalDialog(enabled: Boolean, onCreate: (String) -> Boolean, onDismiss: () -> Unit) {
    var goal by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvel objectif") },
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
            Button(onClick = { if (onCreate(goal.trim())) onDismiss() }, enabled = enabled && goal.isNotBlank(), modifier = Modifier.heightIn(min = 48.dp), shape = LearningShape) { Text("Commencer") }
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
