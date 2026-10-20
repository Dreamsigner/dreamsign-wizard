// Generated course registry.
package com.duolingo.app.data.generated

import com.duolingo.app.model.*

object GeneratedCourses {
    fun unitsFor(category: SubjectCategory): List<LearningUnit> = when (category) {
        SubjectCategory.MATHEMATICS -> listOf(Course_mit_18_01sc_2010.unit(), Course_mit_18_06sc_2011.unit(), Course_mit_18_335j_2019.unit(), Course_mit_6_042j_2010.unit())
        SubjectCategory.COMPUTER_SCIENCE -> listOf(Course_learn_prolog_now_2026.unit(), Course_mit_18_404j_2020.unit(), Course_mit_6_004_2017.unit(), Course_mit_6_100l_2022.unit(), Course_mit_missing_semester_2026.unit())
        SubjectCategory.LANGUAGES -> listOf(Course_tcf_canada_listening_practice_2026.unit(), Course_tcf_canada_reading_practice_2026.unit(), Course_tcf_canada_spoken_practice_2026.unit(), Course_tcf_canada_writing_practice_2026.unit())
    }
}
