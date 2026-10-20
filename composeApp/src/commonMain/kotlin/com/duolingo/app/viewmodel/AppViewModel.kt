package com.duolingo.app.viewmodel

import com.duolingo.app.data.SampleData
import com.duolingo.app.model.*
import com.duolingo.app.storage.ProgressStore
import com.duolingo.app.storage.createProgressStore
import com.duolingo.app.storage.todayLocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class AppScreen { LEARNING_PATH, LESSON_QUIZ, PROFILE }

class AppViewModel(
    private val progressStore: ProgressStore = createProgressStore(),
    private val today: () -> String = ::todayLocalDate,
    catalogue: List<Subject> = SampleData.sampleSubjects
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private var preserveUnreadableSave = false
    private val saveScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val saveMutex = Mutex()
    private var pendingDraftSave: Job? = null
    private val _persistenceStatus = MutableStateFlow("Progression enregistrée sur cet appareil.")
    val persistenceStatus: StateFlow<String> = _persistenceStatus.asStateFlow()
    private val initialStats = loadProgress(catalogue)
    private val _userStats = MutableStateFlow(initialStats)
    val userStats: StateFlow<UserStats> = _userStats.asStateFlow()
    private val _subjects = MutableStateFlow(catalogue.map { subject ->
        subject.copy(units = subject.units.map { unit ->
            unit.copy(lessons = unit.lessons.map { lesson ->
                lesson.copy(isCompleted = lesson.isCompleted || lesson.id in initialStats.completedLessonIds)
            })
        })
    })
    val subjects: StateFlow<List<Subject>> = _subjects.asStateFlow()
    private val _selectedSubjectId = MutableStateFlow(initialStats.activeSubjectId)
    val selectedSubjectId: StateFlow<String> = _selectedSubjectId.asStateFlow()
    private val _currentScreen = MutableStateFlow(AppScreen.LEARNING_PATH)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()
    private val _activeLesson = MutableStateFlow<Lesson?>(null)
    val activeLesson: StateFlow<Lesson?> = _activeLesson.asStateFlow()
    private val _currentExerciseIndex = MutableStateFlow(0)
    val currentExerciseIndex: StateFlow<Int> = _currentExerciseIndex.asStateFlow()
    private val _selectedOptionIndex = MutableStateFlow<Int?>(null)
    val selectedOptionIndex: StateFlow<Int?> = _selectedOptionIndex.asStateFlow()
    private val _selectedTokenSequence = MutableStateFlow<List<String>>(emptyList())
    val selectedTokenSequence: StateFlow<List<String>> = _selectedTokenSequence.asStateFlow()
    private val _isAnswerChecked = MutableStateFlow(false)
    val isAnswerChecked: StateFlow<Boolean> = _isAnswerChecked.asStateFlow()
    private val _isAnswerCorrect = MutableStateFlow(false)
    val isAnswerCorrect: StateFlow<Boolean> = _isAnswerCorrect.asStateFlow()
    private val _isLessonCompleted = MutableStateFlow(false)
    val isLessonCompleted: StateFlow<Boolean> = _isLessonCompleted.asStateFlow()
    private val _lastAwardedXp = MutableStateFlow(0)
    val lastAwardedXp: StateFlow<Int> = _lastAwardedXp.asStateFlow()
    private val _reviewedCriteria = MutableStateFlow<Set<Int>>(emptySet())
    val reviewedCriteria: StateFlow<Set<Int>> = _reviewedCriteria.asStateFlow()

    fun updateWrittenResponse(text: String) {
        if (_isAnswerChecked.value || _isLessonCompleted.value || text.length > 12000) return
        val exercise = currentExercise() as? Exercise.WrittenProduction ?: return
        _userStats.value = _userStats.value.copy(writtenDrafts = _userStats.value.writtenDrafts + (exercise.id to text))
        _reviewedCriteria.value = emptySet()
        if (!preserveUnreadableSave) {
            _persistenceStatus.value = "Brouillon modifié : enregistrement en cours…"
            pendingDraftSave?.cancel()
            pendingDraftSave = saveScope.launch {
                delay(600)
                writeProgress()
            }
        }
    }

    fun toggleReviewCriterion(index: Int) {
        if (_isAnswerChecked.value || _isLessonCompleted.value) return
        val exercise = currentExercise() as? Exercise.WrittenProduction ?: return
        if (index !in exercise.reviewCriteria.indices) return
        val current = _reviewedCriteria.value
        _reviewedCriteria.value = if (index in current) current - index else current + index
    }

    private fun loadProgress(catalogue: List<Subject>): UserStats {
        val fallback = UserStats(activeSubjectId = catalogue.firstOrNull()?.id.orEmpty())
        return try {
            val raw = progressStore.read() ?: return fallback
            val saved = json.decodeFromString<UserStats>(raw)
            val day = localDayOrdinal(today())
            val previous = localDayOrdinal(saved.lastStudyDate)
            val activeStreak = day != null && previous != null && day - previous in 0..1
            saved.copy(
                xp = saved.xp.coerceAtLeast(0), streakDays = if (activeStreak) saved.streakDays.coerceAtLeast(0) else 0,
                maxHearts = saved.maxHearts.coerceIn(1, 20),
                hearts = saved.hearts.coerceIn(0, saved.maxHearts.coerceIn(1, 20)),
                activeSubjectId = saved.activeSubjectId.takeIf { id -> catalogue.any { it.id == id } } ?: fallback.activeSubjectId
            )
        } catch (_: Exception) {
            preserveUnreadableSave = true
            _persistenceStatus.value = "Sauvegarde illisible ou inaccessible : original conservé, progression limitée à cette session."
            fallback
        }
    }

    private fun persist() {
        pendingDraftSave?.cancel()
        pendingDraftSave = null
        runBlocking { writeProgress() }
    }

    // The mutex serializes debounced background saves and final navigation/close flushes.
    // https://kotlinlang.org/docs/shared-mutable-state-and-concurrency.html#mutual-exclusion
    private suspend fun writeProgress() = saveMutex.withLock {
        if (preserveUnreadableSave) return@withLock
        try {
            // https://kotlinlang.org/docs/serialization.html
            progressStore.write(json.encodeToString(_userStats.value))
            _persistenceStatus.value = "Progression enregistrée sur cet appareil."
        } catch (_: Exception) {
            _persistenceStatus.value = "Enregistrement indisponible : progression conservée pour cette session."
        }
    }

    /** Flush the last edit before the app's composition is disposed. */
    fun close() {
        persist()
        saveScope.cancel()
    }

    fun selectSubject(subjectId: String) {
        if (_subjects.value.none { it.id == subjectId }) return
        _selectedSubjectId.value = subjectId
        _userStats.value = _userStats.value.copy(activeSubjectId = subjectId)
        persist()
    }

    fun navigateTo(screen: AppScreen) {
        if (screen == AppScreen.LESSON_QUIZ && _activeLesson.value == null) return
        if (pendingDraftSave != null) persist()
        _currentScreen.value = screen
    }

    fun startLesson(lesson: Lesson) {
        val subject = _subjects.value.firstOrNull { item -> item.units.any { u -> u.lessons.any { it.id == lesson.id } } } ?: return
        val canonical = subject.units.flatMap { it.lessons }.first { it.id == lesson.id }
        if (canonical.exercises.isEmpty() || !subject.isLessonUnlocked(canonical, _userStats.value.completedLessonIds)) return
        _activeLesson.value = canonical
        _currentExerciseIndex.value = 0
        _lastAwardedXp.value = 0
        _isLessonCompleted.value = false
        resetAnswer()
        _currentScreen.value = AppScreen.LESSON_QUIZ
    }

    fun selectOption(index: Int) {
        if (_isAnswerChecked.value || _isLessonCompleted.value) return
        val options = when (val exercise = currentExercise()) {
            is Exercise.MultipleChoice -> exercise.options
            is Exercise.CodeChallenge -> exercise.options
            is Exercise.MathChallenge -> exercise.options
            else -> emptyList()
        }
        if (index in options.indices) _selectedOptionIndex.value = index
    }

    fun toggleTokenSelection(token: String) {
        if (_isAnswerChecked.value || _isLessonCompleted.value) return
        val exercise = currentExercise() as? Exercise.Translation ?: return
        if (token !in exercise.targetTokens) return
        val current = _selectedTokenSequence.value.toMutableList()
        if (current.contains(token)) current.remove(token) else current.add(token)
        _selectedTokenSequence.value = current
    }

    private fun currentExercise(): Exercise? = _activeLesson.value?.exercises?.getOrNull(_currentExerciseIndex.value)

    fun checkAnswer() {
        if (_isAnswerChecked.value || _isLessonCompleted.value) return
        val correct = when (val exercise = currentExercise()) {
            is Exercise.WrittenProduction -> {
                val text = _userStats.value.writtenDrafts[exercise.id].orEmpty()
                if (!exercise.canFinishPractice(text, _reviewedCriteria.value)) return
                true
            }
            is Exercise.MultipleChoice -> (_selectedOptionIndex.value ?: return) == exercise.correctOptionIndex
            is Exercise.CodeChallenge -> (_selectedOptionIndex.value ?: return) == exercise.correctOptionIndex
            is Exercise.MathChallenge -> (_selectedOptionIndex.value ?: return) == exercise.correctOptionIndex
            is Exercise.Translation -> {
                if (_selectedTokenSequence.value.isEmpty()) return
                _selectedTokenSequence.value == exercise.correctSequence
            }
            else -> return
        }
        _isAnswerCorrect.value = correct
        _isAnswerChecked.value = true
        if (!correct) {
            _userStats.value = _userStats.value.copy(hearts = (_userStats.value.hearts - 1).coerceAtLeast(0))
            persist()
        }
    }

    fun retryExercise() {
        if (!_isAnswerChecked.value || _isAnswerCorrect.value || _isLessonCompleted.value) return
        resetAnswer()
    }

    private fun resetAnswer() {
        _reviewedCriteria.value = emptySet()
        _selectedOptionIndex.value = null
        _selectedTokenSequence.value = emptyList()
        _isAnswerChecked.value = false
        _isAnswerCorrect.value = false
    }

    fun nextExercise() {
        val lesson = _activeLesson.value ?: return
        if (_isLessonCompleted.value || !_isAnswerChecked.value || !_isAnswerCorrect.value) return
        if (_currentExerciseIndex.value + 1 < lesson.exercises.size) {
            _currentExerciseIndex.value += 1
            resetAnswer()
            return
        }
        val stats = _userStats.value
        val awarded = if (lesson.id in stats.completedLessonIds || lesson.isCompleted) 0 else lesson.xpReward.coerceAtLeast(0)
        val studyDate = today()
        val day = localDayOrdinal(studyDate)
        val previous = localDayOrdinal(stats.lastStudyDate)
        val streak = when {
            day == null -> stats.streakDays
            previous == day -> stats.streakDays.coerceAtLeast(1)
            previous != null && day == previous + 1 -> stats.streakDays + 1
            else -> 1
        }
        _userStats.value = stats.copy(xp = stats.xp + awarded,
            completedLessonIds = stats.completedLessonIds + lesson.id,
            streakDays = streak, lastStudyDate = if (day != null) studyDate else stats.lastStudyDate)
        _subjects.value = _subjects.value.map { subject -> subject.copy(units = subject.units.map { unit ->
            unit.copy(lessons = unit.lessons.map { if (it.id == lesson.id) it.copy(isCompleted = true) else it })
        }) }
        _lastAwardedXp.value = awarded
        _isLessonCompleted.value = true
        persist()
    }
}
