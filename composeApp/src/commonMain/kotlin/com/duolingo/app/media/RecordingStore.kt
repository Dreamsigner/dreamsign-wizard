package com.duolingo.app.media

/** Blocking local IO: call off the UI thread. Throws on failure, preserving the prior file.
 * Missing recordings return null; corrupt/oversized recordings throw rather than masquerade
 * as a missing draft. Exercise IDs, never arbitrary paths, identify private recordings.
 */
expect fun saveAudioRecording(exerciseId: String, bytes: ByteArray)
expect fun loadAudioRecording(exerciseId: String): ByteArray?

internal fun validateRecordingId(id: String) {
    require(Regex("[A-Za-z0-9][A-Za-z0-9_-]{0,127}").matches(id)) { "Identifiant d’exercice invalide." }
}

internal fun validateRecordingBytes(bytes: ByteArray) {
    recordingDurationMillis(bytes)
}

/** Validate PCM chunks and measure the recorded samples, never a UI wall-clock timer. */
internal fun recordingDurationMillis(bytes: ByteArray): Long {
    require(bytes.size in 44..MAX_RECORDING_BYTES) { "Taille d’enregistrement invalide." }
    fun tag(offset: Int, expected: String) = expected.indices.all { bytes[offset + it] == expected[it].code.toByte() }
    fun uint(offset: Int, size: Int): Long {
        require(offset >= 0 && offset + size <= bytes.size)
        return (0 until size).fold(0L) { value, i -> value or ((bytes[offset + i].toLong() and 255L) shl (8 * i)) }
    }
    require(tag(0, "RIFF") && tag(8, "WAVE")) { "Enregistrement WAV attendu." }
    require(uint(4, 4) + 8 == bytes.size.toLong()) { "Enregistrement tronqué." }
    var offset = 12
    var pcm = false
    var dataBytes = 0L
    while (offset + 8 <= bytes.size) {
        val length = uint(offset + 4, 4)
        require(length <= bytes.size - offset - 8L) { "Bloc audio tronqué." }
        val start = offset + 8
        if (tag(offset, "fmt ")) {
            require(length >= 16)
            pcm = uint(start, 2) == 1L && uint(start + 2, 2) == 1L &&
                uint(start + 4, 4) == RECORDING_RATE.toLong() &&
                uint(start + 8, 4) == RECORDING_RATE * 2L && uint(start + 12, 2) == 2L && uint(start + 14, 2) == 16L
        }
        if (tag(offset, "data")) dataBytes += length
        offset = (start + length + length % 2).toInt()
    }
    require(offset == bytes.size && pcm && dataBytes > 0 && dataBytes % 2 == 0L) { "PCM mono 16 bits, 16 kHz attendu." }
    val duration = dataBytes * 1000 / (RECORDING_RATE * 2)
    require(duration in 1L..(MAX_RECORDING_SECONDS * 1000L)) { "Durée d’enregistrement invalide." }
    return duration
}
