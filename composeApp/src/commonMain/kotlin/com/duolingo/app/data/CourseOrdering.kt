package com.duolingo.app.data

import com.duolingo.app.model.LearningUnit

/**
 * Stable order for authored OSSU courses. The source repositories present courses
 * top-to-bottom by curriculum section; generated registry order follows filenames,
 * so it must not decide the learner-facing sequence.
 */
internal object CourseOrdering {
    private val ossuOrder = mapOf(
        // OSSU Math: core calculus, discrete mathematics, linear algebra, then advanced topics.
        "mit_18_01sc_2010" to 10,
        "mit_6_042j_2010" to 20,
        "mit_18_06sc_2011" to 30,
        // Advanced topics: Mathematical Logic precedes Mathematical Analysis.
        "forallx_calgary_2025" to 40,
        "mit_18_335j_2019" to 50,

        // OSSU CS: Intro CS, CS Tools, Core Systems, then advanced sections.
        "mit_6_100l_2022" to 10,
        "mit_missing_semester_2026" to 20,
        "mit_6_004_2017" to 30,
        // Learn Prolog Now! is in Advanced Programming.
        "learn_prolog_now_2026" to 40,
        // MIT 18.404J and Roughgarden are both in Advanced Theory.
        "mit_18_404j_2020" to 50,
        "roughgarden_algorithmic_game_theory_2013" to 60,
    )

    fun order(units: List<LearningUnit>): List<LearningUnit> = units.withIndex()
        .sortedWith(compareBy<IndexedValue<LearningUnit>> { ossuOrder[it.value.id] ?: Int.MAX_VALUE }.thenBy { it.index })
        .map { it.value }
}
