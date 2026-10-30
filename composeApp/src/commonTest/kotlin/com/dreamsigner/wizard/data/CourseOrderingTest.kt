package com.dreamsigner.wizard.data

import com.dreamsigner.wizard.model.LearningUnit
import com.dreamsigner.wizard.model.SubjectCategory
import com.dreamsigner.wizard.data.generated.GeneratedCourses
import kotlin.test.Test
import kotlin.test.assertEquals

class CourseOrderingTest {
    @Test
    fun mathCoursesFollowOssuSectionsAndKeepUnmappedCoursesInTheirOriginalOrder() {
        val units = listOf(
            unit("mit_18_335j_2019"),
            unit("extra_z"),
            unit("forallx_calgary_2025"),
            unit("mit_18_06sc_2011"),
            unit("extra_a"),
            unit("mit_6_042j_2010"),
            unit("mit_18_01sc_2010"),
        )

        assertEquals(
            listOf(
                "mit_18_01sc_2010",
                "mit_6_042j_2010",
                "mit_18_06sc_2011",
                "forallx_calgary_2025",
                "mit_18_335j_2019",
                "extra_z",
                "extra_a",
            ),
            CourseOrdering.order(units).map { it.id },
        )
    }

    @Test
    fun computerScienceCoursesFollowOssuSectionsInsteadOfFilenameOrder() {
        val units = listOf(
            unit("roughgarden_algorithmic_game_theory_2013"),
            unit("mit_18_404j_2020"),
            unit("learn_prolog_now_2026"),
            unit("mit_6_004_2017"),
            unit("mit_missing_semester_2026"),
            unit("mit_6_100l_2022"),
        )

        assertEquals(
            listOf(
                "mit_6_100l_2022",
                "mit_missing_semester_2026",
                "mit_6_004_2017",
                "learn_prolog_now_2026",
                "mit_18_404j_2020",
                "roughgarden_algorithmic_game_theory_2013",
            ),
            CourseOrdering.order(units).map { it.id },
        )
    }

    @Test
    fun publishedRegistryUsesTheExplicitOrderForEachOssuCategory() {
        assertEquals(
            listOf("mit_18_01sc_2010", "mit_6_042j_2010", "mit_18_06sc_2011", "forallx_calgary_2025", "mit_18_335j_2019"),
            CourseOrdering.order(GeneratedCourses.unitsFor(SubjectCategory.MATHEMATICS)).map { it.id },
        )
        assertEquals(
            listOf("mit_6_100l_2022", "mit_missing_semester_2026", "mit_6_004_2017", "learn_prolog_now_2026", "mit_18_404j_2020", "roughgarden_algorithmic_game_theory_2013"),
            CourseOrdering.order(GeneratedCourses.unitsFor(SubjectCategory.COMPUTER_SCIENCE)).map { it.id },
        )
        assertEquals(
            listOf("tcf_canada_listening_practice_2026", "tcf_canada_reading_practice_2026", "tcf_canada_spoken_practice_2026", "tcf_canada_writing_practice_2026"),
            CourseOrdering.order(GeneratedCourses.unitsFor(SubjectCategory.LANGUAGES)).map { it.id },
        )
    }

    private fun unit(id: String) = LearningUnit(
        id = id,
        unitNumber = 1,
        title = id,
        description = "test",
        lessons = emptyList(),
    )
}
