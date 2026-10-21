package com.duolingo.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duolingo.app.model.LearningUnit
import com.duolingo.app.model.Lesson
import com.duolingo.app.data.CourseResources
import kotlinx.coroutines.launch

/** All authored sections can be read independently of exercise unlocks or completion. */
@Composable
fun CourseReaderScreen(
    course: LearningUnit,
    canPractice: (Lesson) -> Boolean,
    onStartLesson: (Lesson) -> Unit,
    onClose: () -> Unit,
    typesetMath: Boolean = false,
) {
    val listState = key(course.id) { rememberLazyListState() }
    var showContents by remember(course.id) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Scaffold(backgroundColor = Paper, topBar = {
        TopAppBar(title = { Text("Cours et supports", fontWeight = FontWeight.Bold) }, backgroundColor = White,
            navigationIcon = { IconButton(onClose, Modifier.size(48.dp)) { Icon(Icons.Default.Close, "Fermer le cours") } },
            actions = { TextButton({ showContents = true }, Modifier.heightIn(min = 48.dp)) { Text("Sommaire") } }, elevation = 0.dp)
    }) { padding ->
        BoxWithConstraints(Modifier.padding(padding).fillMaxSize()) {
            val side = ((maxWidth - 920.dp) / 2).coerceAtLeast(16.dp)
            LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(horizontal = side, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)) {
                item(key = "overview") {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(course.title, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
                        Text("Toutes les notes rédigées en français pour ce parcours sont réunies ici et disponibles hors ligne, y compris pour les leçons encore verrouillées.", color = Graphite)
                        Text("Ces notes de pratique ne remplacent pas le cours original complet. Les supports de référence ci-dessous s’ouvrent avec Internet.", color = Graphite)
                        CourseReferenceLinks(course)
                    }
                }
                items(course.lessons, key = { it.id }) { lesson ->
                    Surface(color = White, shape = LearningShape) {
                        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(lesson.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
                            LessonNotes(lesson, includeHeading = false, typesetMath = typesetMath)
                            Text("Exercices associés : ${lesson.exercises.size}", fontWeight = FontWeight.Medium)
                            LearningButton("Pratiquer · ${lesson.title}", { onStartLesson(lesson) }, enabled = canPractice(lesson))
                            if (!canPractice(lesson)) Text("Termine les prérequis pour pratiquer. Le cours reste consultable.", color = Graphite)
                        }
                    }
                }
            }
        }
    }
    if (showContents) AlertDialog(onDismissRequest = { showContents = false },
        title = { Text("Sommaire du cours") },
        text = {
            LazyColumn(Modifier.heightIn(max = 480.dp)) {
                items(course.lessons.indices.toList(), key = { course.lessons[it].id }) { index ->
                    TextButton({
                        showContents = false
                        scope.launch { listState.scrollToItem(index + 1) }
                    }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("${index + 1}. ${course.lessons[index].title}") }
                }
            }
        }, confirmButton = { TextButton({ showContents = false }) { Text("Fermer") } }, backgroundColor = White)
}

@Composable
private fun CourseReferenceLinks(course: LearningUnit) {
    val uri = androidx.compose.ui.platform.LocalUriHandler.current
    val resources = CourseResources.forCourse(course.id)
    if (resources.isNotEmpty()) ReferenceLinks(resources)
    else if (course.sourceUrl.isNotBlank()) TextButton({ runCatching { uri.openUri(course.sourceUrl) } }, Modifier.heightIn(min = 48.dp)) {
        Text("Ouvrir le cours original · ${course.courseCode.ifBlank { course.title }}")
    }
    if (course.id == "learn_prolog_now_2026") PrologGlossary()
}

@Composable
private fun PrologGlossary() {
    var expanded by remember { mutableStateOf(false) }
    TextButton({ expanded = !expanded }, Modifier.heightIn(min = 48.dp)) { Text(if (expanded) "Masquer le vocabulaire" else "Vocabulaire Prolog · français / anglais") }
    if (expanded) {
        Text("Prolog est le nom du langage : il ne se traduit pas par « prologue ». Les mots de code et les prédicats restent inchangés.")
        Text("fait (fact) · règle (rule) · requête (query) · terme composé (compound term) · foncteur et arité (functor / arity) · unification (unification) · retour arrière (backtracking) · coupure (cut) · liste différence (difference list) · terme clos, sans variable (ground term) · flux (stream).")
        Text("La négation par l’échec (negation as failure), notée \\+ G, réussit lorsque la recherche de G se termine sans solution. Elle ne prouve pas une négation logique universelle; une recherche infinie ne constitue pas un échec fini.")
        Text("Choix terminologiques confrontés à Learn Prolog Now!, à sa présentation française Prolog tout de suite ! et aux cours universitaires français cités dans le dépôt.", color = Graphite)
    }
}
