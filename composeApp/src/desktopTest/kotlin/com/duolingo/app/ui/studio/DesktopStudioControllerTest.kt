package com.duolingo.app.ui.studio

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.serialization.json.*
import java.net.InetSocketAddress
import java.nio.file.Files
import kotlin.test.*

class DesktopStudioControllerTest {
    @Test fun tutorRequestEvictsOldWholeTurnsAndKeepsGoalAndLatestLearnerTurn() {
        val history = (0..10).map { index -> StudioMessage("assistant", "OLD_TURN_$index " + "x".repeat(20_000)) } +
            StudioMessage("user", "LATEST_PENDING_QUESTION")
        val body = buildTutorBody("local-model", "Objectif à conserver", history)
        val root = Json.parseToJsonElement(body).jsonObject
        val messages = root.getValue("messages").jsonArray.map { it.jsonObject.getValue("content").jsonPrimitive.content }
        assertTrue(body.length <= MAX_TUTOR_BODY_CHARS)
        assertTrue(messages.any { "Objectif à conserver" in it })
        assertTrue(messages.last() == "LATEST_PENDING_QUESTION")
        assertFalse(messages.any { "OLD_TURN_0" in it })
    }

    @Test fun corruptSessionFileIsPreservedAndDisplayedAsAnActionableError() {
        val dir = Files.createTempDirectory("studio-corrupt")
        val file = dir.resolve("studio.json")
        Files.writeString(file, "{broken")
        val controller = DesktopStudioController(file)
        try {
            assertTrue(controller.state.value.storageError!!.contains("illisible"))
            assertFalse(controller.create("Ne pas écraser les données"))
            assertEquals("{broken", Files.readString(file))
        } finally { controller.close() }
    }

    @Test fun sessionsReloadWithoutPersistingTheApiKey() {
        val dir = Files.createTempDirectory("studio-store")
        val file = dir.resolve("studio.json")
        val first = DesktopStudioController(file)
        first.create("Comprendre les valeurs propres")
        first.configure("https://example.test", "demo-model", "SECRET_TEST_KEY")
        val saved = Files.readString(file)
        assertTrue("Comprendre les valeurs propres" in saved)
        assertFalse("SECRET_TEST_KEY" in saved)
        first.close()

        val reloaded = DesktopStudioController(file)
        assertEquals("demo-model", reloaded.state.value.model)
        assertEquals("Comprendre les valeurs propres", reloaded.state.value.sessions.single().goal)
        assertEquals(reloaded.state.value.sessions.single().id, reloaded.state.value.selectedId)
        reloaded.close()
    }

