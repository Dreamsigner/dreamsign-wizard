package com.duolingo.app

import com.duolingo.app.model.*
import com.duolingo.app.storage.ProgressStore
import com.duolingo.app.viewmodel.AppViewModel
import kotlin.test.*

class SpokenProductionTest {
    private class Store : ProgressStore {
        var value: String? = null
        override fun read() = value
        override fun write(value: String) { this.value = value }
    }
    private val exercise = Exercise.SpokenProduction(id = "speak1", prompt = "Présente-toi.",
        maxDurationSeconds = 120, reviewCriteria = listOf("Clarté", "Exemple"),
        exampleOutline = "Activité puis exemple", explanation = "Auto-évaluation", hint = "Parle naturellement.")
    private val lesson = Lesson("lspeak", "Oral", isLocked = false, exercises = listOf(exercise))
    private val subject = Subject("oral", "TCF", "", "", SubjectCategory.LANGUAGES,
        listOf(LearningUnit("s", 1, "Atelier", "", listOf(lesson))))
    private fun model(store: Store) = AppViewModel(store, { "2026-10-20" }, listOf(subject))

    @Test fun requiresValidatedRecordingFullReplayAndReview() {
        val vm = model(Store())
        vm.startLesson(lesson)
        exercise.reviewCriteria.indices.forEach(vm::toggleReviewCriterion)
        vm.checkAnswer()
        assertFalse(vm.isAnswerChecked.value)
        vm.confirmSpokenRecording(exercise.id, 5000)
        assertTrue(vm.reviewedCriteria.value.isEmpty())
        exercise.reviewCriteria.indices.forEach(vm::toggleReviewCriterion)
        vm.checkAnswer()
        assertFalse(vm.isAnswerChecked.value)
        vm.markRecordingReplayed("stale_exercise")
        assertFalse(vm.recordingReplayed.value)
        vm.markRecordingReplayed(exercise.id)
        vm.checkAnswer()
        vm.nextExercise()
        assertTrue(vm.isLessonCompleted.value)
        vm.close()
    }

    @Test fun replacingOrCancellingCaptureRequiresAnotherReplayAndReview() {
        val vm = model(Store())
        vm.startLesson(lesson)
        vm.confirmSpokenRecording(exercise.id, 7000)
        vm.markRecordingReplayed(exercise.id)
        exercise.reviewCriteria.indices.forEach(vm::toggleReviewCriterion)
        vm.setSpokenRecordingBusy(exercise.id, true)
        assertFalse(vm.recordingReplayed.value)
        assertTrue(vm.reviewedCriteria.value.isEmpty())
        vm.markRecordingReplayed(exercise.id)
        assertFalse(vm.recordingReplayed.value)
        vm.checkAnswer()
        assertFalse(vm.isAnswerChecked.value)
        vm.setSpokenRecordingBusy(exercise.id, false)
        assertEquals(7000L, vm.verifiedRecordingMillis.value)
        assertFalse(vm.recordingReplayed.value)
        vm.close()
    }

    @Test fun persistedDurationDoesNotReplaceAValidatedFileOnRestart() {
        val store = Store()
        val vm = model(store)
        vm.startLesson(lesson)
        vm.confirmSpokenRecording("stale", 6000)
        assertNull(vm.verifiedRecordingMillis.value)
        vm.confirmSpokenRecording(exercise.id, 121000)
        assertNull(vm.verifiedRecordingMillis.value)
        vm.confirmSpokenRecording(exercise.id, 6000)
        vm.close()
        val reopened = model(store)
        reopened.startLesson(lesson)
        assertEquals(6000L, reopened.userStats.value.spokenRecordingDurationsMillis[exercise.id])
        assertNull(reopened.verifiedRecordingMillis.value)
        reopened.markRecordingReplayed(exercise.id)
        assertFalse(reopened.recordingReplayed.value)
        reopened.close()
    }

    @Test fun technicalMinimumIsNotAnOfficialOralScore() {
        assertFalse(exercise.canFinishPractice(4999, true, false, setOf(0, 1)))
        assertFalse(exercise.canFinishPractice(120001, true, false, setOf(0, 1)))
        assertFalse(exercise.canFinishPractice(5000, false, false, setOf(0, 1)))
        assertFalse(exercise.canFinishPractice(5000, true, true, setOf(0, 1)))
        assertFalse(exercise.canFinishPractice(5000, true, false, setOf(0)))
        assertTrue(exercise.canFinishPractice(5000, true, false, setOf(0, 1)))
    }
}
