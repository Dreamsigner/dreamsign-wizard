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
import com.duolingo.app.model.*
import com.duolingo.app.viewmodel.AppScreen
import com.duolingo.app.viewmodel.AppViewModel
import kotlinx.coroutines.launch

val BrandRed = Color(0xFFE3262E)
val ActionRed = Color(0xFFC91E28)
val Ink = Color(0xFF141414)
val Graphite = Color(0xFF4A4A4A)
val Paper = Color(0xFFF7F5F0)
val Concrete = Color(0xFFD9D7D2)
val White = Color(0xFFFFFFFF)

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
        backgroundColor = Paper
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (currentScreen) {
                AppScreen.LEARNING_PATH -> LearningPathScreen(
                    subject = currentSubject,
                    completedLessonIds = stats.completedLessonIds,
                    onStartLesson = { lesson -> viewModel.startLesson(lesson) }
                )
                AppScreen.LESSON_QUIZ -> LessonQuizScreen(viewModel = viewModel)
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
                        .background(Paper)
                        .clickable { dropdownExpanded = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(selectedSubject.icon, fontSize = 20.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        selectedSubject.title.let { if (it.length > 20) it.take(19) + "…" else it },
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1
                    )
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
                    Text("${stats.streakDays}", fontWeight = FontWeight.Bold, color = Graphite, fontSize = 16.sp)
                }

                // XP
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 12.dp)) {
                    Text("⚡", fontSize = 18.sp)
                    Spacer(Modifier.width(4.dp))
                    Text("${stats.xp}", fontWeight = FontWeight.Bold, color = Ink, fontSize = 16.sp)
                }

                // Hearts
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("❤️", fontSize = 18.sp)
                    Spacer(Modifier.width(4.dp))
                    Text("${stats.hearts}", fontWeight = FontWeight.Bold, color = ActionRed, fontSize = 16.sp)
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
            selectedContentColor = BrandRed,
            unselectedContentColor = Graphite
        )
        BottomNavigationItem(
            selected = currentScreen == AppScreen.PROFILE,
            onClick = { onNavigate(AppScreen.PROFILE) },
            icon = { Icon(Icons.Default.Person, contentDescription = "Profil") },
            label = { Text("Profil") },
            selectedContentColor = Ink,
            unselectedContentColor = Graphite
        )
    }
}

@Composable
fun LearningPathScreen(
    subject: Subject,
    completedLessonIds: Set<String>,
    onStartLesson: (Lesson) -> Unit
) {
    val lessonsInPath = subject.units.flatMap { it.lessons }
    val completedIds = completedLessonIds + lessonsInPath.filter { it.isCompleted }.map { it.id }
    val lockedLessonIds = lessonsInPath.mapIndexedNotNull { index, lesson ->
        val completed = lesson.id in completedIds
        val prerequisites = lesson.prerequisiteLessonIds.ifEmpty {
            if (lesson.requiresPreviousLesson && index > 0) listOf(lessonsInPath[index - 1].id) else emptyList()
        }
        if (!completed && prerequisites.any { it !in completedIds }) lesson.id else null
    }.toSet()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                shape = RoundedCornerShape(16.dp),
                backgroundColor = BrandRed,
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
                    color = Graphite,
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
            .border(2.dp, if (isCompleted) BrandRed else if (!lesson.isLocked) Ink else Concrete, RoundedCornerShape(16.dp))
            .clickable(enabled = !lesson.isLocked) { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (isCompleted) BrandRed else if (!lesson.isLocked) Ink else Concrete),
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
            Text("${lesson.exercises.size} exercices • +${lesson.xpReward} XP", fontSize = 13.sp, color = Graphite)
        }

        if (!lesson.isLocked) {
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(backgroundColor = if (isCompleted) BrandRed else Ink),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isCompleted) "Revoir" else "COMMENCER", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
