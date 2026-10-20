package com.duolingo.app

import com.duolingo.app.model.*
import com.duolingo.app.storage.ProgressStore
import com.duolingo.app.viewmodel.AppViewModel
import kotlin.test.*

class DeferredReviewTest {
    private val questions = (0..2).map { n ->
        Exercise.MultipleChoice("q$n", "Question $n", listOf("Juste", "Faux"), 0, "Correction $n", "Indice")
    }
    private val lesson = Lesson("review", "Reprise différée", isLocked = false, exercises = questions)
    private fun model() = AppViewModel(object : ProgressStore {
        override fun read(): String? = null
        override fun write(value: String) = Unit
    }, { "2026-10-20" }, listOf(Subject("math", "Maths", "", "", SubjectCategory.MATHEMATICS,
        listOf(LearningUnit("course", 1, "Cours", "", listOf(lesson))))))
    private fun answer(vm: AppViewModel, option: Int) {
        vm.selectOption(option); vm.checkAnswer(); vm.nextExercise()
    }

    @Test fun wrongAnswersAdvanceAndReturnAfterAllOriginalQuestions() {
        val vm = model()
        try {
            vm.startLesson(lesson)
            answer(vm, 1)
            assertEquals(1, vm.currentExerciseIndex.value)
            assertEquals(4, vm.userStats.value.hearts)
            answer(vm, 0)
            assertEquals(2, vm.currentExerciseIndex.value)
            answer(vm, 1)
            assertEquals(0, vm.currentExerciseIndex.value)
            assertFalse(vm.isLessonCompleted.value)
            assertEquals(0, vm.userStats.value.xp)
            answer(vm, 0)
            assertEquals(2, vm.currentExerciseIndex.value)
            answer(vm, 0)
            assertTrue(vm.isLessonCompleted.value)
            assertEquals(15, vm.userStats.value.xp)
            vm.nextExercise()
            assertEquals(15, vm.userStats.value.xp)
        } finally { vm.close() }
    }

    @Test fun anotherWrongAnswerInReviewCyclesBehindOtherMissedQuestions() {
        val vm = model()
        try {
            vm.startLesson(lesson)
            answer(vm, 1); answer(vm, 1); answer(vm, 0)
            assertEquals(0, vm.currentExerciseIndex.value)
            answer(vm, 1)
            assertEquals(1, vm.currentExerciseIndex.value)
            answer(vm, 0)
            assertEquals(0, vm.currentExerciseIndex.value)
            assertFalse(vm.isLessonCompleted.value)
            answer(vm, 0)
            assertTrue(vm.isLessonCompleted.value)
            assertEquals(2, vm.userStats.value.hearts)
        } finally { vm.close() }
    }

    @Test fun checksAreIdempotentAndRestartClearsTheAttemptQueue() {
        val vm = model()
        try {
            vm.startLesson(lesson)
            vm.selectOption(1); vm.checkAnswer(); vm.checkAnswer()
            assertEquals(4, vm.userStats.value.hearts)
            vm.startLesson(lesson)
            answer(vm, 0); answer(vm, 0); answer(vm, 0)
            assertTrue(vm.isLessonCompleted.value)
            assertEquals(15, vm.userStats.value.xp)
        } finally { vm.close() }
    }

    @Test fun aCorrectImmediateRetryRemovesAnAlreadyQueuedError() {
        val vm = model()
        try {
            vm.startLesson(lesson)
            vm.selectOption(1); vm.checkAnswer()
            vm.retryExercise()
            vm.selectOption(0); vm.checkAnswer()
            assertTrue(vm.reviewQuestionIndices.value.isEmpty())
            vm.nextExercise()
            answer(vm, 0); answer(vm, 0)
            assertTrue(vm.isLessonCompleted.value)
        } finally { vm.close() }
    }

    @Test fun aReviewedListeningQuestionPreservesTheHeardDocumentAndTextMode() {
        listOf(false, true).forEach { textMode ->
            val audio = Exercise.ListeningChoice("audio", "Écouter", "files/audio/test.wav", "Document", listOf("Oui", "Non"), 0, "Oui", "Indice")
            val attempt = lesson.copy(exercises = listOf(audio, questions[1]))
            val vm = AppViewModel(object : ProgressStore {
                override fun read(): String? = null
                override fun write(value: String) = Unit
            }, { "2026-10-20" }, listOf(Subject("math", "Maths", "", "", SubjectCategory.LANGUAGES,
                listOf(LearningUnit("course", 1, "Écoute", "", listOf(attempt))))))
            try {
                vm.startLesson(attempt)
                if (textMode) vm.enableTranscriptMode(audio.id) else vm.markAudioCompleted(audio.id)
                answer(vm, 1); answer(vm, 0)
                assertEquals(0, vm.currentExerciseIndex.value)
                assertEquals(0, vm.userStats.value.xp)
                if (textMode) assertTrue(audio.audioResourcePath in vm.transcriptModePaths.value)
                else assertTrue(audio.audioResourcePath in vm.heardAudioPaths.value)
                answer(vm, 0)
                assertTrue(vm.isLessonCompleted.value)
                assertEquals(textMode, attempt.id in vm.userStats.value.textModeCompletedLessonIds)
            } finally { vm.close() }
        }
    }
}
