package com.duolingo.app

import com.duolingo.app.data.generated.GeneratedCourses
import com.duolingo.app.model.*
import com.duolingo.app.storage.ProgressStore
import com.duolingo.app.viewmodel.AppViewModel
import kotlin.test.*

class AuthoredCurriculumTest {
    private class Store : ProgressStore {
        private var value: String? = null
        override fun read() = value
        override fun write(value: String) { this.value = value }
    }

    @Test fun everyPublishedSourceSessionIsReachableAndCompletable() {
        val allIds = mutableSetOf<String>()
        var courses = 0
        SubjectCategory.entries.forEach { category ->
            GeneratedCourses.unitsFor(category).forEach { unit ->
                courses++
                assertEquals(unit.expectedLessonCount, unit.lessons.size, unit.id)
                assertEquals((1..unit.lessons.size).toList(), unit.lessons.map { it.sourceSessionNumber }, unit.id)
                val subject = Subject("test", unit.title, "", "", category, listOf(unit))
                val store = Store()
                val model = AppViewModel(store, { "2026-10-19" }, listOf(subject))
                var expectedXp = 0
                unit.lessons.forEach { lesson ->
                    assertTrue(allIds.add(lesson.id), "Duplicate lesson ${lesson.id}")
                    assertTrue(subject.isLessonUnlocked(lesson, model.userStats.value.completedLessonIds), lesson.id)
                    model.startLesson(lesson)
                    assertFalse(model.isLessonCompleted.value, lesson.id)
                    assertTrue(lesson.exercises.isNotEmpty(), lesson.id)
                    lesson.exercises.forEach { exercise ->
                        assertTrue(allIds.add(exercise.id), "Duplicate exercise ${exercise.id}")
                        val answer = when (exercise) {
                            is Exercise.ReadingChoice -> {
                                assertTrue(exercise.passage.isNotBlank(), "Missing reading passage ${exercise.id}")
                                exercise.correctOptionIndex
                            }
                            is Exercise.SpokenProduction -> {
                                // Business-state test only: native microphone/file/playback is checked separately.
                                model.confirmSpokenRecording(exercise.id, exercise.minimumPracticeSeconds * 1000L)
                                model.markRecordingReplayed(exercise.id)
                                exercise.reviewCriteria.indices.forEach(model::toggleReviewCriterion)
                                null
                            }
                            is Exercise.ListeningChoice -> {
                                model.markAudioCompleted(exercise.id)
                                exercise.correctOptionIndex
                            }
                            is Exercise.MultipleChoice -> exercise.correctOptionIndex
                            is Exercise.CodeChallenge -> exercise.correctOptionIndex
                            is Exercise.MathChallenge -> exercise.correctOptionIndex
                            is Exercise.WrittenProduction -> {
                                model.updateWrittenResponse(exercise.exampleResponse)
                                exercise.reviewCriteria.indices.forEach(model::toggleReviewCriterion)
                                null
                            }
                            else -> fail("Unhandled published exercise ${exercise.id}")
                        }
                        if (answer != null) model.selectOption(answer)
                        model.checkAnswer()
                        assertTrue(model.isAnswerCorrect.value, exercise.id)
                        model.nextExercise()
                    }
                    assertTrue(model.isLessonCompleted.value, lesson.id)
                    expectedXp += lesson.xpReward
                    assertEquals(expectedXp, model.userStats.value.xp, lesson.id)
                }
                val reopened = AppViewModel(store, { "2026-10-19" }, listOf(subject))
                assertEquals(unit.lessons.map { it.id }.toSet(), reopened.userStats.value.completedLessonIds)
                assertEquals(expectedXp, reopened.userStats.value.xp)
            }
        }
        assertTrue(courses > 0, "An inventory alone is not an authored course")
    }
}
