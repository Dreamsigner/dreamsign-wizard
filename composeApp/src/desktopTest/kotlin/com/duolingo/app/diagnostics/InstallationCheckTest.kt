package com.duolingo.app.diagnostics

import com.duolingo.app.data.SampleData
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import kotlin.test.*

class InstallationCheckTest {
    @Test fun newReportsIncludeTheUiAndUserStorageIsolationFlags() {
        val directory = Files.createTempDirectory("installation-check-")
        val path = directory.resolve("new-report.json")
        try {
            writeInstallationCheckReport(path, InstallationCheckReport(1, 2, 3, emptyMap()))
            val report = Json.parseToJsonElement(Files.readString(path)).jsonObject
            assertEquals(JsonPrimitive(false), report["uiStarted"])
            assertEquals(JsonPrimitive(false), report["userDataAccessed"])
        } finally {
            Files.deleteIfExists(path)
            Files.deleteIfExists(directory)
        }
    }

    @Test fun thePublishedCatalogueAndRealBundledAudioLoadWithoutUiOrUserStorage() = runBlocking<Unit> {
        val report = checkInstallation()
        val units = SampleData.sampleSubjects.flatMap { it.units }
        assertEquals(units.size, report.courses)
        assertEquals(units.sumOf { it.lessons.size }, report.lessons)
        assertEquals(units.sumOf { u -> u.lessons.sumOf { it.exercises.size } }, report.exercises)
        assertEquals(3, report.audioSha256.size)
        assertTrue(report.audioSha256.values.all { it.matches(Regex("[0-9a-f]{64}")) })
        assertFalse(report.uiStarted)
        assertFalse(report.userDataAccessed)
    }

    @Test fun aMissingBundledResourceFailsTheCheck() = runBlocking<Unit> {
        assertFailsWith<IllegalStateException> { checkInstallation(readResource = { error("Missing fixture") }) }
    }

    @Test fun corruptAudioFailsTheCheck() = runBlocking<Unit> {
        assertFails { checkInstallation(readResource = { byteArrayOf(1, 2, 3) }) }
    }

    @Test fun writingAReportNeverOverwritesAnExistingFile() {
        val directory = Files.createTempDirectory("installation-check-")
        val path = directory.resolve("report.json")
        try {
            Files.writeString(path, "existing content")
            assertFailsWith<FileAlreadyExistsException> {
                writeInstallationCheckReport(path, InstallationCheckReport(1, 1, 1, emptyMap()))
            }
            assertEquals("existing content", Files.readString(path))
        } finally {
            Files.deleteIfExists(path)
            Files.deleteIfExists(directory)
        }
    }
}
