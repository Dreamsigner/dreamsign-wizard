package com.dreamsigner.wizard.storage

import java.nio.file.Files
import kotlin.test.*

class FileProgressStoreTest {
    @Test fun largeCatalogueProgressSurvivesAtomicReplacement() {
        val directory = Files.createTempDirectory("learning-store-test")
        val path = directory.resolve("progress.json")
        try {
            val store = FileProgressStore(path)
            assertNull(store.read())
            val large = "{\"completed\":\"" + "lesson-id,".repeat(4000) + "\"}"
            store.write(large)
            assertEquals(large, FileProgressStore(path).read())
            store.write("{\"xp\":20}")
            assertEquals("{\"xp\":20}", store.read())
            Files.list(directory).use { assertEquals(1, it.count()) }
        } finally {
            Files.deleteIfExists(path)
            Files.deleteIfExists(directory)
        }
    }

    @Test fun failedWriteDoesNotDeleteTheBlockingFile() {
        val blocker = Files.createTempFile("learning-store-blocker", ".txt")
        try {
            Files.writeString(blocker, "existing user data")
            val store = FileProgressStore(blocker.resolve("progress.json"))
            assertFails { store.write("new progress") }
            assertEquals("existing user data", Files.readString(blocker))
        } finally { Files.deleteIfExists(blocker) }
    }
}
