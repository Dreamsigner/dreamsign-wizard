package com.duolingo.app.viewmodel

import com.duolingo.app.ai.AiContentPipeline
import com.duolingo.app.data.SampleData
import com.duolingo.app.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppScreen {
    LEARNING_PATH,
    LESSON_QUIZ,
    AI_IMPORTER,
    PROFILE
}

class AppViewModel {
    private val aiPipeline = AiContentPipeline()

    private val _subjects = MutableStateFlow(SampleData.sampleSubjects)
    val subjects: StateFlow<List<Subject>> = _subjects.asStateFlow()

    private val _selectedSubjectId = MutableStateFlow("math_vectors")
    val selectedSubjectId: StateFlow<String> = _selectedSubjectId.asStateFlow()

    private val _userStats = MutableStateFlow(UserStats())
    val userStats: StateFlow<UserStats> = _userStats.asStateFlow()

    private val _currentScreen = MutableStateFlow(AppScreen.LEARNING_PATH)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Active Quiz Session State
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

    // AI Ingestion list
    private val _ingestedItems = MutableStateFlow<List<ResourceIngestionItem>>(
        listOf(
            ResourceIngestionItem(
                id = "item_1",
                title = "Cours_Matrice_Et_Probabilites.pdf",
                sourceType = IngestionSourceType.PDF_DOCUMENT,
                sourceUrlOrPath = "/docs/math_matrices.pdf",
                status = "COMPLETED",
                generatedLessonsCount = 2,
                targetSubjectId = "math_vectors"
            ),
            ResourceIngestionItem(
                id = "item_2",
                title = "TCF Canada Expression Orale & Ecrite Tips",
                sourceType = IngestionSourceType.YOUTUBE_VIDEO,
                sourceUrlOrPath = "https://youtube.com/watch?v=tcf_canada_guide",
                status = "COMPLETED",
                generatedLessonsCount = 1,
                targetSubjectId = "lang_tcf_canada"
            )
        )
    )
    val ingestedItems: StateFlow<List<ResourceIngestionItem>> = _ingestedItems.asStateFlow()

    private val _ingestionProgressText = MutableStateFlow("")
    val ingestionProgressText: StateFlow<String> = _ingestionProgressText.asStateFlow()

    fun selectSubject(subjectId: String) {
        _selectedSubjectId.value = subjectId
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun startLesson(lesson: Lesson) {
        _activeLesson.value = lesson
        _currentExerciseIndex.value = 0
        _selectedOptionIndex.value = null
        _selectedTokenSequence.value = emptyList()
        _isAnswerChecked.value = false
        _isAnswerCorrect.value = false
        _isLessonCompleted.value = false
        _currentScreen.value = AppScreen.LESSON_QUIZ
    }

    fun selectOption(index: Int) {
        if (!_isAnswerChecked.value) {
            _selectedOptionIndex.value = index
        }
    }

    fun toggleTokenSelection(token: String) {
        if (_isAnswerChecked.value) return
        val current = _selectedTokenSequence.value.toMutableList()
        if (current.contains(token)) {
            current.remove(token)
        } else {
            current.add(token)
        }
        _selectedTokenSequence.value = current
    }

    fun checkAnswer() {
        val lesson = _activeLesson.value ?: return
        val exercise = lesson.exercises.getOrNull(_currentExerciseIndex.value) ?: return

        var correct = false
        when (exercise) {
            is Exercise.MultipleChoice -> {
                correct = _selectedOptionIndex.value == exercise.correctOptionIndex
            }
            is Exercise.CodeChallenge -> {
                correct = _selectedOptionIndex.value == exercise.correctOptionIndex
            }
            is Exercise.MathChallenge -> {
                correct = _selectedOptionIndex.value == exercise.correctOptionIndex
            }
            is Exercise.Translation -> {
                correct = _selectedTokenSequence.value == exercise.correctSequence
            }
            else -> correct = true
        }

        _isAnswerCorrect.value = correct
        _isAnswerChecked.value = true

        if (!correct) {
            // Lose 1 heart
            val currentHearts = _userStats.value.hearts
            if (currentHearts > 0) {
                _userStats.value = _userStats.value.copy(hearts = currentHearts - 1)
            }
        }
    }

    fun nextExercise() {
        val lesson = _activeLesson.value ?: return
        if (_currentExerciseIndex.value + 1 < lesson.exercises.size) {
            _currentExerciseIndex.value += 1
            _selectedOptionIndex.value = null
            _selectedTokenSequence.value = emptyList()
            _isAnswerChecked.value = false
            _isAnswerCorrect.value = false
        } else {
            // Lesson completed!
            _isLessonCompleted.value = true
            val newXp = _userStats.value.xp + lesson.xpReward
            val updatedCompleted = _userStats.value.completedLessonIds + lesson.id
            _userStats.value = _userStats.value.copy(
                xp = newXp,
                completedLessonIds = updatedCompleted
            )
            markLessonCompletedInSubject(lesson.id)
        }
    }

    private fun markLessonCompletedInSubject(lessonId: String) {
        val updatedSubjects = _subjects.value.map { subject ->
            val updatedUnits = subject.units.map { unit ->
                val updatedLessons = unit.lessons.map { l ->
                    if (l.id == lessonId) l.copy(isCompleted = true) else l
                }
                unit.copy(lessons = updatedLessons)
            }
            subject.copy(units = updatedUnits)
        }
        _subjects.value = updatedSubjects
    }

    suspend fun importResourceFromAi(sourceUrlOrPath: String, sourceType: IngestionSourceType) {
        val targetSubject = _selectedSubjectId.value
        val newLesson = aiPipeline.processResource(sourceUrlOrPath, sourceType, targetSubject) { status ->
            _ingestionProgressText.value = status
        }

        // Add to subject
        val updatedSubjects = _subjects.value.map { subject ->
            if (subject.id == targetSubject && subject.units.isNotEmpty()) {
                val firstUnit = subject.units.first()
                val updatedFirstUnit = firstUnit.copy(lessons = firstUnit.lessons + newLesson)
                subject.copy(units = listOf(updatedFirstUnit) + subject.units.drop(1))
            } else subject
        }
        _subjects.value = updatedSubjects

        // Add to ingestion history
        val newItem = ResourceIngestionItem(
            id = "item_" + System.currentTimeMillis(),
            title = if (sourceType == IngestionSourceType.YOUTUBE_VIDEO) "Vidéo YouTube" else "Fichier PDF",
            sourceType = sourceType,
            sourceUrlOrPath = sourceUrlOrPath,
            status = "COMPLETED",
            generatedLessonsCount = 1,
            targetSubjectId = targetSubject
        )
        _ingestedItems.value = listOf(newItem) + _ingestedItems.value
        _ingestionProgressText.value = ""
    }
}
