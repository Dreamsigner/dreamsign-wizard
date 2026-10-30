package com.dreamsigner.wizard.media

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class RecordingStoreTest {
    private fun inTemporaryDirectory(block: (Path) -> Unit) {
        val temporaryRoot = Path.of(System.getProperty("java.io.tmpdir")).toAbsolutePath().normalize()
        val directory = Files.createTempDirectory(temporaryRoot, "wizard-recording-store-test-").toAbsolutePath().normalize()
        try { block(directory) }
        finally {
            // Restrict recursive cleanup to the exact temporary directory we created.
            check(directory.startsWith(temporaryRoot) && directory.fileName.toString().startsWith("wizard-recording-store-test-"))
            directory.toFile().deleteRecursively()
        }
    }

    @Test fun savesReloadsAndReplacesOnlyInsideIsolatedDirectory() = inTemporaryDirectory { directory ->
        val first = pcmRecordingToWav(ByteArray(RECORDING_RATE * 2))
        val second = pcmRecordingToWav(ByteArray(RECORDING_RATE * 4) { (it % 97).toByte() })
        val store = FileAudioRecordingStore(directory)
        assertNull(store.load("oral_1"))
        store.save("oral_1", first)
        assertContentEquals(first, FileAudioRecordingStore(directory).load("oral_1"))
        store.save("oral_1", second)
        assertContentEquals(second, store.load("oral_1"))
        Files.list(directory).use { assertEquals(1L, it.count()) }
    }

    @Test fun corruptRecordingIsRejectedRatherThanReportedMissing() = inTemporaryDirectory { directory ->
        val store = FileAudioRecordingStore(directory)
        val corrupt = pcmRecordingToWav(ByteArray(RECORDING_RATE * 2)).also { it[0] = 0 }
        Files.write(directory.resolve("oral_1.wav"), corrupt)
        assertFailsWith<IllegalArgumentException> { store.load("oral_1") }
    }

    @Test fun invalidReplacementPreservesOriginalAndRejectsTraversal() = inTemporaryDirectory { directory ->
        val store = FileAudioRecordingStore(directory)
        val original = pcmRecordingToWav(ByteArray(RECORDING_RATE * 2))
        store.save("oral_1", original)
        assertFailsWith<IllegalArgumentException> { store.save("oral_1", byteArrayOf(1, 2, 3)) }
        assertFailsWith<IllegalArgumentException> { store.save("../escape", original) }
        assertFailsWith<IllegalArgumentException> { store.load("../escape") }
        assertContentEquals(original, store.load("oral_1"))
        Files.list(directory).use { assertEquals(1L, it.count()) }
    }

    @Test fun failedAtomicReplacementPreservesOriginalAndRemovesTemporary() = inTemporaryDirectory { directory ->
        val original = pcmRecordingToWav(ByteArray(RECORDING_RATE * 2))
        val replacement = pcmRecordingToWav(ByteArray(RECORDING_RATE * 4))
        val store = FileAudioRecordingStore(directory)
        store.save("oral_1", original)
        val failingStore = FileAudioRecordingStore(directory) { _, _ -> throw IOException("Injected atomic move failure") }
        assertFailsWith<IOException> { failingStore.save("oral_1", replacement) }
        assertContentEquals(original, store.load("oral_1"))
        Files.list(directory).use { assertEquals(1L, it.count()) }
    }
}
