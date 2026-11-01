package com.dreamsigner.wizard.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dreamsigner.wizard.model.*
import com.dreamsigner.wizard.viewmodel.AppScreen
import com.dreamsigner.wizard.viewmodel.AppViewModel
import com.dreamsigner.wizard.ui.studio.StudioScreen

val BrandRed = Color(0xFFE3262E)
val ActionRed = Color(0xFFC91E28)
val Ink = Color(0xFF141414)
val Graphite = Color(0xFF4A4A4A)
val Paper = Color(0xFFF7F5F0)
val Concrete = Color(0xFFD9D7D2)
val White = Color(0xFFFFFFFF)
val LearningShape = RoundedCornerShape(16.dp)
private const val SCREEN_FADE_MS = 200
private const val INTERACTION_MOTION_MS = 140
private const val COURSE_FADE_MS = 180
private const val SEARCH_FADE_MS = 180
private const val PROGRESS_MOTION_MS = 400

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

/** Material controls supply keyboard activation; only keyboard navigation needs a visible focus ring. */
fun Modifier.learningFocus(source: MutableInteractionSource, focusColor: Color = Ink): Modifier = composed {
    val focused by source.collectIsFocusedAsState()
    val keyboardMode = LocalInputModeManager.current.inputMode == InputMode.Keyboard
    if (focused && keyboardMode) border(2.dp, focusColor, LearningShape) else this
}

@Composable
private fun MutableInteractionSource.hoveredAndPressed(enabled: Boolean = true): Pair<Boolean, Boolean> {
    val hovered by collectIsHoveredAsState()
    val pressed by collectIsPressedAsState()
    return (enabled && hovered) to (enabled && pressed)
}

@Composable
fun LearningButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, focusColor: Color = Ink) {
    val interaction = remember { MutableInteractionSource() }
    val (hovered, pressed) = interaction.hoveredAndPressed(enabled)
    val background by animateColorAsState(if (enabled && hovered) BrandRed else ActionRed, tween(INTERACTION_MOTION_MS, easing = FastOutSlowInEasing))
    val scale by animateFloatAsState(if (pressed) 0.985f else 1f, tween(INTERACTION_MOTION_MS, easing = FastOutSlowInEasing))
    Button(onClick, modifier.heightIn(min = 48.dp).graphicsLayer { scaleX = scale; scaleY = scale }
        .learningFocus(interaction, focusColor), enabled = enabled,
        interactionSource = interaction, shape = LearningShape,
        colors = ButtonDefaults.buttonColors(backgroundColor = background, contentColor = White),
        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp)) {
        Text(text, style = MaterialTheme.typography.button.copy(letterSpacing = 0.sp, fontWeight = FontWeight.SemiBold))
    }
}

