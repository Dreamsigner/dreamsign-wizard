package com.dreamsigner.wizard.ui.studio

import java.awt.FileDialog
import java.awt.Frame
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

internal fun studioExportFilename(goal: String): String {
    val stem = goal.replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), " ").trim().trimEnd('.')
        .replace(Regex("\\s+"), " ").take(80).trimEnd(' ', '.')
    val safe = if (stem.isBlank() || stem.uppercase().substringBefore('.') in setOf("CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9", "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9")) "seance" else stem
    return "$safe.md"
}

internal fun chooseStudioExportFile(suggestion: String): Path? {
    val dialog = FileDialog(null as Frame?, "Exporter la séance", FileDialog.SAVE)
    return try {
        dialog.file = suggestion
        dialog.isVisible = true
        val name = dialog.file ?: return null
        val filename = if (name.endsWith(".md", ignoreCase = true)) name else "$name.md"
        Path.of(dialog.directory, filename)
    } finally { dialog.dispose() }
}

/** Publish a complete file atomically without replacing an existing destination. */
internal fun writeStudioExport(destination: Path, markdown: String) {
    val target = destination.toAbsolutePath()
    val temporary = Files.createTempFile(target.parent, ".studio-export-", ".tmp")
    try {
        Files.writeString(temporary, markdown, StandardCharsets.UTF_8)
        Files.createLink(target, temporary)
    } finally {
        Files.deleteIfExists(temporary)
    }
}
