package com.duolingo.app

import com.duolingo.app.data.SampleData
import com.duolingo.app.model.SubjectCategory
import com.duolingo.app.storage.ProgressStore
import com.duolingo.app.viewmodel.AppViewModel
import kotlin.test.*

class FavoriteLessonsTest {
    private val math = SampleData.sampleSubjects.first { it.category == SubjectCategory.MATHEMATICS }
    private val lessonId = math.units.first().lessons.first().id
    private fun vm(store: ProgressStore) = AppViewModel(store, { "2026-10-22" })
    private class Store(var value: String? = null, var fail: Boolean = false) : ProgressStore {
        var writes = 0
        override fun read() = value
        override fun write(value: String) { if (fail) error("disk unavailable"); this.value = value; writes++ }
    }

    @Test fun favoritesSurviveRestartAndRemovalWithoutAwardingProgress() {
        val store = Store()
        val first = vm(store)
        try {
            val before = first.userStats.value
            first.toggleMathLessonFavorite(lessonId)
            assertEquals(before.copy(favoriteLessonIds = setOf(lessonId)), first.userStats.value)
        } finally { first.close() }
        val restored = vm(store)
        try {
            assertEquals(setOf(lessonId), restored.userStats.value.favoriteLessonIds)
            restored.toggleMathLessonFavorite(lessonId)
            assertTrue(restored.userStats.value.favoriteLessonIds.isEmpty())
        } finally { restored.close() }
        val removed = vm(store)
        try { assertTrue(removed.userStats.value.favoriteLessonIds.isEmpty()) } finally { removed.close() }
    }

    @Test fun oldSaveKeepsXpHeartsAndCompletedLessonsWhenAddingAFavorite() {
        val store = Store("""{"xp":45,"hearts":3,"completedLessonIds":["legacy-lesson"]}""")
        val first = vm(store)
        try {
            assertTrue(first.userStats.value.favoriteLessonIds.isEmpty())
            first.toggleMathLessonFavorite(lessonId)
        } finally { first.close() }
        val restored = vm(store)
        try {
            assertEquals(45, restored.userStats.value.xp)
            assertEquals(3, restored.userStats.value.hearts)
            assertEquals(setOf("legacy-lesson"), restored.userStats.value.completedLessonIds)
            assertEquals(setOf(lessonId), restored.userStats.value.favoriteLessonIds)
        } finally { restored.close() }
    }

    @Test fun unknownAndNonMathLessonsAreRejectedWithoutWriting() {
        val store = Store()
        val first = vm(store)
        try {
            val other = SampleData.sampleSubjects.first { it.category != SubjectCategory.MATHEMATICS }.units.first().lessons.first().id
            first.toggleMathLessonFavorite("unknown")
            first.toggleMathLessonFavorite(other)
            assertTrue(first.userStats.value.favoriteLessonIds.isEmpty())
            assertEquals(0, store.writes)
        } finally { first.close() }
    }

    @Test fun unreadableOriginalIsNotOverwrittenByAFavorite() {
        val store = Store("{broken")
        val first = vm(store)
        try {
            first.toggleMathLessonFavorite(lessonId)
            assertEquals(setOf(lessonId), first.userStats.value.favoriteLessonIds)
            assertTrue(first.persistenceStatus.value.contains("original conservé"))
        } finally { first.close() }
        assertEquals("{broken", store.value)
        assertEquals(0, store.writes)
    }

    @Test fun failedSaveReportsSessionOnlyStateAndCanBeRetriedOnClose() {
        val store = Store(fail = true)
        val first = vm(store)
        first.toggleMathLessonFavorite(lessonId)
        assertEquals(setOf(lessonId), first.userStats.value.favoriteLessonIds)
        assertTrue(first.persistenceStatus.value.contains("session"))
        store.fail = false
        first.close()
        val restored = vm(store)
        try { assertEquals(setOf(lessonId), restored.userStats.value.favoriteLessonIds) } finally { restored.close() }
    }
}
