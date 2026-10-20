package com.duolingo.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duolingo.app.model.Exercise
import com.duolingo.app.model.Lesson
import com.duolingo.app.model.practiceWordCount
import com.duolingo.app.model.canFinishPractice
import com.duolingo.app.viewmodel.AppScreen
import com.duolingo.app.viewmodel.AppViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LessonQuizScreen(viewModel: AppViewModel) {
    val activeLesson by viewModel.activeLesson.collectAsState()
    val index by viewModel.currentExerciseIndex.collectAsState()
    val selectedOption by viewModel.selectedOptionIndex.collectAsState()
    val selectedTokens by viewModel.selectedTokenSequence.collectAsState()
    val checked by viewModel.isAnswerChecked.collectAsState()
    val correct by viewModel.isAnswerCorrect.collectAsState()
    val completed by viewModel.isLessonCompleted.collectAsState()
    val stats by viewModel.userStats.collectAsState()
    val awardedXp by viewModel.lastAwardedXp.collectAsState()
    val reviewedCriteria by viewModel.reviewedCriteria.collectAsState()
    val persistenceStatus by viewModel.persistenceStatus.collectAsState()
    val heardAudioPaths by viewModel.heardAudioPaths.collectAsState()
    val transcriptModePaths by viewModel.transcriptModePaths.collectAsState()
    val recordingMillis by viewModel.verifiedRecordingMillis.collectAsState()
    val recordingReplayed by viewModel.recordingReplayed.collectAsState()
    val recordingBusy by viewModel.spokenRecordingBusy.collectAsState()
    val lesson = activeLesson
    if (lesson == null) {
        Column { EmptyLearningState("Choisis une leçon", "Aucune leçon n’est ouverte."); LearningButton("Retour aux cours", { viewModel.navigateTo(AppScreen.LEARNING_PATH) }) }
        return
    }
    var showNotes by remember(lesson.id) { mutableStateOf(true) }
    var showHelp by remember(lesson.id) { mutableStateOf(false) }
    val exit = { viewModel.navigateTo(AppScreen.LEARNING_PATH) }
    if (completed) { LessonCompletedTriumphView(awardedXp, exit); return }
    val exercise = lesson.exercises.getOrNull(index)
    val questionScrollState = key(lesson.id, showNotes, index) { rememberScrollState() }
    val unsupported = exercise is Exercise.MatchingPairs
    val written = exercise as? Exercise.WrittenProduction
    val spoken = exercise as? Exercise.SpokenProduction
    val writtenResponse = written?.let { stats.writtenDrafts[it.id] }.orEmpty()
    Scaffold(backgroundColor = Paper,
        topBar = {
            Surface(color = White) {
                Column(Modifier.fillMaxWidth().padding(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(exit, Modifier.size(48.dp)) { Icon(Icons.Default.Close, "Quitter la leçon") }
                        Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                            Text(lesson.title, fontWeight = FontWeight.Bold)
                            Text(if (showNotes) "Le cours avant la pratique" else "Exercice ${index + 1} sur ${lesson.exercises.size}", color = Graphite, fontSize = 14.sp)
                        }
                        IconButton({ showHelp = true }, Modifier.size(48.dp)) { Icon(Icons.Default.Info, "Ouvrir le cours et l’indice") }
                    }
                    if (!showNotes && exercise != null) LinearProgressIndicator(index.toFloat() / lesson.exercises.size.coerceAtLeast(1), Modifier.fillMaxWidth().height(8.dp), color = BrandRed, backgroundColor = Concrete)
                }
            }
        },
        bottomBar = {
            Surface(color = White) {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    Column(Modifier.widthIn(max = 760.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (checked && !showNotes) Text(
                        if (written != null || spoken != null) "Entraînement auto-évalué terminé" else if (correct) "Bonne réponse" else "À corriger · consulte l’explication puis réessaie",
                        color = if (correct) Ink else ActionRed, fontWeight = FontWeight.Bold)
                    when {
                        showNotes -> LearningButton(if (lesson.exercises.isEmpty()) "Retour aux cours" else "Passer aux exercices", { if (lesson.exercises.isEmpty()) exit() else showNotes = false }, Modifier.widthIn(max = 760.dp).fillMaxWidth())
                        exercise == null || unsupported -> LearningButton("Retour aux cours", exit, Modifier.widthIn(max = 760.dp).fillMaxWidth())
                        checked && !correct -> LearningButton("Réessayer", viewModel::retryExercise, Modifier.widthIn(max = 760.dp).fillMaxWidth())
                        checked -> LearningButton("Continuer", viewModel::nextExercise, Modifier.widthIn(max = 760.dp).fillMaxWidth())
                        spoken != null -> LearningButton("Terminer ma réécoute et mon bilan", viewModel::checkAnswer, Modifier.fillMaxWidth(), spoken.canFinishPractice(recordingMillis, recordingReplayed, recordingBusy, reviewedCriteria))
                        written != null -> LearningButton("Terminer ma relecture", viewModel::checkAnswer, Modifier.widthIn(max = 760.dp).fillMaxWidth(), written.canFinishPractice(writtenResponse, reviewedCriteria))
                        else -> LearningButton("Vérifier ma réponse", viewModel::checkAnswer, Modifier.widthIn(max = 760.dp).fillMaxWidth(),
                            (selectedOption != null || selectedTokens.isNotEmpty()) &&
                                (exercise !is Exercise.ListeningChoice || exercise.audioResourcePath in heardAudioPaths || exercise.audioResourcePath in transcriptModePaths))
                    }
                    }
                }
            }
        }) { padding ->
        Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 920.dp).fillMaxWidth().verticalScroll(questionScrollState).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                when {
                    showNotes -> LessonNotes(lesson)
                    exercise == null -> EmptyLearningState("Aucun exercice disponible", "Les notes du cours restent accessibles avec le bouton d’aide.")
                    unsupported -> EmptyLearningState("Association de paires indisponible", "Cet exercice n’est pas encore pris en charge. Reviens au cours pour choisir une autre leçon ; aucune réponse ni récompense ne sera enregistrée ici.")
                    else -> {
                        if (written != null || spoken != null) {
                            Text(if (spoken != null) "Ta production orale" else "Ta production écrite",
                                fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.semantics { heading() })
                            Text(exercise.prompt, fontSize = 18.sp, lineHeight = 28.sp)
                        } else {
                            Text(exercise.prompt, fontSize = 26.sp, lineHeight = 34.sp,
                                fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
                        }
                        if (lesson.objective.isNotBlank()) Text("Objectif : ${lesson.objective}", color = Graphite)
                        when (exercise) {
                            is Exercise.ReadingChoice -> {
                                Surface(color = White, shape = LearningShape) {
                                    SelectionContainer {
                                        Text(exercise.passage, Modifier.fillMaxWidth().padding(20.dp),
                                            fontSize = 18.sp, lineHeight = 28.sp, fontWeight = FontWeight.Normal)
                                    }
                                }
                                OptionsList(exercise.options, selectedOption, checked, viewModel::selectOption)
                            }
                            is Exercise.SpokenProduction -> SpokenProductionPractice(exercise, viewModel, checked)
                            is Exercise.ListeningChoice -> {
                                ListeningPractice(exercise, checked, exercise.audioResourcePath in heardAudioPaths,
                                    exercise.audioResourcePath in transcriptModePaths, viewModel::markAudioCompleted, viewModel::enableTranscriptMode)
                                OptionsList(exercise.options, selectedOption, checked, viewModel::selectOption)
                            }
                            is Exercise.WrittenProduction -> WrittenProductionPractice(exercise, writtenResponse, reviewedCriteria, checked, persistenceStatus, viewModel::updateWrittenResponse, viewModel::toggleReviewCriterion)
                            is Exercise.CodeChallenge -> {
                                Surface(color = Ink, shape = LearningShape) {
                                    Text(exercise.codeSnippet, Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(20.dp), color = White, fontFamily = FontFamily.Monospace, fontSize = 16.sp)
                                }
                                OptionsList(exercise.options, selectedOption, checked, viewModel::selectOption)
                            }
                            is Exercise.MathChallenge -> {
                                exercise.formula?.let { Surface(color = White, shape = LearningShape) { Text(it, Modifier.fillMaxWidth().padding(20.dp), fontSize = 24.sp, fontWeight = FontWeight.Medium) } }
                                OptionsList(exercise.options, selectedOption, checked, viewModel::selectOption)
                            }
                            is Exercise.MultipleChoice -> OptionsList(exercise.options, selectedOption, checked, viewModel::selectOption)
                            is Exercise.Translation -> {
                                Surface(color = White, shape = LearningShape) { Text(exercise.sourceSentence, Modifier.fillMaxWidth().padding(20.dp), fontSize = 20.sp) }
                                Text("Ta réponse", fontWeight = FontWeight.Bold)
                                Surface(color = White, shape = LearningShape) {
                                    Text(selectedTokens.joinToString(" ").ifBlank { "Choisis les mots dans l’ordre." }, Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(16.dp), color = if (selectedTokens.isEmpty()) Graphite else Ink, fontSize = 18.sp)
                                }
                                Text("Touche à nouveau un mot pour le retirer.", color = Graphite)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    exercise.targetTokens.forEach { token -> TokenChip(token, token in selectedTokens, { viewModel.toggleTokenSelection(token) }, !checked) }
                                }
                            }
                            is Exercise.MatchingPairs -> Unit
                        }
                        if (checked) Surface(color = White, shape = LearningShape, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(if (written != null || spoken != null) "Auto-évaluation terminée" else if (correct) "Bien joué !" else "À retenir", color = if (correct) Ink else ActionRed, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                                Text(exercise.explanation, fontSize = 16.sp)
                                Text("${stats.hearts}/${stats.maxHearts} vies restantes", color = Graphite)
                            }
                        }
                    }
                }
            }
        }
    }
    if (showHelp) AlertDialog(onDismissRequest = { showHelp = false },
        title = { Text("Le cours et l’indice") },
        text = {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                LessonNotes(lesson)
                if (!showNotes && exercise != null) {
                    Text("Indice pour cet exercice", fontWeight = FontWeight.Bold)
                    Text(exercise.hint)
                }
            }
        },
        confirmButton = { TextButton({ showHelp = false }, Modifier.heightIn(min = 48.dp)) { Text("Revenir à la leçon") } },
        shape = LearningShape, backgroundColor = White)
}

