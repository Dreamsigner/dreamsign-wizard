package com.dreamsigner.wizard.storage

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.AtomicMoveNotSupportedException
import java.time.LocalDate

actual fun createProgressStore(): ProgressStore {
    val base = System.getenv("LOCALAPPDATA")?.takeIf { it.isNotBlank() }
        ?: System.getProperty("user.home")
    return FileProgressStore(Path.of(base, "DreamsignWizard", "progress.json"))
}

/** Atomic replacement keeps the previous file intact when a write cannot finish. */
internal class FileProgressStore(private val path: Path) : ProgressStore {
    override fun read(): String? = if (Files.exists(path)) Files.readString(path) else null
    override fun write(value: String) {
        Files.createDirectories(path.parent)
        val temporary = Files.createTempFile(path.parent, "progress-", ".tmp")
        try {
            Files.writeString(temporary, value)
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(temporary)
        }
    }
}

actual fun todayLocalDate(): String = LocalDate.now().toString()
