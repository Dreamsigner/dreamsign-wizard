package com.dreamsigner.wizard.ui.math

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FractionExpressionsTest {
    @Test
    fun formatsCorpusStyleNestedAndCompositeFractions() {
        val cases = mapOf(
            "[f(a+h)−f(a)]/h" to "\\frac{\\left[f\\left(a+h\\right)-f\\left(a\\right)\\right]}{h}",
            "(1/x)′=−1/x²" to "\\left(\\frac{1}{x}\\right)^{\\prime}=-\\frac{1}{x^{2}}",
            "(g+x/g)/2" to "\\frac{\\left(g+\\frac{x}{g}\\right)}{2}",
            "(5+9/5)/2" to "\\frac{\\left(5+\\frac{9}{5}\\right)}{2}",
            "P(A∩B)/P(B)" to "\\frac{P\\left(A\\cap B\\right)}{P\\left(B\\right)}",
            "12000/10^9" to "\\frac{12000}{10^{9}}",
            "9/6²" to "\\frac{9}{6^{2}}",
            "9/6²=9/36=1/4" to "\\frac{9}{6^{2}}=\\frac{9}{36}=\\frac{1}{4}",
            "5×4×3/5³" to "\\frac{5\\cdot 4\\cdot 3}{5^{3}}",
            "1/cos²x" to "\\frac{1}{\\cos^{2}x}",
            "1/cos² x" to "\\frac{1}{\\cos^{2} x}",
            "x/cos² x" to "\\frac{x}{\\cos^{2} x}",
            "sin x/cos x" to "\\frac{\\sin x}{\\cos x}",
            "0,08/4" to "\\frac{0,08}{4}",
            "0.08/4" to "\\frac{0.08}{4}",
            "nested(a,b)/nested(c,d)" to "\\frac{nested\\left(a,b\\right)}{nested\\left(c,d\\right)}",
            "(1−e⁻²ᴮ)/2" to "\\frac{\\left(1-e^{-2B}\\right)}{2}",
            "1/(2√x)" to "\\frac{1}{\\left(2\\sqrt{x}\\right)}",
            "f(xₙ)/f′(xₙ)" to "\\frac{f\\left(x_{n}\\right)}{f^{\\prime}\\left(x_{n}\\right)}",
            "π/2" to "\\frac{\\pi }{2}",
            "dx/x" to "\\frac{dx}{x}",
            "|x|/x" to "\\frac{\\left|x\\right|}{x}",
            "M|x|/x" to "\\frac{M\\left|x\\right|}{x}",
            "(1/x)/(−1/x²)" to "\\frac{\\left(\\frac{1}{x}\\right)}{\\left(-\\frac{1}{x^{2}}\\right)}",
            "-1/(x+1)" to "-\\frac{1}{\\left(x+1\\right)}",
            "[(2+h)²−4]/h" to "\\frac{\\left[\\left(2+h\\right)^{2}-4\\right]}{h}",
            "[(eˣ+e⁻ˣ)²−(eˣ−e⁻ˣ)²]/4=1" to "\\frac{\\left[\\left(e^{x}+e^{-x}\\right)^{2}-\\left(e^{x}-e^{-x}\\right)^{2}\\right]}{4}=1",
            "(3x+1)³/3+C" to "\\frac{\\left(3x+1\\right)^{3}}{3}+C",
            "aₙ₊₁/aₙ" to "\\frac{a_{n+1}}{a_{n}}",
            "xⁿ⁺¹/x" to "\\frac{x^{n+1}}{x}",
            "xᵖ/2" to "\\frac{x^{p}}{2}",
            "1/xₚ" to "\\frac{1}{x_{p}}",
            "Aᵀ/n" to "\\frac{A^{T}}{n}",
            "x⁽ⁿ⁺¹⁾/2" to "\\frac{x^{(n+1)}}{2}",
            "n!/(n+1)!" to "\\frac{n!}{\\left(n+1\\right)!}",
            "(1,−1)/√2" to "\\frac{\\left(1,-1\\right)}{\\sqrt{2}}",
            "∂g/∂u" to "\\frac{\\partial g}{\\partial u}",
            "κ=Σ|xᵢ|/|Σxᵢ|" to "κ=\\frac{\\sum \\left|x_{i}\\right|}{\\left|\\sum x_{i}\\right|}",
        )
        cases.forEach { (source, expected) -> assertEquals(expected, formatFractionExpression(source), source) }
    }

    @Test
    fun findsExpressionsInsideProseWithoutTakingTheProsePrefix() {
        val text = "Le taux est (a+b)/c, donc 3/4."
        assertEquals(
            listOf(
                FractionExpressionSpan(12, 19, "\\frac{\\left(a+b\\right)}{c}"),
                FractionExpressionSpan(26, 29, "\\frac{3}{4}"),
            ),
            findFractionExpressions(text),
        )
        assertEquals(1, findFractionExpressions("donne (a+b)/c").size)
        assertEquals(6, findFractionExpressions("donne (a+b)/c").single().start)
        assertEquals(
            listOf(
                FractionExpressionSpan(22, 25, "\\frac{1}{2}"),
                FractionExpressionSpan(29, 33, "\\frac{b^{2}}{2}"),
            ),
            findFractionExpressions("|x| est positif. Puis 1/2 et b²/2."),
        )
    }

    @Test
    fun rejectsDatesPathsCodeAndKnownProseSlashPairs() {
        listOf("2026/01/09", "D:/cours/math", "assets/logo.png", "vrai/faux", "et/ou", "`1/2`").forEach {
            assertEquals(emptyList(), findFractionExpressions(it), it)
            assertNull(formatFractionExpression(it), it)
        }
        assertEquals(emptyList(), findFractionExpressions("|x/x"))
        assertNull(formatFractionExpression("|x/x"))
        assertEquals("\\frac{x}{2}", formatFractionExpression("x/2"))
    }

    @Test
    fun wholeFormulaRequiresCompleteParseAndRetainsPrecedence() {
        assertEquals("\\frac{x}{2}+3", formatFractionExpression("x/2+3"))
        assertEquals("\\frac{x^{2}}{3}+1", formatFractionExpression("x²/3+1"))
        assertEquals("x^{2}+\\frac{3}{4}", formatFractionExpression("x^2+3/4"))
        assertEquals("x=\\frac{\\frac{1}{2}}{3}", formatFractionExpression("x=1/2/3"))
        assertEquals("2,5+\\frac{2}{4}", formatFractionExpression("2,5+2/4"))
        assertNull(formatFractionExpression("x/(y+"))
        assertNull(formatFractionExpression("motif sans expression"))
        assertEquals("\\frac{\\left(a+b\\right)}{c}=x", formatFractionExpression("(a+b)/c = x"))
        assertNull(formatFractionExpression("(a+b)/c ="))
        assertEquals("1+\\frac{1}{3}+\\frac{1}{9}", formatFractionExpression("1+1/3+1/9"))
        assertEquals(
            listOf(FractionExpressionSpan(0, 9, "1+\\frac{1}{3}+\\frac{1}{9}")),
            findFractionExpressions("1+1/3+1/9+…"),
        )
        assertEquals(
            FractionExpressionSpan(4, 15, "\\frac{\\left(1-\\cos h\\right)}{h}"),
            findFractionExpressions("lim (1−cos h)/h").single(),
        )
        assertNull(formatFractionExpression("lim (1−cos h)/h"))
    }
}
