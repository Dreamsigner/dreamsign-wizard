package com.dreamsigner.wizard.ui.studio

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.*
import java.nio.file.Files
import kotlin.test.*

class StudioPracticeContextTest {
    private val source = listOf(KnowledgeSource("source", 0))
    private fun proposition(id: String, text: String = id) = KnowledgeProposition(id, text, source)
    private fun completed(id: String, text: String, outcome: String, snapshot: Boolean = true) =
        StudioMessage("assistant", "", problem = StudioProblem("Énoncé", "recall", "Solution", target = listOf(id),
            attempt = StudioProblemAttempt("Réponse", "Raisonnement", 1), result = StudioProblemResult(outcome, "Retour"),
            targetSnapshot = if (snapshot) mapOf(id to text) else emptyMap()))

    @Test fun prioritizesWeakAndUnseenAndIgnoresStaleOrLegacyEvidence() {
        val graph = KnowledgeGraph(propositions = listOf(proposition("strong"), proposition("new"), proposition("weak"), proposition("changed", "nouvel énoncé")))
        val messages = listOf(completed("strong", "strong", "solved"), completed("weak", "weak", "movedOn"),
            completed("changed", "ancien énoncé", "solved"), completed("new", "new", "solved", snapshot = false),
            completed("deleted", "deleted", "solved"))
        val targets = practiceTargets(graph, listOf(StudioSession("s", "objectif", messages)))
        assertEquals("weak", targets.first().id)
        assertEquals(1, targets.first().needsWork)
        assertEquals(0, targets.single { it.id == "new" }.solved)
        assertEquals(0, targets.single { it.id == "changed" }.solved)
        assertEquals("recall", targets.first().suggestedMode)
        assertEquals("discrimination", targets.single { it.id == "strong" }.suggestedMode)
        assertTrue(practiceTargets(KnowledgeGraph(), emptyList()).isEmpty())
        val relevant = KnowledgeGraph(propositions = listOf(proposition("unrelated", "Les équations"), proposition("relevant", "Les fractions simples")))
        assertEquals("relevant", practiceTargets(relevant, emptyList(), "Comprendre les fractions").first().id)
    }

    @Test fun contextIsBoundedAndIncludedInBothTransportsOnlyWhenRequested() {
        val graph = KnowledgeGraph(propositions = (1..50).map { proposition("p$it", "énoncé $it") })
        val targets = practiceTargets(graph, emptyList())
        assertEquals(20, targets.size)
        val history = listOf(StudioMessage("user", STUDIO_PROBLEM_REQUEST))
        val api = Json.parseToJsonElement(buildTutorBody("model", "objectif", history, practiceTargets = targets)).jsonObject
        val apiSystem = api.getValue("messages").jsonArray.first().jsonObject.getValue("content").jsonPrimitive.content
        val chat = Json.parseToJsonElement(buildChatGptBody("model", "objectif", history, practiceTargets = targets)).jsonObject
        assertTrue(apiSystem.contains("énoncé 1"))
        assertTrue(chat.getValue("instructions").jsonPrimitive.content.contains("énoncé 1"))
        assertFalse(buildTutorBody("model", "objectif", history).contains("Contexte de pratique"))
        assertTrue(practiceTargetsContext(emptyList()).contains("[]"))
    }

    @Test fun rejectsInventedTargetAndPersistsApprovedSnapshotOnRetry() = runBlocking {
        val file = Files.createTempDirectory("practice-target").resolve("studio.json")
        DesktopKnowledgeGraphStore(file.resolveSibling("knowledge-graph.json")).save(KnowledgeGraph(propositions = listOf(proposition("p1", "Deux parts égales"))))
        val fence = "`".repeat(3)
        var calls = 0
        val tutor = StudioTutor { request ->
            assertEquals("p1", request.practiceTargets?.single()?.id)
            val id = if (++calls == 1) "invented" else "p1"
            fence + "studio-problem\n" + """{"statement":"Question ?","mode":"recall","modelSolution":"Solution.","target":["$id"]}""" + "\n" + fence
        }
        var controller = DesktopStudioController(file, tutor)
        try {
            assertTrue(controller.configure("http://localhost:1234/v1", "model", ""))
            assertTrue(controller.create("Fractions"))
            assertTrue(controller.requestPracticeProblem())
            withTimeout(5_000) { while (controller.state.value.busy) delay(10) }
            assertNotNull(controller.state.value.error)
            assertNull(controller.state.value.sessions.single().messages.last().problem)
            controller.retry()
            withTimeout(5_000) { while (controller.state.value.busy) delay(10) }
            assertEquals(mapOf("p1" to "Deux parts égales"), controller.state.value.sessions.single().messages.last().problem?.targetSnapshot)
        } finally { controller.close() }
        controller = DesktopStudioController(file, tutor)
        try { assertEquals(mapOf("p1" to "Deux parts égales"), controller.state.value.sessions.single().messages.last().problem?.targetSnapshot) }
        finally { controller.close() }
    }

    @Test fun boundedRequestsDropTargetsBeforeExceedingTransportLimit() {
        val graph = KnowledgeGraph(propositions = (1..20).map { proposition("p$it", "x".repeat(180) + it) })
        val targets = practiceTargets(graph, emptyList())
        val history = listOf(StudioMessage("user", "x".repeat(55_000)))
        for (chatGpt in listOf(false, true)) {
            val fitted = fitPracticeTargets("model", "goal", history, null, ReasoningEffort.AUTOMATIC, targets, chatGpt)
            assertNotNull(fitted)
            assertTrue(fitted.size < targets.size)
            val body = if (chatGpt) buildChatGptBody("model", "goal", history, practiceTargets = fitted)
                else buildTutorBody("model", "goal", history, practiceTargets = fitted)
            assertTrue(body.length <= MAX_TUTOR_BODY_CHARS)
        }
    }
}
