package com.dreamsigner.wizard.ui.math

import com.dreamsigner.wizard.data.SampleData
import kotlin.test.Test
import kotlin.test.assertTrue

class AuthoredFractionNotationTest {
    private val calculus = SampleData.sampleSubjects.flatMap { it.units }.first { it.id == "mit_18_01sc_2010" }

    @Test fun secantDifferenceQuotientIsAuthoredAsOneLatexFraction() {
        val math = parseLearningText(calculus.lessons[0].learningNotes, true).filterIsInstance<LearningTextSegment.Math>()
        assertTrue(math.any { "\\frac{f(a+h)-f(a)}{h}" in it.latex }, "The whole numerator must belong to the fraction")
    }

    @Test fun reciprocalDerivativeHasTwoTrueFractionsAndASquaredDenominator() {
        val math = parseLearningText(calculus.lessons[1].learningNotes, true).filterIsInstance<LearningTextSegment.Math>()
        assertTrue(math.any { "\\frac{1}{x}" in it.latex && "-\\frac{1}{x^{2}}" in it.latex }, "Do not leave 1/x as prose with a separately rendered power")
    }
}
