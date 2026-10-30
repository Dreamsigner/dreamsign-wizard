package com.dreamsigner.wizard

import com.dreamsigner.wizard.model.*
import com.dreamsigner.wizard.storage.ProgressStore
import com.dreamsigner.wizard.viewmodel.AppViewModel
import kotlin.test.*

class WrittenProductionTest {
    private class Store : ProgressStore {
        var value: String? = null
        override fun read() = value
        override fun write(value: String) { this.value = value }
    }
    private val exercise = Exercise.WrittenProduction("write1", "Rédiger", 3, 6,
        listOf("But respecté", "Langue relue"), "Bonjour, voici mon texte.", "Relire avec un professeur.", "Organiser.")
    private val lesson = Lesson("lwrite", "Écriture", isLocked = false, exercises = listOf(exercise))
    private val subject = Subject("writing", "TCF", "", "", SubjectCategory.LANGUAGES,
        listOf(LearningUnit("w", 1, "Atelier", "", listOf(lesson))))
    private fun model(store: Store) = AppViewModel(store, { "2026-10-19" }, listOf(subject))

    @Test fun productionRequiresTextAndExplicitReviewAndSurvivesRestart() {
        val store = Store()
        val vm = model(store)
        vm.startLesson(lesson)
        vm.checkAnswer()
        assertFalse(vm.isAnswerChecked.value)
        vm.updateWrittenResponse("Bonjour, voici mon texte.")
        vm.toggleReviewCriterion(0)
        vm.checkAnswer()
        assertFalse(vm.isAnswerChecked.value)
        vm.toggleReviewCriterion(1)
        vm.updateWrittenResponse("Bonjour, voici mon nouveau texte.")
        assertTrue(vm.reviewedCriteria.value.isEmpty())
        vm.toggleReviewCriterion(0)
        vm.toggleReviewCriterion(1)
        vm.checkAnswer()
        vm.nextExercise()
        assertTrue(vm.isLessonCompleted.value)
        val reopened = model(store)
        assertEquals("Bonjour, voici mon nouveau texte.", reopened.userStats.value.writtenDrafts[exercise.id])
        assertTrue(lesson.id in reopened.userStats.value.completedLessonIds)
    }

    @Test fun outOfRangeTextCannotCompleteEvenWithAllChecks() {
        assertFalse(exercise.canFinishPractice("", setOf(0, 1)))
        assertFalse(exercise.canFinishPractice("Un deux", setOf(0, 1)))
        assertFalse(exercise.canFinishPractice("Un deux trois quatre cinq six sept", setOf(0, 1)))
        assertEquals(4, practiceWordCount("  un  deux\ntrois\tquatre  "))
        assertTrue(exercise.canFinishPractice("Un deux trois", setOf(0, 1)))
    }

    @Test fun closingFlushesTheLatestDraftWithoutCompletingTheExercise() {
        val store = Store()
        val vm = model(store)
        vm.startLesson(lesson)
        repeat(30) { vm.updateWrittenResponse("Brouillon numéro $it encore ouvert.") }
        vm.close()
        val reopened = model(store)
        assertEquals("Brouillon numéro 29 encore ouvert.", reopened.userStats.value.writtenDrafts[exercise.id])
        assertTrue(reopened.userStats.value.completedLessonIds.isEmpty())
        reopened.close()
    }
}
