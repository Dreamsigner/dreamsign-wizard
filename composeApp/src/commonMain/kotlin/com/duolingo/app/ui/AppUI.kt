package com.duolingo.app.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duolingo.app.ai.IngestionSourceType
import com.duolingo.app.model.*
import com.duolingo.app.viewmodel.AppScreen
import com.duolingo.app.viewmodel.AppViewModel
import kotlinx.coroutines.launch

val DuoGreen = Color(0xFF58CC02)
val DuoDarkGreen = Color(0xFF46A302)
val DuoRed = Color(0xFFFF4B4B)
val DuoBlue = Color(0xFF1CB0F6)
val DuoYellow = Color(0xFFFFC800)
val DuoPurple = Color(0xFFCE82FF)
val DuoBgGray = Color(0xFFF7F7F7)

@Composable
fun DuolingoApp(viewModel: AppViewModel = remember { AppViewModel() }) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val stats by viewModel.userStats.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val selectedSubjectId by viewModel.selectedSubjectId.collectAsState()

    val currentSubject = subjects.find { it.id == selectedSubjectId } ?: subjects.first()

    Scaffold(
        topBar = {
            if (currentScreen != AppScreen.LESSON_QUIZ) {
                TopStatsHeader(stats = stats, subjects = subjects, selectedSubject = currentSubject, onSelectSubject = { viewModel.selectSubject(it) })
            }
        },
        bottomBar = {
            if (currentScreen != AppScreen.LESSON_QUIZ) {
                BottomNavigationBar(currentScreen = currentScreen, onNavigate = { viewModel.navigateTo(it) })
            }
        },
        backgroundColor = DuoBgGray
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (currentScreen) {
                AppScreen.LEARNING_PATH -> LearningPathScreen(
                    subject = currentSubject,
                    completedLessonIds = stats.completedLessonIds,
                    onStartLesson = { lesson -> viewModel.startLesson(lesson) }
                )
                AppScreen.LESSON_QUIZ -> LessonQuizScreen(viewModel = viewModel)
                AppScreen.AI_IMPORTER -> AiImporterScreen(viewModel = viewModel)
                AppScreen.PROFILE -> ProfileScreen(stats = stats)
            }
        }
    }
}

@Composable
fun TopStatsHeader(
    stats: UserStats,
    subjects: List<Subject>,
    selectedSubject: Subject,
    onSelectSubject: (String) -> Unit
) {
    var dropdownExpanded by remember { mutableStateOf(false) }

    Surface(
        color = Color.White,
        elevation = 3.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Subject Dropdown Selector
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF0F0F0))
                        .clickable { dropdownExpanded = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(selectedSubject.icon, fontSize = 20.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(selectedSubject.title.take(15) + "...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }

                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    subjects.forEach { subj ->
                        DropdownMenuItem(onClick = {
                            onSelectSubject(subj.id)
                            dropdownExpanded = false
                        }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(subj.icon, fontSize = 18.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(subj.title, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            // Stats Badges (Streak, XP, Hearts)
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Streak
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 12.dp)) {
                    Text("🔥", fontSize = 18.sp)
                    Spacer(Modifier.width(4.dp))
                    Text("${stats.streakDays}", fontWeight = FontWeight.Bold, color = DuoYellow, fontSize = 16.sp)
                }

                // XP
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 12.dp)) {
                    Text("⚡", fontSize = 18.sp)
                    Spacer(Modifier.width(4.dp))
                    Text("${stats.xp}", fontWeight = FontWeight.Bold, color = DuoBlue, fontSize = 16.sp)
                }

                // Hearts
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("❤️", fontSize = 18.sp)
                    Spacer(Modifier.width(4.dp))
                    Text("${stats.hearts}", fontWeight = FontWeight.Bold, color = DuoRed, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(currentScreen: AppScreen, onNavigate: (AppScreen) -> Unit) {
    BottomNavigation(
        backgroundColor = Color.White,
        elevation = 8.dp
    ) {
        BottomNavigationItem(
            selected = currentScreen == AppScreen.LEARNING_PATH,
            onClick = { onNavigate(AppScreen.LEARNING_PATH) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Leçons") },
            label = { Text("Leçons") },
            selectedContentColor = DuoGreen,
            unselectedContentColor = Color.Gray
        )
        BottomNavigationItem(
            selected = currentScreen == AppScreen.AI_IMPORTER,
            onClick = { onNavigate(AppScreen.AI_IMPORTER) },
            icon = { Icon(Icons.Default.AddCircle, contentDescription = "Import de démonstration") },
            label = { Text("Import démo") },
            selectedContentColor = DuoPurple,
            unselectedContentColor = Color.Gray
        )
        BottomNavigationItem(
            selected = currentScreen == AppScreen.PROFILE,
            onClick = { onNavigate(AppScreen.PROFILE) },
            icon = { Icon(Icons.Default.Person, contentDescription = "Profil") },
            label = { Text("Profil") },
            selectedContentColor = DuoBlue,
            unselectedContentColor = Color.Gray
        )
    }
}

@Composable
fun LearningPathScreen(
    subject: Subject,
    completedLessonIds: Set<String>,
    onStartLesson: (Lesson) -> Unit
) {
    val lockedLessonIds = buildSet {
        var previousLessonCompleted = true
        subject.units.flatMap { it.lessons }.forEach { lesson ->
            val completed = completedLessonIds.contains(lesson.id) || lesson.isCompleted
            if (lesson.isLocked && !completed && !previousLessonCompleted) add(lesson.id)
            previousLessonCompleted = completed
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                shape = RoundedCornerShape(16.dp),
                backgroundColor = DuoGreen,
                elevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "${subject.icon} ${subject.title}",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = subject.description,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp
                    )
                }
            }
        }

        subject.units.forEach { unit ->
            item {
                Text(
                    text = "UNITÉ ${unit.unitNumber}: ${unit.title.uppercase()}",
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }

            items(unit.lessons) { lesson ->
                val isCompleted = completedLessonIds.contains(lesson.id) || lesson.isCompleted
                val lessonForDisplay = lesson.copy(isLocked = lesson.id in lockedLessonIds)
                LessonNodeItem(lesson = lessonForDisplay, isCompleted = isCompleted, onClick = { onStartLesson(lessonForDisplay) })
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun LessonNodeItem(lesson: Lesson, isCompleted: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(2.dp, if (isCompleted) DuoGreen else if (!lesson.isLocked) DuoBlue else Color(0xFFE0E0E0), RoundedCornerShape(16.dp))
            .clickable(enabled = !lesson.isLocked) { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (isCompleted) DuoGreen else if (!lesson.isLocked) DuoBlue else Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
            } else if (!lesson.isLocked) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
            } else {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White)
            }
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(lesson.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(4.dp))
            Text("${lesson.exercises.size} exercices • +${lesson.xpReward} XP", fontSize = 13.sp, color = Color.Gray)
        }

        if (!lesson.isLocked) {
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(backgroundColor = if (isCompleted) DuoGreen else DuoBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isCompleted) "Revoir" else "COMMENCER", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
