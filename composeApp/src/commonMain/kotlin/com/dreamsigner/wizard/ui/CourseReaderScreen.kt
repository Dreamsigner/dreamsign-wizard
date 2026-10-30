package com.dreamsigner.wizard.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dreamsigner.wizard.model.LearningUnit
import com.dreamsigner.wizard.model.Lesson
import com.dreamsigner.wizard.data.CourseResources
import kotlinx.coroutines.launch

/** All authored sections can be read independently of exercise unlocks or completion. */
@Composable
fun CourseReaderScreen(
    course: LearningUnit,
    canPractice: (Lesson) -> Boolean,
    onStartLesson: (Lesson) -> Unit,
    onClose: () -> Unit,
    typesetMath: Boolean = false,
    showVideoPreviews: Boolean = false,
    searchableContents: Boolean = false,
    favoriteLessonIds: Set<String> = emptySet(),
    onToggleFavorite: ((String) -> Unit)? = null,
    favoriteSaveStatus: String? = null,
) {
    val listState = key(course.id) { rememberLazyListState() }
    var showContents by remember(course.id) { mutableStateOf(false) }
    var contentsQuery by remember(course.id) { mutableStateOf("") }
    var favoritesOnly by remember(course.id) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 1000.dp
        Scaffold(backgroundColor = Paper, topBar = {
            Column {
                TopAppBar(title = { Text("Cours et supports", fontWeight = FontWeight.Bold) }, backgroundColor = White,
                    navigationIcon = { IconButton(onClose, Modifier.size(48.dp)) { Icon(Icons.Default.Close, "Fermer le cours") } },
                    actions = { if (!wide) TextButton({ contentsQuery = ""; showContents = true }, Modifier.heightIn(min = 48.dp)) { Text("Sommaire") } }, elevation = 0.dp)
                if (onToggleFavorite != null && favoriteSaveStatus != null) Surface(color = White) {
                    Text(favoriteSaveStatus, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), fontSize = 12.sp, color = Graphite)
                }
            }
        }) { padding ->
            BoxWithConstraints(Modifier.padding(padding).fillMaxSize()) {
                val activeLessonIndex by remember(listState) {
                    derivedStateOf { (listState.firstVisibleItemIndex - 1).takeIf { it >= 0 } ?: -1 }
                }
                val reading: @Composable (Modifier) -> Unit = { modifier ->
                    BoxWithConstraints(modifier) {
                        val side = ((maxWidth - 920.dp) / 2).coerceAtLeast(16.dp)
                        LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(horizontal = side, vertical = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            item(key = "overview") {
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text(course.title, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
                                    Text("Toutes les notes rédigées en français pour ce parcours sont réunies ici et disponibles hors ligne, y compris pour les leçons encore verrouillées.", color = Graphite)
                                    Text("Ces notes de pratique ne remplacent pas le cours original complet. Les supports de référence ci-dessous s’ouvrent avec Internet.", color = Graphite)
                                    CourseReferenceLinks(course, showVideoPreviews)
                                }
                            }
                            items(course.lessons, key = { it.id }) { lesson ->
                                Surface(color = White, shape = LearningShape) {
                                    Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Text(lesson.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
                                        if (onToggleFavorite != null) {
                                            val favorite = lesson.id in favoriteLessonIds
                                            val action = if (favorite) "Retirer des favoris" else "Ajouter aux favoris"
                                            TextButton({ onToggleFavorite(lesson.id) }, Modifier.heightIn(min = 48.dp).semantics {
                                                contentDescription = "$action · ${lesson.title}"
                                                stateDescription = if (favorite) "En favori" else "Pas en favori"
                                            }) {
                                                Icon(Icons.Default.Star, contentDescription = null, tint = if (favorite) ActionRed else Graphite)
                                                Spacer(Modifier.width(8.dp))
                                                Text(action, color = if (favorite) ActionRed else Graphite)
                                            }
                                        }
                                        LessonNotes(lesson, includeHeading = false, typesetMath = typesetMath, showVideoPreviews = showVideoPreviews)
                                        Text("Exercices associés : ${lesson.exercises.size}", fontWeight = FontWeight.Medium)
                                        LearningButton("Pratiquer · ${lesson.title}", { onStartLesson(lesson) }, enabled = canPractice(lesson))
                                        if (!canPractice(lesson)) Text("Termine les prérequis pour pratiquer. Le cours reste consultable.", color = Graphite)
                                    }
                                }
                            }
                        }
                    }
                }
                if (wide) Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Surface(Modifier.width(304.dp).fillMaxHeight().padding(12.dp), color = White, shape = LearningShape) {
                        CourseContentsPanel(course, contentsQuery, { contentsQuery = it }, searchableContents,
                            favoriteLessonIds, onToggleFavorite, favoritesOnly, { favoritesOnly = it }, activeLessonIndex,
                            onSelect = { index -> scope.launch { listState.animateScrollToItem(index + 1) } },
                            modifier = Modifier.fillMaxSize().padding(16.dp), wide = true)
                    }
                    reading(Modifier.weight(1f).fillMaxHeight())
                } else reading(Modifier.fillMaxSize())
            }
        }
        if (showContents && !wide) AlertDialog(onDismissRequest = { showContents = false },
            title = { Text("Sommaire du cours") },
            text = {
                CourseContentsPanel(course, contentsQuery, { contentsQuery = it }, searchableContents,
                    favoriteLessonIds, onToggleFavorite, favoritesOnly, { favoritesOnly = it }, -1,
                    onSelect = { index -> showContents = false; scope.launch { listState.scrollToItem(index + 1) } },
                    modifier = Modifier.heightIn(max = 480.dp), wide = false)
            }, confirmButton = { TextButton({ showContents = false }) { Text("Fermer") } }, backgroundColor = White)
    }
}