@Composable
fun DreamsignWizardApp(
    viewModel: AppViewModel = remember { AppViewModel(initialScreen = AppScreen.AI_STUDIO) },
    settingsActionCount: Int = 0,
    settingsContent: @Composable () -> Unit = {},
) {
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
        val rail = maxWidth >= 700.dp
        var navigationExpanded by remember { mutableStateOf(false) }
        if (screen == AppScreen.COURSE_READER && subject != null && selectedUnitId != null) {
            val unit = subject.units.first { it.id == selectedUnitId }
            CourseReaderScreen(unit, { subject.isLessonUnlocked(it, stats.completedLessonIds) }, viewModel::startLesson,
                onClose = { viewModel.navigateTo(AppScreen.LEARNING_PATH) }, searchableContents = subject.category == SubjectCategory.MATHEMATICS, showVideoPreviews = subject.category == SubjectCategory.MATHEMATICS, typesetMath = subject.category == SubjectCategory.MATHEMATICS || subject.category == SubjectCategory.COMPUTER_SCIENCE,
                favoriteLessonIds = stats.favoriteLessonIds,
                onToggleFavorite = if (subject.category == SubjectCategory.MATHEMATICS) viewModel::toggleMathLessonFavorite else null,
                favoriteSaveStatus = persistence)
        } else if (screen == AppScreen.LESSON_QUIZ) {
            LessonQuizScreen(viewModel)
        } else {
            Row(Modifier.fillMaxSize()) {
                if (rail) AppNavigation(screen, navigationExpanded, settingsActionCount,
                    onExpandedChange = { navigationExpanded = it },
                    navigate = { destination -> navigationExpanded = false; viewModel.navigateTo(destination) })
                Scaffold(backgroundColor = Paper, modifier = Modifier.weight(1f),
                    topBar = { if (subject != null && screen != AppScreen.SETTINGS && screen != AppScreen.AI_STUDIO) TopStatsHeader(stats, subjects, subject, viewModel::selectSubject) },
                    bottomBar = { if (!rail) BottomNavigationBar(screen, viewModel::navigateTo, settingsActionCount) }) { padding ->
                    Box(Modifier.padding(padding).fillMaxSize()) {
                        AnimatedContent(targetState = screen, modifier = Modifier.fillMaxSize(), transitionSpec = {
                            // Fade through the paper surface so outgoing and incoming text never overlap.
                            (fadeIn(tween(SCREEN_FADE_MS - 80, delayMillis = 80, easing = FastOutSlowInEasing)) togetherWith
                                fadeOut(tween(80, easing = FastOutSlowInEasing))).using(null)
                        }) { targetScreen -> when (targetScreen) {
                            AppScreen.LEARNING_PATH -> if (subject != null) LearningPathScreen(subject, stats.completedLessonIds, viewModel::startLesson, selectedUnitId,
                                onOpenCourse = { viewModel.navigateTo(AppScreen.COURSE_READER) }) { viewModel.selectCourse(subject.id, it) }
                                else EmptyLearningState("Aucun cours disponible", "Les cours apparaîtront ici dès qu’ils seront disponibles.")
                            AppScreen.PROFILE -> ProfileScreen(stats, persistence)
                            AppScreen.SETTINGS -> SettingsScreen(persistence, settingsContent)
                            AppScreen.AI_STUDIO -> StudioScreen()
                            AppScreen.LESSON_QUIZ, AppScreen.COURSE_READER -> Unit
                        } }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppNavigation(screen: AppScreen, expanded: Boolean, settingsActionCount: Int, onExpandedChange: (Boolean) -> Unit, navigate: (AppScreen) -> Unit) {
    Column(Modifier.width(if (expanded) 224.dp else 72.dp).fillMaxHeight().verticalScroll(rememberScrollState())
        .padding(horizontal = if (expanded) 16.dp else 8.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val interaction = remember { MutableInteractionSource() }
        TextButton(onClick = { onExpandedChange(!expanded) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).learningFocus(interaction).semantics {
                contentDescription = if (expanded) "Fermer le menu de navigation" else "Ouvrir le menu de navigation"
            }, interactionSource = interaction, shape = LearningShape,
            contentPadding = PaddingValues(0.dp), colors = ButtonDefaults.textButtonColors(contentColor = Ink)) {
            if (expanded) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                PixelIcon(LearningSymbol.BOOK, Modifier.size(32.dp))
                Spacer(Modifier.width(12.dp))
                Text("Atelier\nd’apprentissage", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, lineHeight = 20.sp)
            } else PixelIcon(LearningSymbol.BOOK, Modifier.size(32.dp))
        }
        Spacer(Modifier.height(12.dp))
        NavigationChoice("Cours", Icons.Default.Home, screen == AppScreen.LEARNING_PATH, expanded) { navigate(AppScreen.LEARNING_PATH) }
        NavigationChoice("Studio IA", Icons.Default.Edit, screen == AppScreen.AI_STUDIO, expanded) { navigate(AppScreen.AI_STUDIO) }
        NavigationChoice("Profil", Icons.Default.Person, screen == AppScreen.PROFILE, expanded) { navigate(AppScreen.PROFILE) }
        NavigationChoice("Paramètres", Icons.Default.Settings, screen == AppScreen.SETTINGS, expanded, settingsActionCount) { navigate(AppScreen.SETTINGS) }
    }
}

@Composable
private fun NavigationChoice(label: String, icon: ImageVector, selected: Boolean, expanded: Boolean, actionCount: Int = 0, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val (hovered, pressed) = interaction.hoveredAndPressed()
    val background by animateColorAsState(when { selected -> Concrete; hovered -> Concrete.copy(alpha = 0.65f); else -> Color.Transparent },
        tween(INTERACTION_MOTION_MS, easing = FastOutSlowInEasing))
    val scale by animateFloatAsState(if (pressed) 0.985f else 1f, tween(INTERACTION_MOTION_MS, easing = FastOutSlowInEasing))
    TextButton(onClick, Modifier.fillMaxWidth().heightIn(min = 56.dp).graphicsLayer { scaleX = scale; scaleY = scale }
        .learningFocus(interaction).semantics {
        this.selected = selected
        if (!expanded) contentDescription = label
        if (actionCount > 0) stateDescription = settingsActionDescription(actionCount)
    },
        interactionSource = interaction, shape = LearningShape,
        colors = ButtonDefaults.textButtonColors(backgroundColor = background, contentColor = Ink)) {
        if (expanded) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            NavigationIcon(icon, actionCount); Spacer(Modifier.width(12.dp)); Text(label, style = MaterialTheme.typography.button.copy(letterSpacing = 0.sp, fontWeight = FontWeight.SemiBold))
        } else NavigationIcon(icon, actionCount)
    }
}

internal fun settingsActionDescription(count: Int) = if (count == 1) "1 action requise" else "$count actions requises"

@Composable
private fun NavigationIcon(icon: ImageVector, actionCount: Int = 0) {
    Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(24.dp))
        if (actionCount > 0) Surface(
            modifier = Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-4).dp),
            shape = RoundedCornerShape(50), color = ActionRed, contentColor = White,
        ) {
            Box(Modifier.defaultMinSize(minWidth = 18.dp, minHeight = 18.dp).padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
                Text(if (actionCount > 99) "99+" else actionCount.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clearAndSetSemantics { })
            }
        }
    }
}

