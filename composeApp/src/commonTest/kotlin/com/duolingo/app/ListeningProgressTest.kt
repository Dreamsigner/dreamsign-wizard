package com.duolingo.app

import com.duolingo.app.model.*
import com.duolingo.app.storage.ProgressStore
import com.duolingo.app.viewmodel.AppViewModel
import kotlin.test.*

class ListeningProgressTest {
    private val exercise = Exercise.ListeningChoice("audio1", "Que dit le document ?", "files/audio/fixture.wav",
        "Bonjour.", listOf("Bonjour", "Au revoir"), 0, "Une salutation.", "Écouter.")
    private val lesson = Lesson("listen", "Écoute", isLocked = false, exercises = listOf(exercise, exercise.copy(id = "audio2")))
    private fun model(): AppViewModel {
        val subject = Subject("audio", "Écoute", "", "", SubjectCategory.LANGUAGES,
            listOf(LearningUnit("audio-unit", 1, "Écoute", "", listOf(lesson))))
        return AppViewModel(object : ProgressStore {
            override fun read(): String? = null
            override fun write(value: String) = Unit
        }, { "2026-10-20" }, listOf(subject))
    }

    @Test fun answersRequireActualCompletionEventForTheCurrentAudio() {
        val vm = model()
        vm.startLesson(lesson)
        vm.selectOption(0)
        vm.checkAnswer()
        assertFalse(vm.isAnswerChecked.value)
        vm.markAudioCompleted("other-exercise")
        vm.checkAnswer()
        assertFalse(vm.isAnswerChecked.value)
        vm.markAudioCompleted(exercise.id)
        vm.checkAnswer()
        assertTrue(vm.isAnswerCorrect.value)
        vm.nextExercise()
        vm.selectOption(0)
        vm.checkAnswer()
        assertTrue(vm.isAnswerCorrect.value, "Second question may use the same already-heard document")
        vm.nextExercise()
        assertTrue(vm.isLessonCompleted.value)
        vm.startLesson(lesson)
        assertTrue(vm.heardAudioPaths.value.isEmpty(), "A replay starts a fresh listening attempt")
        vm.close()
    }

    @Test fun accessibleTextModeIsNotRecordedAsListening() {
        val vm = model()
        vm.startLesson(lesson)
        vm.enableTranscriptMode(exercise.id)
        repeat(2) {
            vm.selectOption(0)
            vm.checkAnswer()
            vm.nextExercise()
        }
        assertTrue(vm.heardAudioPaths.value.isEmpty())
        assertTrue(lesson.id in vm.userStats.value.textModeCompletedLessonIds)
        vm.startLesson(lesson)
        vm.markAudioCompleted(exercise.id)
        repeat(2) { vm.selectOption(0); vm.checkAnswer(); vm.nextExercise() }
        assertTrue(vm.userStats.value.textModeCompletedLessonIds.isEmpty())
        assertEquals(0, vm.lastAwardedXp.value)
        vm.close()
    }
}