@Composable
private fun WrittenProductionPractice(
    exercise: Exercise.WrittenProduction, response: String, reviewed: Set<Int>, checked: Boolean, persistenceStatus: String,
    updateResponse: (String) -> Unit, toggleCriterion: (Int) -> Unit
) {
    // Material value/onValueChange keeps editing state hoisted on the project's Compose 1.6 API.
    // https://developer.android.com/develop/ui/compose/text/user-input#value-based
    var showExample by remember(exercise.id) { mutableStateOf(false) }
    val count = practiceWordCount(response)
    Text("Atelier avec auto-évaluation. Aucun score officiel ni correction automatique de la langue.", color = Graphite)
    OutlinedTextField(response, updateResponse, Modifier.fillMaxWidth().heightIn(min = 240.dp),
        enabled = !checked, label = { Text("Ta production écrite") },
        isError = count > exercise.maxWords,
        colors = TextFieldDefaults.outlinedTextFieldColors(focusedBorderColor = Ink, cursorColor = Ink))
    Text("$count mots · objectif ${exercise.minWords}–${exercise.maxWords}. Comptage indicatif par espaces.", color = if (count in exercise.minWords..exercise.maxWords) Ink else Graphite)
    Text(persistenceStatus, color = if (persistenceStatus.contains("session")) ActionRed else Graphite)
    Text("Une modification réinitialise la relecture.", color = Graphite)
    Text("Relis ta réponse", fontSize = 20.sp, fontWeight = FontWeight.Bold)
    exercise.reviewCriteria.forEachIndexed { index, criterion ->
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(index in reviewed, if (checked) null else { _ -> toggleCriterion(index) }, Modifier.semantics { contentDescription = criterion })
            Text(criterion, Modifier.weight(1f), fontSize = 16.sp)
        }
    }
    TextButton({ showExample = !showExample }, Modifier.heightIn(min = 48.dp)) {
        Text(if (showExample) "Masquer l’exemple" else "Comparer avec un exemple rédigé")
    }
    if (showExample || checked) Surface(color = White, shape = LearningShape) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Un exemple possible, à comparer avec ton texte", fontWeight = FontWeight.Bold)
            Text(exercise.exampleResponse, fontSize = 16.sp, lineHeight = 25.sp)
        }
    }
}