    @Test fun sendsConfiguredConversationToLoopbackAndPersistsTheReply() = runBlocking {
        val dir = Files.createTempDirectory("studio-http")
        var auth = ""
        var requestBody = ""
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/v1/chat/completions") { exchange ->
            auth = exchange.requestHeaders.getFirst("Authorization") ?: ""
            requestBody = exchange.requestBody.use { it.readBytes().decodeToString() }
            val response = """{"choices":[{"message":{"content":"Partons de ton raisonnement."}}]}"""
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
            exchange.responseBody.use { it.write(response.toByteArray()) }
        }
        server.start()
        val controller = DesktopStudioController(dir.resolve("studio.json"))
        try {
            controller.create("Algèbre linéaire")
            controller.configure("http://127.0.0.1:${server.address.port}", "local-model", "LOCAL_SECRET")
            controller.send("Je connais les matrices")
            withTimeout(5_000) { while (controller.state.value.busy) delay(10); while (controller.state.value.sessions.first().messages.none { it.text == "Partons de ton raisonnement." }) delay(10) }
            assertEquals("Bearer LOCAL_SECRET", auth)
            assertTrue("local-model" in requestBody)
            assertTrue("Algèbre linéaire" in requestBody)
            assertTrue("Je connais les matrices" in requestBody)
            assertEquals(1, controller.state.value.sessions.first().messages.count { it.text == "Je connais les matrices" })
            assertFalse("LOCAL_SECRET" in Files.readString(dir.resolve("studio.json")))
        } finally { controller.close(); server.stop(0) }
    }

    @Test fun neverFollowsRedirectsWithTheProviderCredential() = runBlocking {
        val dir = Files.createTempDirectory("studio-redirect")
        var redirected = false
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/v1/chat/completions") { exchange ->
            exchange.requestBody.close()
            exchange.responseHeaders.add("Location", "/credential-capture")
            exchange.sendResponseHeaders(302, -1)
            exchange.close()
        }
        server.createContext("/credential-capture") { exchange -> redirected = true; exchange.sendResponseHeaders(200, -1); exchange.close() }
        server.start()
        val controller = DesktopStudioController(dir.resolve("studio.json"))
        try {
            controller.create("Comprendre les séries")
            controller.configure("http://127.0.0.1:${server.address.port}/v1", "local", "SECRET")
            assertTrue(controller.send("Je commence par zéro"))
            withTimeout(5_000) { while (controller.state.value.busy) delay(10); while (controller.state.value.error == null) delay(10) }
            assertFalse(redirected)
            assertEquals(1, controller.state.value.sessions.first().messages.count { it.role == "user" })
        } finally { controller.close(); server.stop(0) }
    }

    @Test fun rejectsNonLoopbackPlainHttpWithoutMakingARequest() = runBlocking {
        val file = Files.createTempDirectory("studio-http-policy").resolve("studio.json")
        val controller = DesktopStudioController(file)
        try {
            controller.create("Bases de Python")
            controller.configure("http://example.com", "model", "key")
            assertFalse(controller.send("Bonjour"))
            assertTrue(controller.state.value.error!!.contains("HTTPS"))
            assertEquals(0, controller.state.value.sessions.first().messages.count { it.text == "Bonjour" })
        } finally { controller.close() }
    }

    @Test fun failedReplyCanRetryWithoutDuplicatingTheLearnerTurn() = runBlocking {
        val file = Files.createTempDirectory("studio-retry").resolve("studio.json")
        var calls = 0
        val tutor = StudioTutor { request ->
            calls++
            assertEquals(1, request.history.count { it.role == "user" && it.text == "Je bloque sur le produit" })
            if (calls == 1) error("temporary failure") else "Regardons un exemple simple."
        }
        val controller = DesktopStudioController(file, tutor)
        try {
            controller.create("Comprendre le produit matriciel")
            controller.configure("http://localhost:1234/v1", "local", "")
            assertTrue(controller.send("Je bloque sur le produit"))
            withTimeout(5_000) { while (controller.state.value.error == null) delay(10) }
            assertEquals(1, controller.state.value.sessions.first().messages.count { it.role == "user" })
            controller.retry()
            withTimeout(5_000) { while (controller.state.value.sessions.first().messages.none { it.text == "Regardons un exemple simple." }) delay(10) }
            assertEquals(2, calls)
            assertEquals(1, controller.state.value.sessions.first().messages.count { it.role == "user" })
        } finally { controller.close() }
    }

    @Test fun cancellationKeepsTheTurnAndAllowsARealRetry() = runBlocking {
        val file = Files.createTempDirectory("studio-cancel").resolve("studio.json")
        var calls = 0
        val entered = CompletableDeferred<Unit>()
        val tutor = StudioTutor { _ -> calls++; if (calls == 1) { entered.complete(Unit); awaitCancellation() } else "Reprenons." }
        val controller = DesktopStudioController(file, tutor)
        try {
            controller.create("Apprendre les limites")
            controller.configure("http://127.0.0.1:4321", "local", "")
            assertTrue(controller.send("Je ne comprends pas"))
            withTimeout(5_000) { entered.await() }
            controller.cancel()
            assertFalse(controller.state.value.busy)
            assertEquals(1, controller.state.value.sessions.first().messages.count { it.text == "Je ne comprends pas" })
            controller.retry()
            withTimeout(5_000) { while (controller.state.value.sessions.first().messages.none { it.text == "Reprenons." }) delay(10) }
            assertEquals(1, controller.state.value.sessions.first().messages.count { it.text == "Je ne comprends pas" })
        } finally { controller.close() }
    }

    @Test fun failedReplyPersistenceLeavesControllerIdleAndDoesNotInventAReply() = runBlocking {
        val dir = Files.createTempDirectory("studio-write-failure")
        val file = dir.resolve("studio.json")
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<String>()
        val controller = DesktopStudioController(file, StudioTutor { entered.complete(Unit); release.await() })
        try {
            controller.create("Apprendre les suites")
            controller.configure("http://localhost:1234", "local", "")
            assertTrue(controller.send("J’ai une question"))
            withTimeout(5_000) { entered.await() }
            Files.delete(file)
            Files.createDirectory(file)
            release.complete("Réponse non persistée")
            withTimeout(5_000) { while (controller.state.value.storageError == null) delay(10) }
            assertFalse(controller.state.value.busy)
            assertTrue(controller.state.value.sessions.first().messages.none { it.text == "Réponse non persistée" })
            assertEquals(1, controller.state.value.sessions.first().messages.count { it.text == "J’ai une question" })
        } finally { controller.close() }
    }
}
