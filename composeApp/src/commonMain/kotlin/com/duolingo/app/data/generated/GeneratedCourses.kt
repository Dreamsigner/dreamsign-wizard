// Generated course registry.
package com.duolingo.app.data.generated

import com.duolingo.app.model.*

object GeneratedCourses {
    fun unitsFor(category: SubjectCategory): List<LearningUnit> = when (category) {
        SubjectCategory.MATHEMATICS -> listOf(Course_mit_18_01sc_2010.unit(), Course_mit_18_06sc_2011.unit())
        SubjectCategory.COMPUTER_SCIENCE -> listOf(Course_mit_missing_semester_2026.unit())
        SubjectCategory.LANGUAGES -> listOf(Course_tcf_canada_writing_practice_2026.unit())
    }
}
