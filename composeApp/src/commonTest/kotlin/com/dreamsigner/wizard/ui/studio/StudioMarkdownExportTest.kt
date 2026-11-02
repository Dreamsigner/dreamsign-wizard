package com.dreamsigner.wizard.ui.studio

import kotlin.test.*

class StudioMarkdownExportTest {
    @Test fun rendersVisibleContentInOrderWithoutUnansweredKeys() {
        val quiz = StudioQuiz("Combien ?", listOf(StudioQuizOption("a", "Deux"), StudioQuizOption("b", "Trois")), correctIds = listOf("a"), explanation = "SECRET EXPLICATION")
        val problem = StudioProblem("Résoudre \$x\$", "recall", "SECRET SOLUTION")
        val session = StudioSession("id", "Étudier les fractions", listOf(StudioMessage("user", "Bonjour"), StudioMessage("assistant", "", quiz = quiz), StudioMessage("assistant", "", problem = problem)), plan = StudioPlan(listOf(StudioPlanStep("Bases", emptyList(), "Calculer", emptyList())), "accepted"))
        val output = renderStudioMarkdown(session)
        assertEquals(output, renderStudioMarkdown(session))
        assertTrue(output.contains("Étudier les fractions"))
        assertTrue(output.indexOf("Bonjour") < output.indexOf("Combien ?"))
        assertTrue(output.indexOf("Combien ?") < output.indexOf("Résoudre"))
        assertTrue(output.contains("\$x\$"))
        assertFalse(output.contains("SECRET"))
        assertFalse(output.contains("correctIds"))
    }

    @Test fun rendersAnsweredExercisesAndOnlyRevealedHints() {
        val quiz = StudioQuiz("Q", listOf(StudioQuizOption("a", "Oui"), StudioQuizOption("b", "Non")), correctIds = listOf("a"), explanation = "Pourquoi")
        val answer = StudioQuizAnswer(listOf("a"), false, "Mon raisonnement", 80, true, 1)
        val problem = StudioProblem("Problème", "recall", "Solution", listOf("Indice visible", "Indice secret"), shownHints = 1, attempt = StudioProblemAttempt("Réponse", "Pensée", 1), result = StudioProblemResult("solved", "Bien"))
        val output = renderStudioMarkdown(StudioSession("id", "But", listOf(StudioMessage("assistant", "", quiz, answer), StudioMessage("assistant", "", problem = problem))))
        listOf("Mon raisonnement", "80 %", "Pourquoi", "Indice visible", "Réponse", "Pensée", "Bien", "Solution").forEach { assertTrue(output.contains(it), it) }
        assertFalse(output.contains("Indice secret"))
    }
}
