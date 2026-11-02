package com.dreamsigner.wizard.ui.studio

import java.nio.file.Files
import kotlin.test.*

class DesktopStudioExportTest {
    @Test fun atomicPublicationDoesNotOverwrite() {
        val directory = Files.createTempDirectory("studio-export-test")
        val path = directory.resolve("séance.md")
        writeStudioExport(path, "Été")
        assertEquals("Été", Files.readString(path))
        assertFails { writeStudioExport(path, "perdu") }
        assertEquals("Été", Files.readString(path))
        assertEquals(1, Files.list(directory).use { it.count() }.toInt())
    }
    @Test fun cancellationAndWriteErrorAreReportedWithoutChangingSession() {
        val dir = Files.createTempDirectory("studio-export-controller")
        var fail = false
        val controller = DesktopStudioController(dir.resolve("data.json"), chooseExport = { if (fail) dir.resolve("x.md") else null }, writeExport = { _, _ -> error("failure") })
        try {
            assertTrue(controller.create("Objectif"))
            val before = controller.state.value.sessions
            assertFalse(controller.exportSession())
            assertNull(controller.state.value.exportError)
            fail = true
            assertFalse(controller.exportSession())
            assertNotNull(controller.state.value.exportError)
            assertEquals(before, controller.state.value.sessions)
        } finally { controller.close() }
    }
    @Test fun filenameIsSafe() {
        assertEquals("seance.md", studioExportFilename("CON"))
        assertFalse(studioExportFilename("a/b:c").contains('/'))
    }
}