@Composable
fun TopStatsHeader(stats: UserStats, subjects: List<Subject>, selectedSubject: Subject, onSelectSubject: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    BoxWithConstraints(Modifier.fillMaxWidth().background(White).padding(horizontal = 16.dp, vertical = 8.dp)) {
        val rowLayout = maxWidth >= 920.dp
        Column {
            if (rowLayout) Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).widthIn(max = 420.dp)) {
                    Text("Domaine d’apprentissage", fontSize = 12.sp, color = Graphite, letterSpacing = 0.sp)
                    SubjectPicker(selectedSubject, subjects, { onSelectSubject(it) }, expanded, { expanded = it })
                }
                Spacer(Modifier.width(24.dp))
                StatsStrip(stats, Modifier.widthIn(min = 280.dp, max = 320.dp))
            }
            if (!rowLayout) {
                Text("Domaine d’apprentissage", fontSize = 12.sp, color = Graphite, letterSpacing = 0.sp)
                SubjectPicker(selectedSubject, subjects, { onSelectSubject(it) }, expanded, { expanded = it })
                StatsStrip(stats, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun SubjectPicker(selectedSubject: Subject, subjects: List<Subject>, onSelectSubject: (String) -> Unit, expanded: Boolean, onExpandedChange: (Boolean) -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(Modifier.fillMaxWidth()) {
        TextButton({ onExpandedChange(true) }, Modifier.fillMaxWidth().heightIn(min = 48.dp).border(1.dp, Concrete, LearningShape).learningFocus(interaction),
            interactionSource = interaction, shape = LearningShape, colors = ButtonDefaults.textButtonColors(contentColor = Ink)) {
            Text(selectedSubject.title, Modifier.weight(1f), style = MaterialTheme.typography.button.copy(letterSpacing = 0.sp, fontWeight = FontWeight.SemiBold), maxLines = 1)
            Icon(Icons.Default.ArrowDropDown, "Choisir une matière")
        }
        DropdownMenu(expanded, { onExpandedChange(false) }, Modifier.widthIn(max = 360.dp)) {
            subjects.forEach { subject -> DropdownMenuItem({ onSelectSubject(subject.id); onExpandedChange(false) }, Modifier.heightIn(min = 48.dp)) { Text(subject.title, letterSpacing = 0.sp) } }
        }
    }
}

@Composable
fun BottomNavigationBar(currentScreen: AppScreen, onNavigate: (AppScreen) -> Unit, settingsActionCount: Int = 0) {
    BottomNavigation(backgroundColor = White, elevation = 0.dp) {
        BottomNavigationItem(currentScreen == AppScreen.LEARNING_PATH, { onNavigate(AppScreen.LEARNING_PATH) },
            icon = { Icon(Icons.Default.Home, null) }, label = { Text("Cours") }, selectedContentColor = ActionRed, unselectedContentColor = Graphite)
        BottomNavigationItem(currentScreen == AppScreen.AI_STUDIO, { onNavigate(AppScreen.AI_STUDIO) },
            icon = { Icon(Icons.Default.Edit, null) }, label = { Text("Studio IA") }, selectedContentColor = ActionRed, unselectedContentColor = Graphite)
        BottomNavigationItem(currentScreen == AppScreen.PROFILE, { onNavigate(AppScreen.PROFILE) },
            icon = { Icon(Icons.Default.Person, null) }, label = { Text("Profil") }, selectedContentColor = ActionRed, unselectedContentColor = Graphite)
        BottomNavigationItem(currentScreen == AppScreen.SETTINGS, { onNavigate(AppScreen.SETTINGS) },
            modifier = Modifier.semantics { if (settingsActionCount > 0) stateDescription = settingsActionDescription(settingsActionCount) },
            icon = { NavigationIcon(Icons.Default.Settings, settingsActionCount) }, label = { Text("Paramètres") }, selectedContentColor = ActionRed, unselectedContentColor = Graphite)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatsStrip(stats: UserStats, modifier: Modifier = Modifier) {
    FlowRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SymbolValue(LearningSymbol.FLAME, "${stats.streakDays} j", Modifier.semantics { contentDescription = "Série : ${stats.streakDays} jours" })
        SymbolValue(LearningSymbol.XP, "${stats.xp} XP", Modifier.semantics { contentDescription = "${stats.xp} points d’expérience" })
        SymbolValue(LearningSymbol.HEART, "${stats.hearts}/${stats.maxHearts}", Modifier.semantics { contentDescription = "Vies : ${stats.hearts} sur ${stats.maxHearts}" })
    }
}

@Composable
fun LearningPathScreen(subject: Subject, completedLessonIds: Set<String>, onStartLesson: (Lesson) -> Unit, selectedUnitId: String?, onOpenCourse: () -> Unit = {}, onSelectUnit: (String) -> Unit) {
    val lessons = subject.units.flatMap { it.lessons }
    val completed = completedLessonIds + lessons.filter { it.isCompleted }.map { it.id }
    val lockedIds = lessons.filterNot { subject.isLessonUnlocked(it, completed) }.map { it.id }.toSet()
    val unit = subject.units.find { it.id == selectedUnitId } ?: subject.units.firstOrNull()
    val courseListState = key(subject.id) { rememberLazyListState() }
    val lessonListState = key(subject.id, unit?.id) { rememberLazyListState() }
    var searchExpanded by remember(subject.id, unit?.id) { mutableStateOf(false) }
    var query by remember(subject.id, unit?.id) { mutableStateOf("") }
    var filter by remember(subject.id, unit?.id) { mutableStateOf(LessonFilter.ALL) }
    var previousUnitId by remember(subject.id) { mutableStateOf(unit?.id) }
    val unitOpacity = remember(subject.id) { Animatable(1f) }
    LaunchedEffect(subject.id, unit?.id) {
        if (previousUnitId != unit?.id) {
            unitOpacity.snapTo(0.85f)
            unitOpacity.animateTo(1f, tween(COURSE_FADE_MS, easing = FastOutSlowInEasing))
            previousUnitId = unit?.id
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val split = maxWidth >= 880.dp
        val gutter = if (maxWidth >= 600.dp) 24.dp else 16.dp
        // Desktop: 260 dp course column + 24 dp gutter + flexible lesson workspace.
        Row(Modifier.fillMaxSize().padding(gutter), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            if (split) LazyColumn(Modifier.width(260.dp).fillMaxHeight(), state = courseListState, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { Text("Mes cours", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp, modifier = Modifier.semantics { heading() }) }
                item { Text(subject.description, color = Graphite) }
                items(subject.units, key = { it.id }) { course -> CourseChoice(course, course.id == unit?.id, completed) { onSelectUnit(course.id) } }
            }
            LazyColumn(Modifier.weight(1f).fillMaxHeight().graphicsLayer { alpha = unitOpacity.value }, state = lessonListState,
                verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                if (!split) {
                    item { Text("Mes cours", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp, modifier = Modifier.semantics { heading() }) }
                    item { CoursePicker(subject.units, unit, onSelectUnit) }
                }
                if (unit == null) item { EmptyLearningState("Aucune leçon", "Ce cours ne contient pas encore de leçon.") }
                else {
                    val next = unit.lessons.firstOrNull { it.id !in completed && it.id !in lockedIds }
                    item { CourseOverview(unit, completed, next) { lesson -> onStartLesson(lesson.copy(isLocked = false)) } }
                    item { OutlinedButton(onOpenCourse, Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = LearningShape) {
                        PixelIcon(LearningSymbol.BOOK, description = null); Spacer(Modifier.width(10.dp)); Text("Lire le cours et les supports", style = MaterialTheme.typography.button.copy(letterSpacing = 0.sp, fontWeight = FontWeight.Medium))
                    } }
                    item {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Les leçons", Modifier.weight(1f).semantics { heading() }, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
                            IconButton(onClick = { searchExpanded = !searchExpanded; if (!searchExpanded) query = "" },
                                modifier = Modifier.size(48.dp).semantics { contentDescription = if (searchExpanded) "Fermer la recherche" else "Rechercher une leçon" }) {
                                Icon(if (searchExpanded) Icons.Default.Close else Icons.Default.Search, null, tint = Ink)
                            }
                        }
                        AnimatedVisibility(visible = searchExpanded,
                            enter = fadeIn(tween(SEARCH_FADE_MS, easing = FastOutSlowInEasing)),
                            exit = fadeOut(tween(SEARCH_FADE_MS, easing = FastOutSlowInEasing))) {
                            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().heightIn(min = 56.dp), singleLine = true,
                                label = { Text("Rechercher dans ce cours", letterSpacing = 0.sp) }, shape = LearningShape)
                        }
                    }
                    item { LessonFilterChoices(filter) { filter = it } }
                    val visibleLessons = unit.lessons.filter { lesson ->
                        val search = query.trim()
                        val matchesQuery = search.isBlank() || lesson.title.contains(search, ignoreCase = true) || lesson.objective.contains(search, ignoreCase = true)
                        val done = lesson.id in completed || lesson.isCompleted
                        val matchesFilter = when (filter) {
                            LessonFilter.ALL -> true
                            LessonFilter.AVAILABLE -> !done && lesson.id !in lockedIds
                            LessonFilter.COMPLETED -> done
                        }
                        matchesQuery && matchesFilter
                    }
                    item { Text("${visibleLessons.size} sur ${unit.lessons.size} leçons", fontSize = 13.sp, color = Graphite, letterSpacing = 0.sp) }
                    if (visibleLessons.isEmpty()) item {
                        Column(Modifier.fillMaxWidth().background(White, LearningShape).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(if (unit.lessons.isEmpty()) "Aucune leçon dans ce cours" else "Aucune leçon ne correspond", fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
                            Text(if (unit.lessons.isEmpty()) "Choisis un autre cours pour continuer." else "Essaie une autre recherche ou réinitialise les filtres.", color = Graphite)
                            if (unit.lessons.isNotEmpty()) TextButton({ query = ""; filter = LessonFilter.ALL }, Modifier.heightIn(min = 48.dp)) {
                                Text("Réinitialiser", letterSpacing = 0.sp)
                            }
                        }
                    }
                    items(visibleLessons, key = { it.id }) { lesson ->
                        val display = lesson.copy(isLocked = lesson.id in lockedIds)
                        LessonNodeItem(display, lesson.id in completed) { onStartLesson(display) }
                    }
                }
            }
        }
    }
}

private enum class LessonFilter { ALL, AVAILABLE, COMPLETED }

@Composable
private fun LessonFilterChoices(selected: LessonFilter, onSelect: (LessonFilter) -> Unit) {
    Row(Modifier.fillMaxWidth().semantics { selectableGroup() }, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(LessonFilter.ALL to "Toutes", LessonFilter.AVAILABLE to "Disponibles", LessonFilter.COMPLETED to "Terminées").forEach { (filter, label) ->
            val active = selected == filter
            val interaction = remember(filter) { MutableInteractionSource() }
            OutlinedButton(onClick = { onSelect(filter) }, Modifier.weight(1f).heightIn(min = 48.dp).learningFocus(interaction)
                .semantics { this.selected = active }, interactionSource = interaction, shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (active) Ink else Concrete),
                colors = ButtonDefaults.outlinedButtonColors(backgroundColor = if (active) Ink else Color.Transparent, contentColor = if (active) White else Ink),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)) {
                Text(label, fontSize = 12.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal, letterSpacing = 0.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun CourseChoice(unit: LearningUnit, selected: Boolean, completed: Set<String>, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val (hovered, pressed) = source.hoveredAndPressed()
    val surface by animateColorAsState(when { selected -> ActionRed.copy(alpha = 0.07f); hovered -> White; else -> Paper }, tween(INTERACTION_MOTION_MS, easing = FastOutSlowInEasing))
    val outline by animateColorAsState(when { selected -> ActionRed; hovered -> Graphite; else -> Concrete }, tween(INTERACTION_MOTION_MS, easing = FastOutSlowInEasing))
    val scale by animateFloatAsState(if (pressed) 0.985f else 1f, tween(INTERACTION_MOTION_MS, easing = FastOutSlowInEasing))
    OutlinedButton(onClick, Modifier.fillMaxWidth().graphicsLayer {
        scaleX = scale; scaleY = scale
    }.learningFocus(source).semantics { this.selected = selected }, interactionSource = source,
        shape = LearningShape, border = BorderStroke(1.dp, outline),
        colors = ButtonDefaults.outlinedButtonColors(backgroundColor = surface, contentColor = Ink), contentPadding = PaddingValues(16.dp)) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (unit.courseCode.isNotBlank()) Text(unit.courseCode, color = ActionRed, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
            Text(unit.title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, letterSpacing = 0.sp)
            Text("${unit.lessons.count { it.id in completed }} / ${unit.lessons.size} leçons", color = Graphite, letterSpacing = 0.sp)
        }
    }
}

@Composable
private fun CoursePicker(units: List<LearningUnit>, selected: LearningUnit?, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton({ expanded = true }, Modifier.fillMaxWidth().heightIn(min = 56.dp), shape = LearningShape) {
            Text(selected?.title ?: "Choisir un cours", Modifier.weight(1f), letterSpacing = 0.sp); Icon(Icons.Default.ArrowDropDown, null)
        }
        DropdownMenu(expanded, { expanded = false }) {
            units.forEach { unit -> DropdownMenuItem({ onSelect(unit.id); expanded = false }, Modifier.heightIn(min = 48.dp)) { Text(unit.title) } }
        }
    }
}

@Composable
private fun CourseOverview(unit: LearningUnit, completed: Set<String>, next: Lesson?, onContinue: (Lesson) -> Unit) {
    val count = unit.lessons.count { it.id in completed }
    val progress by animateFloatAsState(
        targetValue = if (unit.lessons.isEmpty()) 0f else (count.toFloat() / unit.lessons.size).coerceIn(0f, 1f),
        animationSpec = tween(PROGRESS_MOTION_MS, easing = FastOutSlowInEasing)
    )
    Surface(shape = LearningShape, color = Ink) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val illustrated = maxWidth >= 600.dp
            Row(Modifier.fillMaxWidth().padding(24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (unit.courseCode.isNotBlank() || unit.edition.isNotBlank()) Text(
                        listOf(unit.courseCode, unit.edition).filter { it.isNotBlank() }.joinToString(" · "), color = Concrete, fontSize = 13.sp, letterSpacing = 0.sp)
                    Text(unit.title, color = White, fontSize = 29.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
                    if (unit.description.isNotBlank()) Text(unit.description, color = White.copy(alpha = 0.82f), fontSize = 15.sp, letterSpacing = 0.sp)
                    Text("$count / ${unit.lessons.size} leçons terminées", color = White, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
                    LinearProgressIndicator(progress,
                        Modifier.fillMaxWidth().height(4.dp), color = Concrete, backgroundColor = Graphite)
                    val expected = unit.expectedLessonCount
                    if (expected != null && expected != unit.lessons.size) Text(
                        "${unit.lessons.size} leçons disponibles sur $expected prévues dans la source.", color = Concrete, fontSize = 13.sp, letterSpacing = 0.sp)
                    if (next != null) LearningButton("Continuer · ${next.title}", { onContinue(next) }, Modifier.fillMaxWidth(), focusColor = White)
                }
                if (illustrated) VoxelBookshelf(Modifier.width(160.dp).height(64.dp))
            }
        }
    }
}

@Composable
fun LessonNodeItem(lesson: Lesson, isCompleted: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val (hovered, pressed) = interaction.hoveredAndPressed(!lesson.isLocked)
    val surface by animateColorAsState(if (hovered) Paper else White, tween(INTERACTION_MOTION_MS, easing = FastOutSlowInEasing))
    val outline by animateColorAsState(when { isCompleted -> BrandRed; hovered && !lesson.isLocked -> Graphite; else -> Concrete }, tween(INTERACTION_MOTION_MS, easing = FastOutSlowInEasing))
    val scale by animateFloatAsState(if (pressed) 0.985f else 1f, tween(INTERACTION_MOTION_MS, easing = FastOutSlowInEasing))
    val state = when { isCompleted -> "Terminée · Réviser"; lesson.isLocked -> "Verrouillée · Terminer les prérequis"; else -> "Disponible · Commencer" }
    OutlinedButton(onClick, Modifier.fillMaxWidth().heightIn(min = 88.dp).graphicsLayer {
        scaleX = scale; scaleY = scale
    }.learningFocus(interaction).semantics { stateDescription = state },
        enabled = !lesson.isLocked, interactionSource = interaction, shape = LearningShape,
        border = BorderStroke(1.dp, outline),
        colors = ButtonDefaults.outlinedButtonColors(backgroundColor = surface, contentColor = Ink, disabledContentColor = Graphite), contentPadding = PaddingValues(16.dp)) {
        Icon(when { isCompleted -> Icons.Default.CheckCircle; lesson.isLocked -> Icons.Default.Lock; else -> Icons.Default.PlayArrow }, null,
            tint = if (isCompleted) ActionRed else Graphite, modifier = Modifier.size(32.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            lesson.sourceSessionLabel?.let { Text(it, fontSize = 13.sp, color = Graphite, letterSpacing = 0.sp) }
            Text(lesson.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
            Text("${lesson.exercises.size} exercices · ${lesson.xpReward} XP à la première réussite", fontSize = 14.sp, color = Graphite, letterSpacing = 0.sp)
            Text(state, fontSize = 14.sp, color = if (lesson.isLocked) Graphite else ActionRed, letterSpacing = 0.sp)
        }
    }
}

@Composable
fun EmptyLearningState(title: String, message: String) {
    Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold); Text(message, color = Graphite)
    }
}
