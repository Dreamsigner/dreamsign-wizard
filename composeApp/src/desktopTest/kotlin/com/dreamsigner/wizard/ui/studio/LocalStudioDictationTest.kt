package com.dreamsigner.wizard.ui.studio

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.nio.file.Files
import kotlin.test.*

class LocalStudioDictationTest {
    @Test fun argumentsPreserveSpacesAndTemporaryAudioIsRemoved() = runBlocking {
        val script = Files.createTempDirectory("dictation script ").resolve("start local.ps1")
        Files.writeString(script, "")
        var audio: java.nio.file.Path? = null
        val dictation = LocalStudioDictation(DictationProcessRunner { args ->
            assertEquals(script.toString(), args[4])
            audio = java.nio.file.Path.of(args.last())
            assertContentEquals(byteArrayOf(1, 2), Files.readAllBytes(audio))
            DictationProcessResult(0, """{"text":" Bonjour ! "}""")
        })
        assertEquals("Bonjour !", dictation.transcribe(script.toString(), byteArrayOf(1, 2)))
        assertFalse(Files.exists(audio))
    }

    @Test fun parsesSilenceAndRejectsInvalidOutput() {
        assertEquals("", parseDictationOutput("""{"text":" "}"""))
        assertFails { parseDictationOutput("{}") }
        assertFails { parseDictationOutput("not json") }
    }

    @Test fun processErrorDoesNotExposeOutputAndCleansAudio() = runBlocking {
        val script = Files.createTempFile("dictation-script", ".ps1")
        var audio: java.nio.file.Path? = null
        val dictation = LocalStudioDictation(DictationProcessRunner { args ->
            audio = java.nio.file.Path.of(args.last())
            DictationProcessResult(1, "private output")
        })
        assertFails { dictation.transcribe(script.toString(), byteArrayOf(1)) }
        assertFalse(Files.exists(audio))
    }

    @Test fun persistsOnlyPathAndConsumesResult() = runBlocking {
        val file = Files.createTempDirectory("dictation-state").resolve("studio.json")
        val controller = DesktopStudioController(file = file, dictation = StudioDictation { _, _ -> "texte privé" })
        try {
            assertTrue(controller.create("Objectif"))
            assertTrue(controller.configureDictation("D:\\Chemin avec espaces\\script.ps1"))
            assertTrue(controller.transcribeDictation(byteArrayOf(1)))
            withTimeout(5000) { while (controller.state.value.dictationBusy) delay(10) }
            assertEquals("texte privé", controller.consumeDictationResult())
            assertNull(controller.consumeDictationResult())
            assertFalse(Files.readString(file).contains("texte privé"))
        } finally { controller.close() }
        val reopened = DesktopStudioController(file = file)
        try {
            assertEquals("D:\\Chemin avec espaces\\script.ps1", reopened.state.value.dictationScriptPath)
            assertNull(reopened.state.value.dictationResult)
        } finally { reopened.close() }
    }

    @Test fun switchingSessionCancelsAndDiscardsResult() = runBlocking {
        val file = Files.createTempDirectory("dictation-cancel").resolve("studio.json")
        val started = CompletableDeferred<Unit>()
        val controller = DesktopStudioController(file = file, dictation = StudioDictation { _, _ -> started.complete(Unit); awaitCancellation() })
        try {
            assertTrue(controller.create("Un"))
            val first = controller.state.value.selectedId!!
            assertTrue(controller.create("Deux"))
            assertTrue(controller.transcribeDictation(byteArrayOf(1)))
            started.await()
            controller.select(first)
            assertFalse(controller.state.value.dictationBusy)
            assertNull(controller.state.value.dictationResult)
            assertNull(controller.state.value.dictationSessionId)
        } finally { controller.close() }
    }
}
