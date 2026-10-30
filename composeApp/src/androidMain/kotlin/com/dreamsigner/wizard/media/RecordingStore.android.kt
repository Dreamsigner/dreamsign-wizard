package com.dreamsigner.wizard.media

import android.system.Os
import java.io.File

private val recordingLock = Any()
private fun recordingFile(id: String): File {
    validateRecordingId(id)
    val folder = File(androidAudioApplication().filesDir, "recordings")
    check(folder.isDirectory || folder.mkdirs()) { "Stockage audio privé indisponible." }
    return File(folder, "$id.wav")
}

actual fun saveAudioRecording(exerciseId: String, bytes: ByteArray) = synchronized(recordingLock) {
    validateRecordingBytes(bytes)
    val destination = recordingFile(exerciseId)
    val temporary = File.createTempFile("recording-", ".tmp", destination.parentFile)
    try {
        temporary.outputStream().use { stream -> stream.write(bytes); stream.fd.sync() }
        // Same private filesystem: POSIX rename either replaces atomically or throws,
        // preserving the destination on failure. Available since Android API 21.
        Os.rename(temporary.absolutePath, destination.absolutePath)
    } finally { temporary.delete() }
}

actual fun loadAudioRecording(exerciseId: String): ByteArray? = synchronized(recordingLock) {
    val file = recordingFile(exerciseId)
    if (!file.exists()) return@synchronized null
    file.inputStream().use {
        require(it.channel.size() in 44L..MAX_RECORDING_BYTES.toLong()) { "Taille d’enregistrement invalide." }
        it.readBytes().also(::validateRecordingBytes)
    }
}
