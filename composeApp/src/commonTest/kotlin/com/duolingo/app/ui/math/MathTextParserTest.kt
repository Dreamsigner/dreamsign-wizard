package com.duolingo.app.ui.math

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MathTextParserTest {
    @Test
    fun parsesInlineAndDisplayMathWithoutLosingSurroundingCopy() {
        assertEquals(
            listOf(
                LearningTextSegment.Text("Résoudre "),
                LearningTextSegment.Math("\\frac{x^2}{2}", display = false),
                LearningTextSegment.Text(" puis : "),
                LearningTextSegment.Math("\\sum_{i=1}^n i", display = true),
                LearningTextSegment.Text(" fin"),
            ),
            parseLearningText("Résoudre \$\\frac{x^2}{2}\$ puis : \$\$\\sum_{i=1}^n i\$\$ fin"),
        )
    }

    @Test
    fun unmatchedDelimiterRemainsOrdinaryText() {
        assertEquals(listOf(LearningTextSegment.Text("Texte \$ incomplet")), parseLearningText("Texte \$ incomplet"))
    }

    @Test
    fun legacyRecognitionOnlyTypesetsUnambiguousNumericMatricesPowersAndFractions() {
        assertEquals(
            listOf(
                LearningTextSegment.Text("A="),
                LearningTextSegment.Math("\\begin{bmatrix}1 & 2 \\\\ 3 & 4\\end{bmatrix}", display = false),
                LearningTextSegment.Text(" et x="),
                LearningTextSegment.Math("x^{2}", display = false),
                LearningTextSegment.Text("; ratio "),
                LearningTextSegment.Math("\\frac{3}{4}", display = false),
                LearningTextSegment.Text("."),
            ),
            parseLearningText("A=[[1,2],[3,4]] et x=x^2; ratio 3/4.", recognizeLegacyMath = true),
        )
    }

    @Test
    fun legacyModeLeavesOrdinaryProseAndUnbalancedMatricesUnchanged() {
        assertEquals(listOf(LearningTextSegment.Text("Choisir la bonne option.")), parseLearningText("Choisir la bonne option.", true))
        assertEquals(listOf(LearningTextSegment.Text("A=[[1,2],[3]]")), parseLearningText("A=[[1,2],[3]]", true))
        assertEquals(
            listOf(
                LearningTextSegment.Text("Pour "),
                LearningTextSegment.Math("x^{2}", false),
                LearningTextSegment.Text(", "),
                LearningTextSegment.Math("u_{1}", false),
                LearningTextSegment.Text(" et "),
                LearningTextSegment.Math("P=\\frac{5}{8}", false),
                LearningTextSegment.Text("."),
            ),
            parseLearningText("Pour x², u₁ et P=5/8.", true),
        )
    }

    @Test
    fun accessibilityDescriptionRemovesLatexLayoutCommands() {
        assertEquals("x sur y + (x)", mathAccessibilityDescription("\\frac{x}{y} + \\left(x\\right)"))
        assertEquals("matrice 1, 2; 3, 4", mathAccessibilityDescription("\\begin{bmatrix}1 & 2 \\\\ 3 & 4\\end{bmatrix}"))
    }

    @Test
    fun normalizesExistingPowersIndicesFractionsRootsLimitsAndMatrices() {
        assertEquals("x^{2}+2x+1", normalizeMathFormula("x²+2x+1"))
        assertEquals("x_{10}^{2}", normalizeMathFormula("x_10^2"))
        assertEquals("\\frac{3}{4}", normalizeMathFormula("3/4"))
        assertEquals("\\frac{x}{2}+3", normalizeMathFormula("x/2+3"))
        assertEquals("\\frac{x^{2}}{3}+1", normalizeMathFormula("x²/3+1"))
        assertEquals("\\sqrt{x^{2}+y^{2}}", normalizeMathFormula("√(x²+y²)"))
        assertEquals("\\lim_{x\\to 1} f(x)", normalizeMathFormula("limₓ→₁ f(x)"))
        assertEquals("\\lim_{x\\to a} f(x)", normalizeMathFormula("lim_{x->a} f(x)"))
        assertEquals("\\begin{bmatrix}1 & 2 \\\\ 3 & 4\\end{bmatrix}", normalizeMathFormula("[[1,2],[3,4]]"))
        assertEquals("A=\\begin{bmatrix}1 & 2 \\\\ 3 & 4\\end{bmatrix}", normalizeMathFormula("A=[[1,2],[3,4]]"))
    }

    @Test
    fun leavesProseAndMalformedMatricesAsPlainText() {
        assertNull(normalizeMathFormula("Une primitive de 2x est x²"))
        assertNull(normalizeMathFormula("[[1,2],[3]]"))
        assertNull(normalizeMathFormula("motif sans expression"))
    }
}
