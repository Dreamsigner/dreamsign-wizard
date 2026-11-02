package com.dreamsigner.wizard.ui.studio

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.*
import java.nio.file.Files
import kotlin.test.*

class StudioLessonSourcesTest {
    private val source = StudioLessonSource("subject", "Matière", 2, "unit", "Unité", "lesson", "Leçon", 3, "Objectif", "Notes \"choisies\"", listOf("https://example.org"))

    @Test fun snapshotsPersistAndRemainIsolated() {
        val file = Files.createTempDirectory("studio-sources").resolve("studio.json")
        var controller = DesktopStudioController(file, StudioTutor { "ok" })
        try {
            assertTrue(controller.create("Première"))
            val first = controller.state.value.selectedId!!
            assertTrue(controller.addLessonSource(source))
            assertTrue(controller.addLessonSource(source.copy(learningNotes = "Changé")))
            assertEquals(listOf(source), controller.state.value.sessions.single().lessonSources)
            assertTrue(controller.create("Deuxième"))
            assertTrue(controller.state.value.sessions.first().lessonSources.isEmpty())
            controller.select(first)
        } finally { controller.close() }
        controller = DesktopStudioController(file, StudioTutor { "ok" })
        try {
            assertEquals(listOf(source), controller.state.value.sessions.first { it.goal == "Première" }.lessonSources)
            assertTrue(controller.removeLessonSource(source))
            assertTrue(controller.state.value.sessions.all { it.lessonSources.isEmpty() })
        } finally { controller.close() }
    }

    @Test fun oldSessionLoadsWithoutSources() {
        val file = Files.createTempDirectory("studio-old").resolve("studio.json")
        Files.writeString(file, """{"sessions":[{"id":"old","goal":"But","messages":[]}],"selectedId":"old"}""")
        val controller = DesktopStudioController(file, StudioTutor { "ok" })
        try { assertTrue(controller.state.value.sessions.single().lessonSources.isEmpty()) } finally { controller.close() }
    }

    @Test fun bothBodiesContainSameEscapedSelectedSnapshot() {
        val history = listOf(StudioMessage("user", "Dernier tour"))
        val api = Json.parseToJsonElement(buildTutorBody("model", "but", history, lessonSources = listOf(source))).jsonObject
        val chat = Json.parseToJsonElement(buildChatGptBody("model", "but", history, lessonSources = listOf(source))).jsonObject
        val apiPrompt = api.getValue("messages").jsonArray.first().jsonObject.getValue("content").jsonPrimitive.content
        val chatPrompt = chat.getValue("instructions").jsonPrimitive.content
        val context = lessonSourcesContext(listOf(source))
        assertTrue(context in apiPrompt && context in chatPrompt)
        assertFalse("Notes non choisies" in api.toString() || "Notes non choisies" in chat.toString())
        assertTrue("Dernier tour" in api.toString() && "Dernier tour" in chat.toString())
    }

    @Test fun oversizeSourcesAreRejectedBeforeTheyCanBeSent() = runBlocking {
        val file = Files.createTempDirectory("studio-limit").resolve("studio.json")
        var called = false
        val controller = DesktopStudioController(file, StudioTutor { called = true; "ok" })
        try {
            assertTrue(controller.configure("http://localhost:1234/v1", "model", ""))
            assertTrue(controller.create("But"))
            assertFalse(controller.addLessonSource(source.copy(learningNotes = "x".repeat(70_000))))
            assertTrue(controller.state.value.sessions.single().lessonSources.isEmpty())
            assertTrue(controller.state.value.error.orEmpty().contains("12 000"))
            controller.send("Dernier tour")
            withTimeout(5_000) { while (controller.state.value.busy) delay(10) }
            assertTrue(called)
            assertTrue(controller.state.value.sessions.single().messages.any { it.role == "user" && it.text == "Dernier tour" })
        } finally { controller.close() }
    }
}
