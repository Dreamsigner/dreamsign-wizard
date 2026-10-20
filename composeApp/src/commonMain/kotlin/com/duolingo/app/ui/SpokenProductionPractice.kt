package com.duolingo.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duolingo.app.media.*
import com.duolingo.app.model.Exercise
import com.duolingo.app.viewmodel.AppViewModel
import kotlinx.coroutines.*

@Composable
fun SpokenProductionPractice(exercise: Exercise.SpokenProduction, model: AppViewModel, checked: Boolean) {
    // Reset the coroutine scope as well as remembered state when the exercise changes.
    key(exercise.id) { SpokenProductionPracticeContent(exercise, model, checked) }
}

@Composable
private fun SpokenProductionPracticeContent(exercise: Exercise.SpokenProduction, model: AppViewModel, checked: Boolean) {
    val recorder = remember(exercise.id) { createAudioRecorder() }
    val player = remember(exercise.id) { createAudioPlayer() }
    val scope = rememberCoroutineScope()
    val duration by model.verifiedRecordingMillis.collectAsState()
    val reviewed by model.reviewedCriteria.collectAsState()
    val replayed by model.recordingReplayed.collectAsState()
    val busy by model.spokenRecordingBusy.collectAsState()
    var bytes by remember(exercise.id) { mutableStateOf<ByteArray?>(null) }
    var loading by remember(exercise.id) { mutableStateOf(true) }
    var recording by remember(exercise.id) { mutableStateOf(false) }
    var waiting by remember(exercise.id) { mutableStateOf(false) }
    var playing by remember(exercise.id) { mutableStateOf(false) }
    var saving by remember(exercise.id) { mutableStateOf(false) }
    var elapsed by remember(exercise.id) { mutableStateOf(0) }
    var status by remember(exercise.id) { mutableStateOf("Chargement du brouillon audio…") }
    var error by remember(exercise.id) { mutableStateOf<String?>(null) }
    var prepRemaining by remember(exercise.id) { mutableStateOf(0) }
    var turn by remember(exercise.id) { mutableStateOf(0) }
    var showOutline by remember(exercise.id) { mutableStateOf(false) }
    var pendingCapture by remember { mutableStateOf<ByteArray?>(null) }
    var saveJob by remember { mutableStateOf<Job?>(null) }
    var reloadKey by remember { mutableStateOf(0) }
    var restoreFailed by remember { mutableStateOf(false) }
    var attached by remember { mutableStateOf(true) }

    fun persistCapture(captured: ByteArray) {
        if (saving) return
        pendingCapture = captured
        recording = false; waiting = false; saving = true; error = null
        model.setSpokenRecordingBusy(exercise.id, true)
        status = "Enregistrement terminé, sauvegarde locale en cours…"
        saveJob = scope.launch {
            try {
                val measured = withContext(Dispatchers.Default) {
                    val measured = recordingDurationMillis(captured)
                    require(measured in 1L..exercise.maxDurationSeconds * 1000L)
                    saveAudioRecording(exercise.id, captured)
                    measured
                }
                coroutineContext.ensureActive()
                bytes = captured
                pendingCapture = null
                restoreFailed = false
                model.confirmSpokenRecording(exercise.id, measured)
                status = "Réponse sauvegardée. Réécoute-la intégralement puis remplis le bilan."
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                error = "Sauvegarde impossible : le précédent brouillon reste sur l’appareil. La nouvelle capture reste en mémoire sur cet écran ; réessaie de la sauvegarder avant de quitter."
                status = "Nouvelle capture non enregistrée."
            } finally {
                if (attached) {
                    saving = false
                    model.setSpokenRecordingBusy(exercise.id, pendingCapture != null)
                }
            }
        }
    }

    LaunchedEffect(exercise.id, reloadKey) {
        loading = true; restoreFailed = false; error = null
        status = "Chargement du brouillon audio…"
        try {
            val loaded = withContext(Dispatchers.Default) { loadAudioRecording(exercise.id) }
            if (loaded != null) {
                val measured = recordingDurationMillis(loaded)
                require(measured in 1L..exercise.maxDurationSeconds * 1000L)
                bytes = loaded
                model.confirmSpokenRecording(exercise.id, measured)
                status = "Brouillon audio restauré. Réécoute-le avant ton bilan."
            } else status = "Aucun enregistrement pour cet atelier."
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            restoreFailed = true
            status = "Le brouillon audio n’a pas pu être chargé."
            error = "Le brouillon audio est illisible ou inaccessible. Il est conservé ; tu peux réessayer ou enregistrer une nouvelle réponse."
        }
        finally { loading = false }
    }
    LaunchedEffect(recording) {
        if (recording) { elapsed = 0; while (recording) { delay(1000); elapsed++ } }
    }
    LaunchedEffect(prepRemaining > 0) {
        while (prepRemaining > 0) { delay(1000); prepRemaining-- }
    }
    DisposableEffect(recorder, player) {
        val unbindRecorder = bindAudioRecorderLifecycle(recorder) {
            if (recording || waiting) status = "Capture interrompue en arrière-plan. Le précédent brouillon enregistré est conservé."
            recording = false; waiting = false
            model.setSpokenRecordingBusy(exercise.id, saving || pendingCapture != null)
        }
        val unbindPlayer = bindAudioLifecycle(player) { playing = false }
        onDispose {
            attached = false
            saveJob?.cancel()
            unbindRecorder(); unbindPlayer(); recorder.cancel(); player.stop()
            model.setSpokenRecordingBusy(exercise.id, false)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Production orale enregistrée sur cet appareil. Aucun envoi, transcription automatique, correction IA ou score officiel.", color = Graphite)
        Text("Durée maximale : ${exercise.maxDurationSeconds / 60} min ${exercise.maxDurationSeconds % 60} s. Minimum technique d’entraînement : ${exercise.minimumPracticeSeconds} s ; ce seuil n’est pas un critère officiel.", color = Graphite)
        if (exercise.preparationSeconds > 0) {
            TextButton({ prepRemaining = exercise.preparationSeconds }, Modifier.heightIn(min = 48.dp), enabled = !busy && !checked) {
                Text(if (prepRemaining > 0) "Préparation conseillée : $prepRemaining s restantes" else "Démarrer les ${exercise.preparationSeconds} s de préparation conseillée")
            }
        }
        if (exercise.roleplayTurns.isNotEmpty()) {
            Surface(color = White, shape = LearningShape) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Réplique d’entraînement ${turn + 1}/${exercise.roleplayTurns.size}", fontWeight = FontWeight.Bold)
                    Text(exercise.roleplayTurns[turn], fontSize = 18.sp)
                    Text("Répliques écrites et préétablies : elles ne s’adaptent pas à ta réponse et ne remplacent pas un interlocuteur réel.", color = Graphite)
                    if (turn + 1 < exercise.roleplayTurns.size) TextButton({ turn++ }, Modifier.heightIn(min = 48.dp)) { Text("Voir la réplique suivante") }
                }
            }
        }
        Text(status, color = Graphite, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        error?.let { Text(it, color = ActionRed, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
        if (restoreFailed && !loading && !busy && !playing && !checked) TextButton({ loading = true; reloadKey++ }, Modifier.heightIn(min = 48.dp)) {
            Text("Réessayer de charger le brouillon")
        }
        if (pendingCapture != null && !saving) {
            LearningButton("Réessayer la sauvegarde", { pendingCapture?.let { persistCapture(it) } }, Modifier.fillMaxWidth())
            TextButton({
                pendingCapture = null; error = null
                model.setSpokenRecordingBusy(exercise.id, false)
                status = "Capture non enregistrée abandonnée. Le précédent brouillon est conservé."
            }, Modifier.heightIn(min = 48.dp)) { Text("Abandonner la capture non enregistrée") }
        }
        if (recording) Text("Microphone actif · $elapsed s", fontWeight = FontWeight.Bold, color = ActionRed)
        if (!recording && !waiting && !saving && pendingCapture == null) LearningButton(if (bytes == null) "Enregistrer ma réponse" else "Enregistrer une nouvelle réponse", {
            player.stop(); playing = false
            error = null; waiting = true; model.setSpokenRecordingBusy(exercise.id, true)
            status = "Ouverture du microphone. L’autorisation native peut être demandée."
            recorder.start(exercise.maxDurationSeconds, onStarted = {
                waiting = false; recording = true; status = "Parle à voix haute. Arrête puis réécoute ta réponse."
            }, onComplete = { captured ->
                persistCapture(captured)
            }, onError = { message ->
                recording = false; waiting = false; model.setSpokenRecordingBusy(exercise.id, false)
                error = message; status = "Le précédent brouillon enregistré est conservé."
            })
        }, Modifier.fillMaxWidth(), enabled = !loading && !checked && !playing)
        if (recording) LearningButton("Arrêter et sauvegarder", {
            // iOS may invoke onComplete synchronously from stop: set flags first.
            recording = false; waiting = true; status = "Finalisation de l’enregistrement…"
            recorder.stop()
        }, Modifier.fillMaxWidth())
        if (recording || waiting) TextButton({ recorder.cancel(); recording = false; waiting = false; model.setSpokenRecordingBusy(exercise.id, false); status = "Capture abandonnée. Le précédent brouillon est conservé." }, Modifier.heightIn(min = 48.dp)) { Text("Abandonner cette capture") }
        if (duration != null) Text("Durée audio sauvegardée : ${duration!! / 1000} s.", fontWeight = FontWeight.Bold)
        if (bytes != null && !busy) {
            LearningButton(if (playing) "Réécoute en cours…" else "Réécouter ma réponse", {
                error = null; playing = true
                val replayBytes = checkNotNull(bytes)
                player.play(replayBytes, onComplete = { playing = false; if (bytes === replayBytes) model.markRecordingReplayed(exercise.id) },
                    onError = { playing = false; error = "La réécoute n’a pas pu se terminer. Vérifie la sortie audio puis réessaie." })
            }, Modifier.fillMaxWidth(), enabled = !playing)
            if (playing) TextButton({ player.stop(); playing = false }, Modifier.heightIn(min = 48.dp)) { Text("Arrêter la réécoute") }
        }
        Text(if (replayed) "Réécoute terminée. Fais maintenant ton bilan." else "Le bilan nécessite une réponse sauvegardée et une réécoute complète.", color = Graphite)
        exercise.reviewCriteria.forEachIndexed { index, criterion ->
            val selected = index in reviewed
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(selected, enabled = replayed && !busy && !checked,
                role = Role.Checkbox, onValueChange = { model.toggleReviewCriterion(index) }), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(selected, null, enabled = replayed && !busy && !checked)
                Text(criterion, Modifier.weight(1f), fontSize = 16.sp)
            }
        }
        TextButton({ showOutline = !showOutline }, Modifier.heightIn(min = 48.dp)) { Text(if (showOutline) "Masquer la piste de réponse" else "Voir une piste de réponse pour comparer") }
        if (showOutline || checked) Surface(color = White, shape = LearningShape) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Piste possible, sans barème officiel", fontWeight = FontWeight.Bold)
                Text(exercise.exampleOutline, fontSize = 16.sp, lineHeight = 25.sp)
            }
        }
    }
}
