package com.dreamsigner.wizard.ui.studio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class KnowledgeGraphExtractionTest {
    private val valid = """{"propositions":[{"id":"a","statement":"A","messageIndices":[0]},{"id":"b","statement":"B","messageIndices":[2]}],"reasonings":[{"id":"r","premises":["a"],"conclusion":"b","messageIndices":[2]}]}"""

    @Test fun rejectsUnknownSourcesDanglingPremisesAndDuplicateIds() {
        assertFails { KnowledgeGraphExtraction.parse(valid, setOf(0, 1)) }
        assertFails { KnowledgeGraphExtraction.parse(valid.replace("[\"a\"]", "[\"missing\"]"), setOf(0, 2)) }
        assertFails { KnowledgeGraphExtraction.parse(valid.replace("\"id\":\"b\"", "\"id\":\"a\""), setOf(0, 2)) }
        assertFails { KnowledgeGraphExtraction.parse(valid.replace("\"statement\":\"A\"", "\"statement\":\"A\",\"mastery\":true"), setOf(0, 2)) }
    }

    @Test fun deterministicMergeAndPreciseProvenance() {
        val input = KnowledgeGraphExtraction.parse(valid, setOf(0, 2))
        val existing = KnowledgeGraph(propositions = listOf(KnowledgeProposition("existing", "A", listOf(KnowledgeSource("old", 1)))))
        val proposal = KnowledgeGraphExtraction.propose(existing, input, "session", setOf(0, 2))
        assertEquals(existing, proposal.base)
        assertEquals(listOf(KnowledgeSource("old", 1), KnowledgeSource("session", 0)), proposal.graph.propositions.first().sources)
        assertEquals(listOf("existing"), proposal.graph.reasonings.single().premises)
        assertEquals(proposal.graph, KnowledgeGraphExtraction.propose(proposal.graph, input, "session", setOf(0, 2)).graph)
        assertFails { KnowledgeGraphExtraction.propose(existing, input, "session", setOf(0)) }
    }
}
