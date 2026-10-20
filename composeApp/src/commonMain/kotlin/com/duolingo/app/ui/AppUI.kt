package com.duolingo.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duolingo.app.model.*
import com.duolingo.app.viewmodel.AppScreen
import com.duolingo.app.viewmodel.AppViewModel

val BrandRed = Color(0xFFE3262E)
val ActionRed = Color(0xFFC91E28)
val Ink = Color(0xFF141414)
val Graphite = Color(0xFF4A4A4A)
val Paper = Color(0xFFF7F5F0)
val Concrete = Color(0xFFD9D7D2)
val White = Color(0xFFFFFFFF)
val LearningShape = RoundedCornerShape(16.dp)

/** Keep the source order visible without renaming stable published lesson IDs. */
internal val Lesson.sourceSessionLabel: String?
    get() {
        val number = sourceSessionNumber ?: return null
        val kind = when (sessionKind) {
            "exam" -> "Entraînement à l’examen"
            "review" -> "Révision"
            "practice" -> "Atelier original"
            else -> "Leçon"
        }
        return "$kind · $number"
    }

/** Material controls supply keyboard activation; this adds an explicit focus outline. */
fun Modifier.learningFocus(source: MutableInteractionSource): Modifier = composed {
    val focused by source.collectIsFocusedAsState()
    border(if (focused) 3.dp else 0.dp, if (focused) Ink else Color.Transparent, LearningShape)
}

@Composable
fun LearningButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val interaction = remember { MutableInteractionSource() }
    Button(onClick, modifier.heightIn(min = 48.dp).learningFocus(interaction), enabled = enabled,
        interactionSource = interaction, shape = LearningShape,
        colors = ButtonDefaults.buttonColors(backgroundColor = ActionRed, contentColor = White),
        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp)) { Text(text, fontWeight = FontWeight.Bold) }
}

