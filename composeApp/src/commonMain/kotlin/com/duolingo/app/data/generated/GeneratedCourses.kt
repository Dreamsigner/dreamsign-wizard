// Generated course registry.
package com.duolingo.app.data.generated

import com.duolingo.app.model.*

object GeneratedCourses {
    fun unitsFor(category: SubjectCategory): List<LearningUnit> = when (category) {
        SubjectCategory.MATHEMATICS -> listOf(Course_mit_18_01sc_2010.unit())
        SubjectCategory.COMPUTER_SCIENCE -> listOf()
        SubjectCategory.LANGUAGES -> listOf()
    }
}
