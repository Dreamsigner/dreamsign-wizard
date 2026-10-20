package com.duolingo.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duolingo.app.media.LocalPracticeMedia
import com.duolingo.app.media.PracticeMedia
import com.duolingo.app.model.Exercise
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive

@Composable
fun ListeningPractice(exercise: Exercise.ListeningChoice, checked: Boolean, heard: Boolean, transcriptMode: Boolean,
    onComplete: (String) -> Unit, onTranscriptMode: (String) -> Unit) {
    val media = LocalPracticeMedia.current
    key(media, exercise.id) {
        ListeningPracticeContent(exercise, checked, heard, transcriptMode, onComplete, onTranscriptMode, media)
    }
}

@Composable
private fun ListeningPracticeContent(exercise: Exercise.ListeningChoice, checked: Boolean, heard: Boolean, transcriptMode: Boolean,
    onComplete: (String) -> Unit, onTranscriptMode: (String) -> Unit, media: PracticeMedia) {
    val player = remember(media, exercise.id) { media.createPlayer() }
    val scope = rememberCoroutineScope()
    var playing by remember(exercise.id) { mutableStateOf(false) }
    var error by remember(exercise.id) { mutableStateOf<String?>(null) }
    var loadingJob by remember(exercise.id) { mutableStateOf<Job?>(null) }
    DisposableEffect(media, player) {
        val unbind = media.bindPlayerLifecycle(player) { loadingJob?.cancel(); playing = false }
        onDispose { loadingJob?.cancel(); unbind(); player.stop() }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Entraînement d’écoute · voix de synthèse française · texte original", color = Graphite)
        LearningButton(if (playing) "Écoute en cours…" else if (heard) "Réécouter le document" else "Écouter le document", {
            playing = true
            error = null
            loadingJob = scope.launch {
                try {
                    // Bundled bytes; no network requests or runtime speech generation.
                    // https://kotlinlang.org/docs/multiplatform/compose-multiplatform-resources-usage.html#raw-files
                    val bytes = media.loadResource(exercise.audioResourcePath)
                    coroutineContext.ensureActive()
                    player.play(bytes, onComplete = {
                        scope.launch { playing = false; onComplete(exercise.id) }
                    }, onError = {
                        scope.launch { playing = false; error = "Lecture audio impossible. Vérifie la sortie audio puis réessaie." }
                    })
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    playing = false
                    error = "Le document audio n’a pas pu être chargé. Réessaie ; l’exercice reste non validé."
                }
            }
        }, Modifier.fillMaxWidth(), enabled = !playing)
        if (playing) TextButton({ loadingJob?.cancel(); player.stop(); playing = false }, Modifier.heightIn(min = 48.dp)) { Text("Arrêter la lecture") }
        error?.let { Text(it, color = ActionRed) }
        Text(if (transcriptMode) "Mode lecture : cette tentative entraîne la compréhension écrite. Elle sera identifiée comme telle dans ton profil."
            else if (heard) "Document écouté. Tu peux répondre ou le réécouter." else "Écoute le document jusqu’au bout avant de vérifier ta réponse.", color = Graphite)
        if (!transcriptMode && !checked) TextButton({
            loadingJob?.cancel(); player.stop(); playing = false; onTranscriptMode(exercise.id)
        }, Modifier.heightIn(min = 48.dp)) { Text("Accessibilité : travailler à partir de la transcription") }
        if (checked || transcriptMode) Surface(color = White, shape = LearningShape) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (transcriptMode) "Transcription · mode lecture" else "Transcription pour la correction", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(exercise.transcript, fontSize = 16.sp, lineHeight = 25.sp)
            }
        }
    }
}