@Composable
fun DuolingoApp(viewModel: AppViewModel = remember { AppViewModel() }) {
    DisposableEffect(viewModel) { onDispose { viewModel.close() } }
    val screen by viewModel.currentScreen.collectAsState()
    val stats by viewModel.userStats.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val selectedId by viewModel.selectedSubjectId.collectAsState()
    val persistence by viewModel.persistenceStatus.collectAsState()
    val subject = subjects.find { it.id == selectedId } ?: subjects.firstOrNull()
    val selectedUnitId = subject?.let { current ->
        stats.selectedCourseIds[current.id]?.takeIf { selected -> current.units.any { it.id == selected } }
            ?: current.units.firstOrNull { course -> course.lessons.any { lesson ->
                lesson.id !in stats.completedLessonIds && !lesson.isCompleted && current.isLessonUnlocked(lesson, stats.completedLessonIds)
            } }?.id ?: current.units.firstOrNull()?.id
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(Paper).windowInsetsPadding(WindowInsets.safeDrawing)) {
        val sidebar = maxWidth >= 1000.dp
        val rail = maxWidth >= 700.dp
        if (screen == AppScreen.LESSON_QUIZ) {
            LessonQuizScreen(viewModel)
        } else {
            Row(Modifier.fillMaxSize()) {
                if (rail) AppNavigation(screen, stats, sidebar, viewModel::navigateTo)
                Scaffold(backgroundColor = Paper, modifier = Modifier.weight(1f),
                    topBar = { if (subject != null) TopStatsHeader(stats, subjects, subject, viewModel::selectSubject) },
                    bottomBar = { if (!rail) BottomNavigationBar(screen, viewModel::navigateTo) }) { padding ->
                    Box(Modifier.padding(padding).fillMaxSize()) {
                        when (screen) {
                            AppScreen.LEARNING_PATH -> if (subject != null) LearningPathScreen(subject, stats.completedLessonIds, viewModel::startLesson, selectedUnitId) { viewModel.selectCourse(subject.id, it) }
                                else EmptyLearningState("Aucun cours disponible", "Les cours apparaîtront ici dès qu’ils seront disponibles.")
                            AppScreen.PROFILE -> ProfileScreen(stats, persistence)
                            AppScreen.LESSON_QUIZ -> Unit
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppNavigation(screen: AppScreen, stats: UserStats, expanded: Boolean, navigate: (AppScreen) -> Unit) {
    Column(Modifier.width(if (expanded) 224.dp else 104.dp).fillMaxHeight().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Default.Star, null, tint = BrandRed, modifier = Modifier.size(40.dp))
        Text(if (expanded) "Atelier\nd’apprentissage" else "Atelier", fontWeight = FontWeight.ExtraBold, fontSize = if (expanded) 20.sp else 16.sp)
        Spacer(Modifier.height(20.dp))
        NavigationChoice("Cours", Icons.Default.Home, screen == AppScreen.LEARNING_PATH, expanded) { navigate(AppScreen.LEARNING_PATH) }
        NavigationChoice("Profil", Icons.Default.Person, screen == AppScreen.PROFILE, expanded) { navigate(AppScreen.PROFILE) }
        Spacer(Modifier.height(32.dp))
        if (expanded) {
            Text("À petits pas,\nchaque jour.", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("${stats.completedLessonIds.size} leçons terminées", color = Graphite)
            Text("${stats.xp} XP accumulés", color = Graphite)
        }
    }
}

@Composable
private fun NavigationChoice(label: String, icon: ImageVector, selected: Boolean, expanded: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    TextButton(onClick, Modifier.fillMaxWidth().heightIn(min = 56.dp).learningFocus(interaction).semantics { this.selected = selected },
        interactionSource = interaction, shape = LearningShape,
        colors = ButtonDefaults.textButtonColors(backgroundColor = if (selected) Concrete else Color.Transparent, contentColor = Ink)) {
        if (expanded) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null); Spacer(Modifier.width(12.dp)); Text(label, fontWeight = FontWeight.Bold)
        } else Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(icon, null); Text(label, fontSize = 12.sp) }
    }
}

@Composable
fun TopStatsHeader(stats: UserStats, subjects: List<Subject>, selectedSubject: Subject, onSelectSubject: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    BoxWithConstraints(Modifier.fillMaxWidth().background(White).padding(horizontal = 16.dp, vertical = 8.dp)) {
        val compact = maxWidth < 480.dp
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    TextButton({ expanded = true }, Modifier.fillMaxWidth().heightIn(min = 48.dp), colors = ButtonDefaults.textButtonColors(contentColor = Ink)) {
                        Text(selectedSubject.title, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.ArrowDropDown, "Choisir une matière")
                    }
                    DropdownMenu(expanded, { expanded = false }, Modifier.widthIn(max = 360.dp)) {
                        subjects.forEach { subject -> DropdownMenuItem({ onSelectSubject(subject.id); expanded = false }, Modifier.heightIn(min = 48.dp)) { Text(subject.title) } }
                    }
                }
                if (!compact) { Spacer(Modifier.width(24.dp)); Text("${stats.xp} XP  ·  ${stats.hearts}/${stats.maxHearts} vies", fontWeight = FontWeight.Bold) }
            }
            if (compact) Text("${stats.xp} XP  ·  ${stats.hearts}/${stats.maxHearts} vies", color = Graphite, modifier = Modifier.padding(start = 16.dp, bottom = 4.dp))
        }
    }
}

@Composable
fun BottomNavigationBar(currentScreen: AppScreen, onNavigate: (AppScreen) -> Unit) {
    BottomNavigation(backgroundColor = White, elevation = 0.dp) {
        BottomNavigationItem(currentScreen == AppScreen.LEARNING_PATH, { onNavigate(AppScreen.LEARNING_PATH) },
            icon = { Icon(Icons.Default.Home, null) }, label = { Text("Cours") }, selectedContentColor = ActionRed, unselectedContentColor = Graphite)
        BottomNavigationItem(currentScreen == AppScreen.PROFILE, { onNavigate(AppScreen.PROFILE) },
            icon = { Icon(Icons.Default.Person, null) }, label = { Text("Profil") }, selectedContentColor = ActionRed, unselectedContentColor = Graphite)
    }
}

@Composable
fun LearningPathScreen(subject: Subject, completedLessonIds: Set<String>, onStartLesson: (Lesson) -> Unit, selectedUnitId: String?, onSelectUnit: (String) -> Unit) {
    val lessons = subject.units.flatMap { it.lessons }
    val completed = completedLessonIds + lessons.filter { it.isCompleted }.map { it.id }
    val lockedIds = lessons.filterNot { subject.isLessonUnlocked(it, completed) }.map { it.id }.toSet()
    val unit = subject.units.find { it.id == selectedUnitId } ?: subject.units.firstOrNull()
    val courseListState = key(subject.id) { rememberLazyListState() }
    val lessonListState = key(subject.id, unit?.id) { rememberLazyListState() }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val split = maxWidth >= 880.dp
        val gutter = if (maxWidth >= 600.dp) 24.dp else 16.dp
        // Desktop: 260 dp course column + 24 dp gutter + flexible lesson workspace.
        Row(Modifier.fillMaxSize().padding(gutter), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            if (split) LazyColumn(Modifier.width(260.dp).fillMaxHeight(), state = courseListState, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { Text("Mes cours", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.semantics { heading() }) }
                item { Text(subject.description, color = Graphite) }
                items(subject.units, key = { it.id }) { course -> CourseChoice(course, course.id == unit?.id, completed) { onSelectUnit(course.id) } }
            }
            LazyColumn(Modifier.weight(1f).fillMaxHeight(), state = lessonListState, verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                if (!split) {
                    item { Text("Mes cours", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.semantics { heading() }) }
                    item { CoursePicker(subject.units, unit, onSelectUnit) }
                }
                if (unit == null) item { EmptyLearningState("Aucune leçon", "Ce cours ne contient pas encore de leçon.") }
                else {
                    item { CourseOverview(unit, completed) }
                    val next = unit.lessons.firstOrNull { it.id !in completed && it.id !in lockedIds }
                    if (next != null) item { LearningButton("Continuer · ${next.title}", { onStartLesson(next.copy(isLocked = false)) }, Modifier.fillMaxWidth()) }
                    item { Text("Les leçons", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() }) }
                    items(unit.lessons, key = { it.id }) { lesson ->
                        val display = lesson.copy(isLocked = lesson.id in lockedIds)
                        LessonNodeItem(display, lesson.id in completed) { onStartLesson(display) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseChoice(unit: LearningUnit, selected: Boolean, completed: Set<String>, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    OutlinedButton(onClick, Modifier.fillMaxWidth().learningFocus(source).semantics { this.selected = selected }, interactionSource = source,
        shape = LearningShape, border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Ink else Concrete),
        colors = ButtonDefaults.outlinedButtonColors(backgroundColor = if (selected) White else Paper, contentColor = Ink), contentPadding = PaddingValues(16.dp)) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (unit.courseCode.isNotBlank()) Text(unit.courseCode, color = ActionRed, fontWeight = FontWeight.Bold)
            Text(unit.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("${unit.lessons.count { it.id in completed }} / ${unit.lessons.size} leçons", color = Graphite)
        }
    }
}

@Composable
private fun CoursePicker(units: List<LearningUnit>, selected: LearningUnit?, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton({ expanded = true }, Modifier.fillMaxWidth().heightIn(min = 56.dp), shape = LearningShape) {
            Text(selected?.title ?: "Choisir un cours", Modifier.weight(1f)); Icon(Icons.Default.ArrowDropDown, null)
        }
        DropdownMenu(expanded, { expanded = false }) {
            units.forEach { unit -> DropdownMenuItem({ onSelect(unit.id); expanded = false }, Modifier.heightIn(min = 48.dp)) { Text(unit.title) } }
        }
    }
}

@Composable
private fun CourseOverview(unit: LearningUnit, completed: Set<String>) {
    val count = unit.lessons.count { it.id in completed }
    Surface(shape = LearningShape, color = White) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (unit.courseCode.isNotBlank() || unit.edition.isNotBlank()) Text(listOf(unit.courseCode, unit.edition).filter { it.isNotBlank() }.joinToString(" · "), color = Graphite)
            Text(unit.title, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            if (unit.description.isNotBlank()) Text(unit.description, color = Graphite, fontSize = 16.sp)
            Text("$count / ${unit.lessons.size} leçons terminées", fontWeight = FontWeight.Bold)
            LinearProgressIndicator(if (unit.lessons.isEmpty()) 0f else count.toFloat() / unit.lessons.size, Modifier.fillMaxWidth().height(8.dp), color = BrandRed, backgroundColor = Concrete)
            val expected = unit.expectedLessonCount
            if (expected != null && expected != unit.lessons.size) Text("${unit.lessons.size} leçons disponibles sur $expected prévues dans la source.", color = Graphite)
        }
    }
}

@Composable
fun LessonNodeItem(lesson: Lesson, isCompleted: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val state = when { isCompleted -> "Terminée · Réviser"; lesson.isLocked -> "Verrouillée · Terminer les prérequis"; else -> "Disponible · Commencer" }
    OutlinedButton(onClick, Modifier.fillMaxWidth().heightIn(min = 88.dp).learningFocus(interaction).semantics { stateDescription = state },
        enabled = !lesson.isLocked, interactionSource = interaction, shape = LearningShape,
        border = BorderStroke(1.dp, if (isCompleted) BrandRed else Concrete),
        colors = ButtonDefaults.outlinedButtonColors(backgroundColor = White, contentColor = Ink, disabledContentColor = Graphite), contentPadding = PaddingValues(16.dp)) {
        Icon(when { isCompleted -> Icons.Default.CheckCircle; lesson.isLocked -> Icons.Default.Lock; else -> Icons.Default.PlayArrow }, null,
            tint = if (isCompleted) ActionRed else Graphite, modifier = Modifier.size(32.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            lesson.sourceSessionLabel?.let { Text(it, fontSize = 13.sp, color = Graphite) }
            Text(lesson.title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("${lesson.exercises.size} exercices · ${lesson.xpReward} XP à la première réussite", fontSize = 14.sp, color = Graphite)
            Text(state, fontSize = 14.sp, color = if (lesson.isLocked) Graphite else ActionRed)
        }
    }
}

@Composable
fun EmptyLearningState(title: String, message: String) {
    Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold); Text(message, color = Graphite)
    }
}
