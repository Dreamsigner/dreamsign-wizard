@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.dreamsigner.wizard.media

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.posix.memcpy

private val recordingLock = NSLock()
private fun recordingPath(id: String): String {
    validateRecordingId(id)
    val documents = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true).firstOrNull() as? String
        ?: error("Stockage audio privé indisponible.")
    val folder = "$documents/recordings"
    check(NSFileManager.defaultManager.createDirectoryAtPath(folder, true, null, null)) { "Stockage audio privé indisponible." }
    check(NSURL.fileURLWithPath(folder).setResourceValue(true, NSURLIsExcludedFromBackupKey, null)) {
        "Impossible d’exclure les enregistrements de la sauvegarde système."
    }
    return "$folder/$id.wav"
}

internal fun recordingDataBytes(data: NSData): ByteArray {
    require(data.length in 44uL..MAX_RECORDING_BYTES.toULong()) { "Taille d’enregistrement invalide." }
    return ByteArray(data.length.toInt()).also { result ->
        result.usePinned { pinned -> memcpy(pinned.addressOf(0), data.bytes, data.length) }
        validateRecordingBytes(result)
    }
}

actual fun saveAudioRecording(exerciseId: String, bytes: ByteArray) {
    validateRecordingBytes(bytes)
    recordingLock.lock()
    try {
        val path = recordingPath(exerciseId)
        val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
        check(data.writeToFile(path, atomically = true)) { "Impossible d’enregistrer le fichier audio ; ancien brouillon conservé." }
    } finally { recordingLock.unlock() }
}

actual fun loadAudioRecording(exerciseId: String): ByteArray? {
    recordingLock.lock()
    try {
        val path = recordingPath(exerciseId)
        if (!NSFileManager.defaultManager.fileExistsAtPath(path)) return null
        val attributes = NSFileManager.defaultManager.attributesOfItemAtPath(path, null)
        val size = (attributes?.get(NSFileSize) as? NSNumber)?.longLongValue
        require(size != null && size in 44L..MAX_RECORDING_BYTES.toLong()) { "Taille d’enregistrement invalide." }
        val data = NSData.dataWithContentsOfFile(path) ?: error("Enregistrement existant illisible.")
        return recordingDataBytes(data)
    } finally { recordingLock.unlock() }
}
