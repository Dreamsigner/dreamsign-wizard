package com.dreamsigner.wizard.ui.studio

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StudioStepEvidenceTest {
    @Test fun onlySuccessfulReasonedAttemptsCanUnlockAPrerequisite() {
        val quiz = StudioQuiz("Combien ?", listOf(StudioQuizOption("a", "Deux"), StudioQuizOption("b", "Trois")), correctIds = listOf("a"), explanation = "Deux.")
        val plan = StudioPlan(listOf(
            StudioPlanStep("Base", emptyList(), "Expliquer", listOf(0)),
            StudioPlanStep("Suite", listOf(0), "Appliquer", listOf(0)),
        ), "accepted")
        val incorrect = StudioQuizAnswer(listOf("b"), false, "Je pense trois", 70, false, 1)
        val correctWithoutReason = StudioQuizAnswer(listOf("a"), false, "", 70, true, 1)
        val correctWithReason = StudioQuizAnswer(listOf("a"), false, "Deux parts égales", 70, true, 1)
        val base = StudioSession("s", "Fractions", listOf(StudioMessage("assistant", "", quiz, incorrect)), plan)
        assertFalse(base.evidenceSource(0))
        assertFalse(base.copy(stepEvidence = listOf(StudioStepEvidence(0, 0))).stepReady(1))
        assertFalse(base.copy(messages = listOf(StudioMessage("assistant", "", quiz, correctWithoutReason))).evidenceSource(0))
        val explained = base.copy(messages = listOf(StudioMessage("assistant", "", quiz, correctWithReason)))
        assertTrue(explained.evidenceSource(0))
        assertTrue(explained.copy(stepEvidence = listOf(StudioStepEvidence(0, 0))).stepReady(1))
    }
}
