package com.dreamsigner.wizard.ui.studio

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
    @Test fun modelDiscoveryUsesConfiguredEndpointAndNeverPersistsKey() = runBlocking {
        val file = Files.createTempDirectory("studio-model-list").resolve("studio.json")
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        var auth = ""
        server.createContext("/v1/models") { exchange ->
            auth = exchange.requestHeaders.getFirst("Authorization").orEmpty()
            val bytes = """{"data":[{"id":"z-model"},{"id":"a-model"},{"id":"a-model"}]}""".toByteArray()
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        val controller = DesktopStudioController(file)
        try {
            val url = "http://127.0.0.1:${server.address.port}/v1"
            controller.discoverApiModels(url, "private-key")
            withTimeout(5_000) { while (controller.state.value.apiModelsBusy) delay(10) }
            assertEquals(listOf("a-model", "z-model"), controller.state.value.apiModels)
            assertEquals("Bearer private-key", auth)
            assertTrue(controller.configure(url, "a-model", "private-key"))
            assertFalse(Files.readString(file).contains("private-key"))
        } finally { controller.close(); server.stop(0) }
    }

    @Test fun cancelledDiscoveryCannotRestoreAStaleCatalog() = runBlocking {
        val file = Files.createTempDirectory("studio-model-cancel").resolve("studio.json")
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val started = java.util.concurrent.CountDownLatch(1)
        val release = java.util.concurrent.CountDownLatch(1)
        server.createContext("/v1/models") { exchange ->
            started.countDown()
            release.await(3, java.util.concurrent.TimeUnit.SECONDS)
            try {
                val bytes = """{"data":[{"id":"stale-model"}]}""".toByteArray()
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            } catch (_: java.io.IOException) { exchange.close() }
        }
        server.start()
        val controller = DesktopStudioController(file)
        try {
            controller.discoverApiModels("http://127.0.0.1:${server.address.port}", "")
            assertTrue(started.await(3, java.util.concurrent.TimeUnit.SECONDS))
            controller.cancelApiModelDiscovery()
            release.countDown()
            delay(100)
            assertFalse(controller.state.value.apiModelsBusy)
            assertTrue(controller.state.value.apiModels.isEmpty())
            assertTrue(controller.state.value.apiModelsUrl.isEmpty())
        } finally { release.countDown(); controller.close(); server.stop(0) }
    }

    @Test fun sessionModelSurvivesDefaultChangeAndRejectsDifferentEndpoint() = runBlocking {
        val file = Files.createTempDirectory("studio-model-pin").resolve("studio.json")
        val requests = mutableListOf<TutorRequest>()
        val tutor = StudioTutor { request -> synchronized(requests) { requests.add(request) }; "Réponse" }
        var controller = DesktopStudioController(file, tutor)
        try {
            assertTrue(controller.configure("http://localhost:1234/v1", "model-a", ""))
            assertTrue(controller.create("But"))
            assertTrue(controller.configure("http://localhost:1234/v1", "model-b", ""))
            assertTrue(controller.send("Question"))
            withTimeout(5_000) { while (controller.state.value.busy) delay(10) }
            assertEquals("model-a", synchronized(requests) { requests.single().model })
            assertTrue(controller.configure("http://localhost:5678/v1", "model-c", ""))
            assertFalse(controller.send("Ne pas envoyer au mauvais fournisseur"))
            assertEquals(2, controller.state.value.sessions.single().messages.size)
        } finally { controller.close() }
        controller = DesktopStudioController(file, tutor)
        try {
            assertEquals("model-a", controller.state.value.sessions.single().modelChoice?.model)
            assertFalse(controller.send("Toujours le mauvais fournisseur"))
            controller.setSessionModel(null)
            assertTrue(controller.send("Utiliser le choix actuel"))
            withTimeout(5_000) { while (controller.state.value.busy) delay(10) }
            assertEquals("model-c", synchronized(requests) { requests.last().model })
        } finally { controller.close() }
    }

    @Test fun sessionEffortPersistsAndOverridesDefaultIncludingAutomatic() = runBlocking {
        val file = Files.createTempDirectory("studio-session-effort").resolve("studio.json")
        val requests = mutableListOf<TutorRequest>()
        val tutor = StudioTutor { request -> synchronized(requests) { requests.add(request) }; "Réponse" }
        var controller = DesktopStudioController(file, tutor)
        try {
            assertTrue(controller.configure("http://localhost:1234/v1", "model", ""))
            controller.setReasoningEffort(ReasoningEffort.HIGH)
            assertTrue(controller.create("But"))
            controller.setReasoningEffort(ReasoningEffort.LOW)
            assertTrue(controller.send("Première question"))
            withTimeout(5_000) { while (controller.state.value.busy) delay(10) }
            controller.setSessionReasoningEffort(ReasoningEffort.AUTOMATIC)
        } finally { controller.close() }
        controller = DesktopStudioController(file, tutor)
        try {
            assertEquals(ReasoningEffort.AUTOMATIC, controller.state.value.sessions.single().reasoningEffort)
            assertTrue(controller.send("Deuxième question"))
            withTimeout(5_000) { while (controller.state.value.busy) delay(10) }
            controller.setReasoningEffort(ReasoningEffort.HIGH)
            controller.setSessionReasoningEffort(null)
            assertTrue(controller.send("Troisième question"))
            withTimeout(5_000) { while (controller.state.value.busy) delay(10) }
            assertEquals(listOf(ReasoningEffort.HIGH, ReasoningEffort.AUTOMATIC, ReasoningEffort.HIGH), synchronized(requests) { requests.map { it.reasoningEffort } })
        } finally { controller.close() }
        val reopened = DesktopStudioController(file)
        try { assertNull(reopened.state.value.sessions.single().reasoningEffort) } finally { reopened.close() }
    }

    @Test fun reasoningPreferencePersistsAndOldSessionsRemainReadable() {
        val file = Files.createTempDirectory("studio-effort").resolve("studio.json")
        Files.writeString(file, """{"sessions":[{"id":"s","goal":"But","messages":[{"role":"user","text":"Bonjour"}]}],"selectedId":"s"}""")
        val controller = DesktopStudioController(file)
        try {
            assertEquals(ReasoningEffort.AUTOMATIC, controller.state.value.reasoningEffort)
            controller.setReasoningEffort(ReasoningEffort.HIGH)
        } finally { controller.close() }
        val reopened = DesktopStudioController(file)
        try {
            assertEquals(ReasoningEffort.HIGH, reopened.state.value.reasoningEffort)
            assertEquals("Bonjour", reopened.state.value.sessions.single().messages.single().text)
            assertFalse(Files.readString(file).contains("apiKey"))
        } finally { reopened.close() }
    }

    @Test fun reasoningFieldsMatchBothProviderContracts() {
        for (effort in ReasoningEffort.entries) {
            val api = Json.parseToJsonElement(buildTutorBody("model", "goal", emptyList(), reasoningEffort = effort)).jsonObject
            val responses = Json.parseToJsonElement(buildChatGptBody("model", "goal", emptyList(), reasoningEffort = effort)).jsonObject
            if (effort == ReasoningEffort.AUTOMATIC) {
                assertFalse("reasoning_effort" in api)
                assertFalse("reasoning" in responses)
            } else {
                assertEquals(effort.wireValue, api.getValue("reasoning_effort").jsonPrimitive.content)
                assertEquals(effort.wireValue, responses.getValue("reasoning").jsonObject.getValue("effort").jsonPrimitive.content)
            }
        }
    }

    @Test fun rejectedReasoningIsReportedWithoutRetry() = runBlocking {
        val file = Files.createTempDirectory("studio-effort-400").resolve("studio.json")
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        var calls = 0
        var body = ""
        server.createContext("/v1/chat/completions") { exchange ->
            calls++
            body = exchange.requestBody.bufferedReader().readText()
            exchange.sendResponseHeaders(400, -1)
            exchange.close()
        }
        server.start()
        val controller = DesktopStudioController(file)
        try {
            assertTrue(controller.configure("http://127.0.0.1:${server.address.port}/v1", "model", ""))
            controller.setReasoningEffort(ReasoningEffort.LOW)
            assertTrue(controller.create("But"))
            assertTrue(controller.send("Question"))
            withTimeout(5_000) { while (controller.state.value.error == null) delay(10) }
            assertTrue(controller.state.value.error!!.contains("Automatique"))
            assertEquals("low", Json.parseToJsonElement(body).jsonObject.getValue("reasoning_effort").jsonPrimitive.content)
            assertEquals(1, calls)
        } finally { controller.close(); server.stop(0) }
    }

    @Test fun onlyAcceptedPlanGuidesBothProviderRequests() {
        val plan = StudioPlan(listOf(StudioPlanStep("Comprendre les fractions", emptyList(), "Expliquer 1/2 avec un dessin", listOf(0))))
        val history = listOf(StudioMessage("user", "Je connais les moitiés"))
        val proposed = buildTutorBody("local", "Fractions", history, plan)
        val accepted = plan.copy(decision = "accepted")
        val api = buildTutorBody("local", "Fractions", history, accepted)
        val chatGpt = buildChatGptBody("chosen-model", "Fractions", history, accepted)
        assertFalse(proposed.contains("Expliquer 1/2 avec un dessin"))
        assertTrue(api.contains("Expliquer 1/2 avec un dessin"))
        assertTrue(chatGpt.contains("Expliquer 1/2 avec un dessin"))
    }

    @Test fun planPersistsDecisionAndSourcesWithoutChangingMessages() {
        val file = Files.createTempDirectory("studio-plan").resolve("studio.json")
        val seed = """{"sessions":[{"id":"s","goal":"Apprendre","messages":[{"role":"user","text":"Je débute"},{"role":"assistant","text":"Explique ton raisonnement"}]}],"selectedId":"s"}"""
        Files.writeString(file, seed)
        val controller = DesktopStudioController(file)
        val steps = listOf(StudioPlanStep("Bases", emptyList(), "Expliquer un exemple", listOf(0)), StudioPlanStep("Application", listOf(0), "Résoudre un problème", listOf(1)))
        try {
            assertFalse(controller.savePlan(StudioPlan(steps + StudioPlanStep("Cycle", listOf(2), "Preuve", listOf(0)))))
            assertFalse(controller.savePlan(StudioPlan(listOf(steps.first().copy(sources = listOf(2))))))
            assertTrue(controller.savePlan(StudioPlan(steps, "accepted")))
        } finally { controller.close() }
        val reopened = DesktopStudioController(file)
        try {
            val session = reopened.state.value.sessions.single()
            assertEquals(StudioPlan(steps, "accepted"), session.plan)
            assertEquals(listOf("Je débute", "Explique ton raisonnement"), session.messages.map { it.text })
            assertTrue(reopened.savePlan(StudioPlan(steps, "diagnosis")))
        } finally { reopened.close() }
        val again = DesktopStudioController(file)
        try { assertEquals("diagnosis", again.state.value.sessions.single().plan?.decision) } finally { again.close() }
    }

    @Test fun unreadableChatGptCredentialsArePreservedWithoutOpeningSignIn() {
        val file = Files.createTempDirectory("studio-chatgpt-corrupt").resolve("chatgpt-plan.json")
        Files.writeString(file, "{broken")
        val client = ChatGptPlanClient(file)
        assertNotNull(client.storageError)
        assertFailsWith<IllegalStateException> { client.signIn() }
        assertEquals("{broken", Files.readString(file))
    }

    @Test fun chatGptPlanRequestUsesResponsesContractAndKeepsTheGoal() {
        val body = buildChatGptBody("chosen-model", "Comprendre les intégrales", listOf(StudioMessage("user", "Je connais les dérivées")))
        val root = Json.parseToJsonElement(body).jsonObject
        assertEquals("chosen-model", root.getValue("model").jsonPrimitive.content)
        assertFalse(root.getValue("store").jsonPrimitive.boolean)
        assertTrue(root.getValue("stream").jsonPrimitive.boolean)
        assertTrue(root.getValue("instructions").jsonPrimitive.content.contains("Comprendre les intégrales"))
        assertEquals("user", root.getValue("input").jsonArray.single().jsonObject.getValue("role").jsonPrimitive.content)
    }

    @Test fun chatGptStreamRequiresCompletedEventBeforeAcceptingAnswer() {
        val partial = "data: {\"type\":\"response.output_text.delta\",\"delta\":\"Bonjour\"}\n\n"
        assertFailsWith<IllegalStateException> { parseChatGptStream(partial.byteInputStream()) }
        val complete = partial + "data: {\"type\":\"response.completed\"}\n\n"
        assertEquals("Bonjour", parseChatGptStream(complete.byteInputStream()))
        val failed = partial + "data: {\"type\":\"response.failed\",\"response\":{\"error\":{\"code\":\"subscription_sharing_usage_limit_exceeded\"}}}\n\n"
        assertTrue(assertFailsWith<ChatGptPlanException> { parseChatGptStream(failed.byteInputStream()) }.safeMessage.contains("limite d’usage"))
    }

    @Test fun incompleteProviderConfigurationIsRejectedBeforeSaving() {
        val file = Files.createTempDirectory("studio-config").resolve("studio.json")
        val controller = DesktopStudioController(file)
        try {
            assertFalse(controller.configure("http://example.com", "model", "key"))
            assertTrue(controller.state.value.connectionError!!.contains("HTTPS"))
            assertFalse(Files.exists(file))
            assertFalse(controller.configure("https://example.com/v1", "", "key"))
            assertTrue(controller.state.value.connectionError!!.contains("modèle"))
            assertFalse(controller.configure("https://example.com/v1", "model", ""))
            assertTrue(controller.state.value.connectionError!!.contains("clé"))
            assertTrue(controller.configure("http://localhost:1234/v1", "local", ""))
            assertNull(controller.state.value.connectionError)
        } finally { controller.close() }
    }

    @Test fun providerAuthenticationFailureExplainsHowToRecoverWithoutLeakingTheKey() = runBlocking {
        val file = Files.createTempDirectory("studio-auth").resolve("studio.json")
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/v1/chat/completions") { exchange ->
            exchange.requestBody.close()
            exchange.sendResponseHeaders(401, -1)
            exchange.close()
        }
        server.start()
        val controller = DesktopStudioController(file)
        try {
            assertTrue(controller.create("Comprendre les limites"))
            assertTrue(controller.configure("http://127.0.0.1:${server.address.port}/v1", "local", "SECRET_TEST_KEY"))
            assertTrue(controller.send("Je pars de zéro"))
            withTimeout(5_000) { while (controller.state.value.error == null) delay(10) }
            assertTrue(controller.state.value.error!!.contains("Clé API refusée"))
            assertFalse(controller.state.value.error!!.contains("SECRET_TEST_KEY"))
            assertEquals(1, controller.state.value.sessions.single().messages.size)
        } finally { controller.close(); server.stop(0) }
    }

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
