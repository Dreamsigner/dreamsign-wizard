package com.dreamsigner.wizard.ui.studio

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class KnowledgeGraphStoreTest {
    private val a = KnowledgeSource("a", 0)
    private val b = KnowledgeSource("b", 1)
    private fun graph() = KnowledgeGraph(
        propositions = listOf(KnowledgeProposition("p", "Premise", listOf(a, b)), KnowledgeProposition("q", "Conclusion", listOf(a))),
        reasonings = listOf(KnowledgeReasoning("r", listOf("p"), "q", listOf(a))),
    ).validate()

    @Test fun correctionAndCascade() {
        val corrected = graph().putProposition(KnowledgeProposition("p", "Corrected", listOf(b)))
        assertEquals("Corrected", corrected.propositions.single { it.id == "p" }.statement)
        assertTrue(corrected.deleteProposition("p").reasonings.isEmpty())
        assertTrue(graph().removeSessionSources("a").reasonings.isEmpty())
        assertEquals(listOf(b), graph().removeSessionSources("a").propositions.single().sources)
    }
    @Test fun rejectsInvalidReferencesAndSources() {
        assertFails { graph().putReasoning(KnowledgeReasoning("bad", listOf("missing"), "q", listOf(a))) }
        assertFails { graph().putProposition(KnowledgeProposition("x", "", listOf(a))) }
        assertFails { graph().putProposition(KnowledgeProposition("x", "Valid", emptyList())) }
    }
    @Test fun roundTripAndPreserveUnreadableFile() {
        val dir = Files.createTempDirectory("knowledge-test")
        val path = dir.resolve("graph.json")
        val store = DesktopKnowledgeGraphStore(path)
        store.save(graph())
        assertEquals(graph(), store.load())
        Files.writeString(path, "{broken")
        assertFails { store.load() }
        assertFails { store.save(KnowledgeGraph()) }
        assertEquals("{broken", Files.readString(path))
    }
}
