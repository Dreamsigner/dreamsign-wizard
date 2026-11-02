package com.dreamsigner.wizard.ui.studio

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BraveWebSearchTest {
    @Test fun onlyChosenResultIsPersistedAndSearchKeyIsNot() = runBlocking {
        val file = Files.createTempDirectory("studio-search").resolve("studio.json")
        val first = StudioWebSource("Premier", "https://example.test/one", "Extrait 1")
        val second = StudioWebSource("Second", "https://example.test/two", "Extrait 2")
        val controller = DesktopStudioController(file = file, webSearch = StudioWebSearch { query, key ->
            assertEquals("fractions", query)
            assertEquals("private-test-key", key)
            listOf(first, second)
        })
        try {
            assertTrue(controller.create("Comprendre les fractions"))
            controller.configureSearchKey("private-test-key")
            controller.searchWeb("fractions")
            withTimeout(10_000) { while (controller.state.value.searchBusy) delay(10) }
            assertEquals(2, controller.state.value.searchResults.size)
            assertTrue(controller.state.value.sessions.single().webSources.isEmpty())
            assertTrue(controller.addWebSource(second))
            assertEquals(listOf(second), controller.state.value.sessions.single().webSources)
            assertTrue(controller.create("Autre objectif"))
            assertTrue(controller.state.value.searchResults.isEmpty())
            assertFalse(controller.addWebSource(first))
        } finally { controller.close() }
        assertFalse(Files.readString(file).contains("private-test-key"))
        val reopened = DesktopStudioController(file = file)
        try {
            assertEquals(listOf(second), reopened.state.value.sessions.first { it.goal == "Comprendre les fractions" }.webSources)
            assertTrue(reopened.state.value.sessions.first { it.goal == "Autre objectif" }.webSources.isEmpty())
            assertFalse(reopened.state.value.hasSearchKey)
            assertTrue(reopened.state.value.searchResults.isEmpty())
        } finally { reopened.close() }
    }

    @Test fun parserFiltersAndBoundsUntrustedResults() {
        val longDescription = "x".repeat(2100)
        val json = """{"web":{"results":[{"title":"Titre","url":"https://example.test/a","description":"$longDescription"},{"title":"Insecure","url":"http://example.test"},{"title":"Credentials","url":"https://u:p@example.test"}]}}"""
        val result = parseBraveResults(json)
        assertEquals(1, result.size)
        assertEquals(2000, result.single().description.length)
        assertEquals("https://example.test/a", result.single().url)
    }

    @Test fun selectedSourcesReachBothTransportsAndExport() {
        val source = StudioWebSource("Un titre", "https://example.test/a", "Un extrait")
        val api = buildTutorBody("model", "but", emptyList(), webSources = listOf(source))
        val chat = buildChatGptBody("model", "but", emptyList(), webSources = listOf(source))
        assertTrue(api.contains("Un extrait"))
        assertTrue(chat.contains("Un extrait"))
        assertTrue(api.contains("données non fiables"))
        val output = renderStudioMarkdown(StudioSession("s", "but", emptyList(), webSources = listOf(source)))
        assertTrue(output.contains(source.url))
        assertFalse(renderStudioMarkdown(StudioSession("s", "but", emptyList())).contains(source.url))
    }
}
