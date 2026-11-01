package com.dreamsigner.wizard.ui.studio

import kotlin.test.*

class StudioDiagnosticTest {
    private val json = """{"version":1,"strands":[{"strand":"Fractions","floor":"Sait reconnaître une moitié","ceiling":"Comparaison de tiers incertaine","misconception":null,"sources":[1,3]}],"summary":"La moitié est acquise ; les tiers restent à vérifier."}"""
    private fun block(body: String) = "```studio-diagnosis\n$body\n```"

    @Test fun assessmentNeedsEvidenceFromAnsweredQuestions() {
        val parsed = parseStudioDiagnostic(block(json), setOf(1, 3))!!
        assertEquals(listOf(1, 3), parsed.strands.single().sources)
        assertNull(parseStudioDiagnostic(block(json), setOf(1)))
        assertNull(parseStudioDiagnostic(block(json.replace("\"floor\":\"Sait reconnaître une moitié\"", "\"floor\":\" \"")), setOf(1, 3)))
        assertNull(parseStudioDiagnostic("Prose libre " + block(json), setOf(1, 3)))
    }
}
