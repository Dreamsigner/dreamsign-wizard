package com.duolingo.app.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duolingo.app.model.Exercise
import com.duolingo.app.viewmodel.AppScreen
import com.duolingo.app.viewmodel.AppViewModel

@Composable
fun LessonQuizScreen(viewModel: AppViewModel) {
    val activeLesson by viewModel.activeLesson.collectAsState()
    val currentIndex by viewModel.currentExerciseIndex.collectAsState()
    val selectedOption by viewModel.selectedOptionIndex.collectAsState()
    val selectedTokens by viewModel.selectedTokenSequence.collectAsState()
    val isChecked by viewModel.isAnswerChecked.collectAsState()
    val isCorrect by viewModel.isAnswerCorrect.collectAsState()
    val isCompleted by viewModel.isLessonCompleted.collectAsState()
    val stats by viewModel.userStats.collectAsState()
    var showHelp by remember(activeLesson?.id) { mutableStateOf(false) }

    val lesson = activeLesson ?: return

    if (isCompleted) {
        LessonCompletedTriumphView(xpReward = lesson.xpReward, onFinish = { viewModel.navigateTo(AppScreen.LEARNING_PATH) })
        return
    }

    val exercise = lesson.exercises.getOrNull(currentIndex) ?: return
    val uriHandler = LocalUriHandler.current
    val progress = (currentIndex + 1).toFloat() / lesson.exercises.size

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateTo(AppScreen.LEARNING_PATH) }) {
                    Icon(Icons.Default.Close, contentDescription = "Fermer")
                }
                Spacer(Modifier.width(8.dp))
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.weight(1f).height(12.dp).clip(RoundedCornerShape(6.dp)),
                    color = BrandRed,
                    backgroundColor = Concrete
                )
                Spacer(Modifier.width(16.dp))
                Text("❤️ ${stats.hearts}", fontWeight = FontWeight.Bold, color = ActionRed, fontSize = 16.sp)
                IconButton(onClick = { showHelp = true }) {
                    Icon(Icons.Default.HelpOutline, contentDescription = "Aide pour cette leçon", tint = Ink)
                }
            }
        },
        bottomBar = {
            Surface(
                elevation = 12.dp,
                color = if (isChecked) (if (isCorrect) Paper else Concrete) else Color.White
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    if (isChecked) {
                        Text(
                            text = if (isCorrect) "Excellent !" else "Correction :",
                            fontWeight = FontWeight.Bold,
                            color = if (isCorrect) Ink else ActionRed,
                            fontSize = 18.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = exercise.explanation,
                            color = Graphite,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.nextExercise() },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = if (isCorrect) BrandRed else ActionRed),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("CONTINUER", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.checkAnswer() },
                            enabled = selectedOption != null || selectedTokens.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = BrandRed),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("VÉRIFIER", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = exercise.prompt,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            if (lesson.objective.isNotBlank()) {
                Text(
                    text = "Objectif : ${lesson.objective}",
                    fontSize = 14.sp,
                    color = Graphite,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            when (exercise) {
                is Exercise.CodeChallenge -> {
                    // Code snippet block
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        backgroundColor = Ink,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = exercise.codeSnippet,
                            color = White,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    // Options
                    OptionsList(options = exercise.options, selectedIndex = selectedOption, isChecked = isChecked, onSelect = { viewModel.selectOption(it) })
                }

                is Exercise.MathChallenge -> {
                    exercise.formula?.let { formula ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            backgroundColor = Paper,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Formule : $formula",
                                color = Ink,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    OptionsList(options = exercise.options, selectedIndex = selectedOption, isChecked = isChecked, onSelect = { viewModel.selectOption(it) })
                }

                is Exercise.Translation -> {
                    Text(
                        text = "\"${exercise.sourceSentence}\"",
                        fontSize = 16.sp,
                        color = Graphite,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Assembled Token Area
                    Card(
                        modifier = Modifier.fillMaxWidth().height(100.dp).padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        backgroundColor = Color.White,
                        elevation = 2.dp
                    ) {
                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            selectedTokens.forEach { token ->
                                TokenChip(token = token, selected = true, onClick = { viewModel.toggleTokenSelection(token) })
                            }
                        }
                    }

                    // Available Token Pool
                    Text("Sélectionnez les mots :", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(exercise.targetTokens) { token ->
                            val isUsed = selectedTokens.contains(token)
                            if (!isUsed) {
                                TokenChip(token = token, selected = false, onClick = { viewModel.toggleTokenSelection(token) })
                            }
                        }
                    }
                }

                is Exercise.MultipleChoice -> {
                    OptionsList(options = exercise.options, selectedIndex = selectedOption, isChecked = isChecked, onSelect = { viewModel.selectOption(it) })
                }
                else -> {}
            }
        }
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text("💡 Coup de pouce", fontWeight = FontWeight.Bold, color = ActionRed) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("À retenir", fontWeight = FontWeight.Bold)
                    Text(lesson.learningNotes.ifBlank { lesson.objective.ifBlank { "Relis l’énoncé et cherche la règle étudiée dans cette leçon." } })
                    Divider()
                    Text("Indice pour cette question", fontWeight = FontWeight.Bold)
                    Text(exercise.hint)
                    if (lesson.sourceUrls.isNotEmpty()) {
                        Divider()
                        Text("Ressources du cours", fontWeight = FontWeight.Bold)
                        lesson.sourceUrls.forEachIndexed { index, url ->
                            TextButton(onClick = { runCatching { uriHandler.openUri(url) } }) {
                                Text(if (index == 0) "Ouvrir la ressource de référence" else "Ouvrir une ressource complémentaire")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelp = false }) { Text("Compris") }
            },
            shape = RoundedCornerShape(20.dp),
            backgroundColor = Color.White
        )
    }
}

@Composable
fun OptionsList(options: List<String>, selectedIndex: Int?, isChecked: Boolean, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        options.forEachIndexed { index, option ->
            val isSelected = selectedIndex == index
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = 2.dp,
                        color = if (isSelected) Ink else Concrete,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable(enabled = !isChecked) { onSelect(index) },
                backgroundColor = if (isSelected) Paper else Color.White
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) Ink else Paper),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${index + 1}", color = if (isSelected) Color.White else Graphite, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(option, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun TokenChip(token: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = if (selected) Ink else Paper,
        elevation = 2.dp
    ) {
        Text(
            text = token,
            color = if (selected) Color.White else Ink,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun LessonCompletedTriumphView(xpReward: Int, onFinish: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Paper).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🎉", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text("Leçon Terminée !", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = BrandRed)
        Spacer(Modifier.height(8.dp))
        Text("Vous avez gagné +$xpReward XP pour vos révisions.", fontSize = 16.sp, color = Graphite)

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = BrandRed),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("CONTINUER VOS REVISIONS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