@Composable
private fun LessonNotes(lesson: Lesson) {
    val uriHandler = LocalUriHandler.current
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Comprendre avant de pratiquer", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
        if (lesson.objective.isNotBlank()) Text("Objectif : ${lesson.objective}", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Text(lesson.learningNotes.ifBlank { "Les notes de cette leçon ne sont pas encore disponibles. Consulte les ressources de référence lorsqu’elles sont proposées." }, fontSize = 16.sp, lineHeight = 25.sp)
        if (lesson.sourceTitle.isNotBlank()) Text("Source : ${lesson.sourceTitle}", color = Graphite)
        if (lesson.sourceUrls.isNotEmpty()) {
            Text("Pour aller plus loin", fontWeight = FontWeight.Bold)
            lesson.sourceUrls.forEachIndexed { index, url ->
                TextButton({ runCatching { uriHandler.openUri(url) } }, Modifier.heightIn(min = 48.dp)) { Text("Ressource ${index + 1} · ${url.substringAfter("://").substringBefore('/')}") }
            }
        }
    }
}

@Composable
fun OptionsList(options: List<String>, selectedIndex: Int?, isChecked: Boolean, onSelect: (Int) -> Unit) {
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        options.forEachIndexed { index, option ->
            val selected = selectedIndex == index
            val interaction = remember { MutableInteractionSource() }
            OutlinedButton({ onSelect(index) }, Modifier.fillMaxWidth().heightIn(min = 64.dp).learningFocus(interaction).semantics { this.selected = selected; role = Role.RadioButton },
                enabled = !isChecked, interactionSource = interaction, shape = LearningShape,
                border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Ink else Concrete),
                colors = ButtonDefaults.outlinedButtonColors(backgroundColor = if (selected) Paper else White, contentColor = Ink, disabledContentColor = Ink), contentPadding = PaddingValues(16.dp)) {
                RadioButton(selected, null, colors = RadioButtonDefaults.colors(selectedColor = Ink, unselectedColor = Graphite))
                Spacer(Modifier.width(12.dp)); Text(option, Modifier.weight(1f), fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun TokenChip(token: String, selected: Boolean, onClick: () -> Unit, enabled: Boolean = true) {
    val interaction = remember { MutableInteractionSource() }
    OutlinedButton(onClick, Modifier.heightIn(min = 48.dp).learningFocus(interaction).semantics { this.selected = selected },
        enabled = enabled, interactionSource = interaction, shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(backgroundColor = if (selected) Ink else White, contentColor = if (selected) White else Ink, disabledContentColor = if (selected) White else Ink), border = BorderStroke(1.dp, Ink)) { Text(token, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
}

@Composable
fun LessonCompletedTriumphView(xpReward: Int, onFinish: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Paper), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 640.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(32.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Icon(Icons.Default.CheckCircle, null, Modifier.size(72.dp), tint = BrandRed)
            Text("Une leçon de plus !", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            Text(if (xpReward > 0) "+$xpReward XP. Ta progression avance." else "Révision terminée. Tu consolides tes acquis sans ajouter de XP.", fontSize = 18.sp)
            LearningButton("Revenir aux cours", onFinish, Modifier.fillMaxWidth())
        }
    }
}
