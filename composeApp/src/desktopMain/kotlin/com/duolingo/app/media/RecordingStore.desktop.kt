package com.duolingo.app.media

import java.nio.channels.FileChannel
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

private val applicationRecordingStore by lazy {
    val root = System.getenv("LOCALAPPDATA")?.takeIf { it.isNotBlank() } ?: System.getProperty("user.home")
    FileAudioRecordingStore(Path.of(root, "AtelierApprentissage", "recordings"))
}

actual fun saveAudioRecording(exerciseId: String, bytes: ByteArray) = applicationRecordingStore.save(exerciseId, bytes)
actual fun loadAudioRecording(exerciseId: String): ByteArray? = applicationRecordingStore.load(exerciseId)

/** Injectable directory prevents tests from touching live learner recordings.
 * atomicReplace is internal fault injection; production always uses atomic Files.move.
 */
internal class FileAudioRecordingStore(
    private val directory: Path,
    private val atomicReplace: (Path, Path) -> Unit = { source, destination ->
        Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        Unit
    }
) {
    private val recordingLock = Any()
    private fun recordingPath(id: String): Path {
        validateRecordingId(id)
        return directory.resolve("$id.wav")
    }

    fun save(exerciseId: String, bytes: ByteArray) = synchronized(recordingLock) {
        validateRecordingBytes(bytes)
        val destination = recordingPath(exerciseId)
        Files.createDirectories(directory)
        val temporary = Files.createTempFile(directory, "recording-", ".tmp")
        try {
            FileChannel.open(temporary, StandardOpenOption.WRITE).use { channel ->
                val buffer = ByteBuffer.wrap(bytes)
                while (buffer.hasRemaining()) channel.write(buffer)
                channel.force(true)
            }
            // Fail rather than risk a destructive fallback on unsupported filesystems.
            atomicReplace(temporary, destination)
        } finally { Files.deleteIfExists(temporary) }
    }

    fun load(exerciseId: String): ByteArray? = synchronized(recordingLock) {
        val path = recordingPath(exerciseId)
        if (!Files.exists(path)) null else {
            require(Files.size(path) in 44L..MAX_RECORDING_BYTES.toLong()) { "Taille d’enregistrement invalide." }
            Files.readAllBytes(path).also(::validateRecordingBytes)
        }
    }
}
