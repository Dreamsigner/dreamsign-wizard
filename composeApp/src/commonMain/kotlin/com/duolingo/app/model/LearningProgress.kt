package com.duolingo.app.model

/** Every course starts independently; explicit lesson prerequisites remain authoritative. */
fun Subject.isLessonUnlocked(lesson: Lesson, completedLessonIds: Set<String>): Boolean {
    val unit = units.firstOrNull { course -> course.lessons.any { it.id == lesson.id } } ?: return false
    val index = unit.lessons.indexOfFirst { it.id == lesson.id }
    val canonical = unit.lessons[index]
    val completed = completedLessonIds + units.flatMap { it.lessons }.filter { it.isCompleted }.map { it.id }
    if (canonical.id in completed) return true
    val prerequisites = canonical.prerequisiteLessonIds.ifEmpty {
        if (canonical.requiresPreviousLesson && index > 0) listOf(unit.lessons[index - 1].id) else emptyList()
    }
    return prerequisites.all { it in completed }
}

/** Gregorian ordinal for comparing local study dates, with no timezone or UTC-day guess. */
internal fun localDayOrdinal(value: String?): Int? {
    if (value == null || !Regex("\\d{4}-\\d{2}-\\d{2}").matches(value)) return null
    val year = value.substring(0, 4).toIntOrNull() ?: return null
    val month = value.substring(5, 7).toIntOrNull() ?: return null
    val day = value.substring(8, 10).toIntOrNull() ?: return null
    if (year < 1 || month !in 1..12) return null
    val leap = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
    val days = when (month) { 2 -> if (leap) 29 else 28; 4, 6, 9, 11 -> 30; else -> 31 }
    if (day !in 1..days) return null
    val y = year - if (month <= 2) 1 else 0
    val era = y / 400
    val yoe = y - era * 400
    val shiftedMonth = month + if (month > 2) -3 else 9
    val doy = (153 * shiftedMonth + 2) / 5 + day - 1
    return era * 146097 + yoe * 365 + yoe / 4 - yoe / 100 + doy
}
