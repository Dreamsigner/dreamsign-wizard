package com.dreamsigner.wizard.ui.studio

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Independent store. Failed reads prevent saves so an unreadable file is never overwritten. */
internal class DesktopKnowledgeGraphStore(private val file: Path) {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = false }
    @Synchronized fun load(): KnowledgeGraph = if (Files.notExists(file)) KnowledgeGraph() else json.decodeFromString<KnowledgeGraph>(Files.readString(file)).validate()
    @Synchronized fun save(graph: KnowledgeGraph) {
        graph.validate()
        load() // Refuse to replace corrupt or newer data, even on a fresh instance.
        val parent = file.toAbsolutePath().parent
        Files.createDirectories(parent)
        val temp = Files.createTempFile(parent, "knowledge-graph-", ".tmp")
        try {
            Files.writeString(temp, json.encodeToString(graph))
            // Fail safely if the filesystem cannot provide an atomic replacement.
            Files.move(temp, file.toAbsolutePath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } finally { Files.deleteIfExists(temp) }
    }
}
