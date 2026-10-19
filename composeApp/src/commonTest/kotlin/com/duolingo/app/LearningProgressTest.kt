package com.duolingo.app

import com.duolingo.app.model.*
import com.duolingo.app.storage.ProgressStore
import com.duolingo.app.viewmodel.AppViewModel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.*

class LearningProgressTest {
    private class MemoryStore(var value: String? = null) : ProgressStore {
        var writes = 0
        var fail = false
        override fun read() = value
        override fun write(value: String) {
            if (fail) error("disk unavailable")
            writes++
            this.value = value
        }
    }

    private val first = Lesson("a", "Première", isLocked = false, exercises = listOf(
        Exercise.MultipleChoice("a1", "2 + 3 ?", listOf("5", "6"), 0, "2+3=5", "Additionne.")))
    private val second = first.copy(id = "b", title = "Suite", exercises = listOf(
        Exercise.MultipleChoice("b1", "3+4 ?", listOf("7", "8"), 0, "3+4=7", "Additionne.")))
    private val other = first.copy(id = "c", title = "Autre cours")
    private val subject = Subject("math_vectors", "Maths", "M", "", SubjectCategory.MATHEMATICS,
        listOf(LearningUnit("u1", 1, "Cours 1", "", listOf(first, second)), LearningUnit("u2", 2, "Cours 2", "", listOf(other))))

    private fun vm(store: MemoryStore = MemoryStore(), today: String = "2026-10-19") =
        AppViewModel(store, { today }, listOf(subject))

    private fun complete(viewModel: AppViewModel, lesson: Lesson = first) {
        viewModel.startLesson(lesson)
        viewModel.selectOption(0)
        viewModel.checkAnswer()
        viewModel.nextExercise()
    }

    @Test fun completionSurvivesRestartAndCannotFarmXp() {
        val store = MemoryStore()
        val one = vm(store)
        complete(one)
        assertEquals(15, one.userStats.value.xp)
        assertEquals(15, one.lastAwardedXp.value)
        val restarted = vm(store)
        assertTrue("a" in restarted.userStats.value.completedLessonIds)
        complete(restarted)
        assertEquals(15, restarted.userStats.value.xp)
        assertEquals(0, restarted.lastAwardedXp.value)
        restarted.nextExercise()
        assertEquals(15, restarted.userStats.value.xp)
    }

    @Test fun incorrectAnswersMustBeRetriedAndCheckIsIdempotent() {
        val one = vm()
        one.startLesson(first)
        one.selectOption(1)
        one.checkAnswer()
        one.checkAnswer()
        assertEquals(4, one.userStats.value.hearts)
        one.nextExercise()
        assertFalse(one.isLessonCompleted.value)
        one.retryExercise()
        assertNull(one.selectedOptionIndex.value)
        one.selectOption(0)
        one.checkAnswer()
        one.nextExercise()
        assertTrue(one.isLessonCompleted.value)
    }

    @Test fun coursesStartIndependentlyButWithinCourseOrderIsEnforced() {
        assertTrue(subject.isLessonUnlocked(first, emptySet()))
        assertFalse(subject.isLessonUnlocked(second, emptySet()))
        assertTrue(subject.isLessonUnlocked(other, emptySet()))
        val one = vm()
        one.startLesson(second)
        assertNull(one.activeLesson.value)
        one.startLesson(other)
        assertEquals("c", one.activeLesson.value?.id)
    }

    @Test fun outOfRangeOrMissingAnswersDoNotCount() {
        val one = vm()
        one.startLesson(first)
        one.checkAnswer()
        one.nextExercise()
        assertFalse(one.isAnswerChecked.value)
        one.selectOption(99)
        assertNull(one.selectedOptionIndex.value)
        assertTrue(one.userStats.value.completedLessonIds.isEmpty())
    }

    @Test fun unsupportedExercisesCannotPassOrEarnXp() {
        val unsupported = first.copy(id = "pairs", exercises = listOf(
            Exercise.MatchingPairs("p1", "Associer", mapOf("a" to "b"), "a correspond à b")))
        val cat = subject.copy(units = listOf(LearningUnit("p", 1, "Paires", "", listOf(unsupported))))
        val one = AppViewModel(MemoryStore(), { "2026-10-19" }, listOf(cat))
        one.startLesson(unsupported)
        one.checkAnswer()
        one.nextExercise()
        assertFalse(one.isAnswerChecked.value)
        assertEquals(0, one.userStats.value.xp)
    }

    @Test fun corruptSaveIsPreservedAndReported() {
        val store = MemoryStore("{not valid json")
        val one = vm(store)
        assertEquals("{not valid json", store.value)
        assertEquals(0, store.writes)
        assertTrue(one.persistenceStatus.value.contains("illisible"))
        complete(one)
        assertEquals("{not valid json", store.value)
        assertEquals(0, store.writes)
    }

    @Test fun writeFailureIsVisibleWithoutDiscardingSessionProgress() {
        val store = MemoryStore().also { it.fail = true }
        val one = vm(store)
        complete(one)
        assertEquals(15, one.userStats.value.xp)
        assertTrue(one.persistenceStatus.value.contains("session"))
    }

    @Test fun streakUsesRealLocalDaysIncludingLeapDay() {
        val store = MemoryStore(Json.encodeToString(UserStats(streakDays = 3, lastStudyDate = "2024-02-28")))
        complete(vm(store, "2024-02-29"))
        assertEquals(4, vm(store, "2024-02-29").userStats.value.streakDays)
        val next = vm(store, "2024-03-01")
        complete(next)
        assertEquals(5, next.userStats.value.streakDays)
        complete(vm(store, "2024-03-04"))
        assertEquals(1, vm(store, "2024-03-04").userStats.value.streakDays)
    }

    @Test fun invalidCalendarDatesAreNotAccepted() {
        assertNull(localDayOrdinal("1900-02-29"))
        assertNull(localDayOrdinal("2026-13-01"))
        assertNull(localDayOrdinal("2026-04-31"))
        assertEquals(1, localDayOrdinal("2000-03-01")!! - localDayOrdinal("2000-02-29")!!)
    }

    @Test fun expiredStreakIsZeroBeforeAnotherLesson() {
        val store = MemoryStore(Json.encodeToString(UserStats(streakDays = 8, lastStudyDate = "2026-10-16")))
        assertEquals(0, vm(store, "2026-10-19").userStats.value.streakDays)
        val yesterday = MemoryStore(Json.encodeToString(UserStats(streakDays = 8, lastStudyDate = "2026-10-18")))
        assertEquals(8, vm(yesterday, "2026-10-19").userStats.value.streakDays)
    }
}
