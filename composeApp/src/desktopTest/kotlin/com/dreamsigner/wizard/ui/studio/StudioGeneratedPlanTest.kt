package com.dreamsigner.wizard.ui.studio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StudioGeneratedPlanTest {
    private val json = """{"version":1,"diagnosis":[{"observation":"Connaît les fractions","sources":[0]}],"steps":[{"title":"Réviser","prerequisites":[],"evidence":"Expliquer","sources":[0]},{"title":"Appliquer","prerequisites":[0],"evidence":"Résoudre","sources":[2]}]}"""
    private fun block(body: String) = "```studio-plan\n$body\n```"

    @Test fun validProposalPreservesSourcesAndDependencies() {
        val proposal = parseGeneratedPlan(block(json), setOf(0, 2))!!
        assertEquals(listOf(0), proposal.diagnosis.single().sources)
        assertEquals(listOf(0), proposal.steps[1].prerequisites)
        assertEquals(listOf(2), proposal.steps[1].sources)
    }

    @Test fun malformedOrAmbiguousResponseFallsBackToOriginalText() {
        for (response in listOf("ordinary text", block("{"), block(json) + " extra", block(json) + block(json), block(json.replace("\"steps\"", "\"extra\":1,\"steps\"")), block(json.replace("\"version\":1,", "")))) {
            assertNull(parseGeneratedPlan(response, setOf(0, 2)))
        }
    }

    @Test fun unseenDuplicateAndInvalidIndicesAreRejected() {
        assertNull(parseGeneratedPlan(block(json), setOf(0, 1)))
        assertNull(parseGeneratedPlan(block(json.replace("\"sources\":[2]", "\"sources\":[0,0]")), setOf(0, 2)))
        assertNull(parseGeneratedPlan(block(json.replace("\"prerequisites\":[0]", "\"prerequisites\":[1]")), setOf(0, 2)))
        assertNull(parseGeneratedPlan(block(json.replace("\"sources\":[2]", "\"sources\":[\"2\"]")), setOf(0, 2)))
    }

    @Test fun emptyAndOversizedFieldsAreRejected() {
        assertNull(parseGeneratedPlan(block(json.replace("Réviser", " ")), setOf(0, 2)))
        assertNull(parseGeneratedPlan(block(json.replace("Réviser", "x".repeat(501))), setOf(0, 2)))
        assertNull(parseGeneratedPlan(block(json.replace("\"diagnosis\":[{\"observation\":\"Connaît les fractions\",\"sources\":[0]}]", "\"diagnosis\":[]")), setOf(0, 2)))
        assertNull(parseGeneratedPlan("x".repeat(20_001), setOf(0)))
    }
}
