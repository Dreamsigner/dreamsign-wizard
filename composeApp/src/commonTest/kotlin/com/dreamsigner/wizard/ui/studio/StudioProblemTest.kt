package com.dreamsigner.wizard.ui.studio

import kotlin.test.*

class StudioProblemTest {
    private val body = """{"statement":"Calcule 2 + 2.","mode":"recall","modelSolution":"2 + 2 = 4.","hints":["Additionne les deux paires."],"target":[]}"""
    private fun block(value: String) = "```studio-problem\n$value\n```"

    @Test fun solutionIsCommittedInsideAValidatedProblem() {
        val problem = parseStudioProblem(block(body))!!
        assertEquals("2 + 2 = 4.", problem.modelSolution)
        assertEquals(1, problem.hints.size)
        assertNull(problem.result)
    }

    @Test fun proseAndForgedProgressAreRejected() {
        assertNull(parseStudioProblem("La réponse est 4.\n" + block(body)))
        assertNull(parseStudioProblem(block(body.replace("\"mode\":\"recall\"", "\"mode\":\"unknown\""))))
        assertNull(parseStudioProblem(block(body.dropLast(1) + ",\"shownHints\":1}")))
        assertNull(parseStudioProblem(block(body.dropLast(1) + ",\"result\":{\"outcome\":\"solved\",\"verdict\":\"ok\"}}")))
    }
}