@Composable
private fun CourseContentsPanel(
    course: LearningUnit,
    query: String,
    onQueryChange: (String) -> Unit,
    searchable: Boolean,
    favoriteLessonIds: Set<String>,
    onToggleFavorite: ((String) -> Unit)?,
    favoritesOnly: Boolean,
    onFavoritesOnlyChange: (Boolean) -> Unit,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier,
    wide: Boolean,
) {
    val indices = remember(course, query, favoriteLessonIds, favoritesOnly, onToggleFavorite != null) {
        matchingLessonIndices(course, query).filter { index ->
            onToggleFavorite == null || !favoritesOnly || course.lessons[index].id in favoriteLessonIds
        }
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (wide) Text("Sommaire du cours", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
        if (searchable) {
            OutlinedTextField(query, onQueryChange, modifier = Modifier.fillMaxWidth(),
                label = { Text("Rechercher une leçon") }, placeholder = { Text("Titre ou objectif") }, singleLine = true)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("${indices.size} leçon${if (indices.size == 1) "" else "s"}", Modifier.weight(1f), color = Graphite)
                if (query.isNotEmpty()) TextButton({ onQueryChange("") }, Modifier.heightIn(min = 48.dp)) { Text("Effacer") }
            }
        }
        if (onToggleFavorite != null) Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(favoritesOnly, role = Role.Checkbox, onValueChange = onFavoritesOnlyChange),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(favoritesOnly, onCheckedChange = null)
            Spacer(Modifier.width(8.dp))
            Text("Favoris uniquement")
        }
        if (indices.isEmpty()) Text(
            if (onToggleFavorite != null && favoritesOnly) {
                if (query.isBlank()) "Aucune leçon favorite dans ce cours. Ajoute une étoile depuis une leçon."
                else "Aucun favori ne correspond à cette recherche."
            } else "Aucune leçon trouvée. Essaie un autre titre ou objectif.", color = Graphite)
        LazyColumn(Modifier.weight(1f, fill = wide)) {
            items(indices, key = { course.lessons[it].id }) { index ->
                val selected = index == selectedIndex
                TextButton(onClick = { onSelect(index) }, Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .semantics { this.selected = selected },
                    colors = ButtonDefaults.textButtonColors(backgroundColor = if (selected) Paper else White, contentColor = Ink)) {
                    if (onToggleFavorite != null && course.lessons[index].id in favoriteLessonIds) {
                        Icon(Icons.Default.Star, contentDescription = "Leçon favorite", tint = ActionRed)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("${index + 1}. ${course.lessons[index].title}", fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
    }
}

@Composable
private fun CourseReferenceLinks(course: LearningUnit, showVideoPreviews: Boolean) {
    val uri = androidx.compose.ui.platform.LocalUriHandler.current
    val resources = CourseResources.forCourse(course.id)
    if (resources.isNotEmpty()) ReferenceLinks(resources, showVideoPreviews)
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

/** Search authored titles/objectives; original indices preserve source order and scroll targets. */
internal fun matchingLessonIndices(course: LearningUnit, query: String): List<Int> {
    val words = searchKey(query).split(Regex("\\s+")).filter { it.isNotBlank() }
    return course.lessons.indices.filter { index ->
        val lesson = course.lessons[index]
        val haystack = searchKey("${lesson.title} ${lesson.objective}")
        words.all { it in haystack }
    }
}

private fun searchKey(value: String): String = buildString {
    value.lowercase().forEach { char ->
        append(when (char) {
            'à', 'â', 'ä' -> "a"
            'é', 'è', 'ê', 'ë' -> "e"
            'î', 'ï' -> "i"
            'ô', 'ö' -> "o"
            'ù', 'û', 'ü' -> "u"
            'ç' -> "c"
            'œ' -> "oe"
            'æ' -> "ae"
            else -> char.toString()
        })
    }
}
