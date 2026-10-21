package com.duolingo.app.media

/** UI-thread API; callbacks run on the platform UI loop. Keep the recorder until disposal.
 * start replaces/cancels unfinished capture. stop finalizes; cancel never completes.
 * The upper duration limit is 270 seconds. A failure never changes a saved recording.
 */
interface PlatformAudioRecorder {
    fun start(maxDurationSeconds: Int, onStarted: () -> Unit, onComplete: (ByteArray) -> Unit, onError: (String) -> Unit)
    fun stop()
    fun cancel()
}

expect fun createAudioRecorder(): PlatformAudioRecorder

/** Reuse the native foreground observers; interruption cancels rather than finalizes. */
fun bindAudioRecorderLifecycle(recorder: PlatformAudioRecorder, onBackground: () -> Unit): () -> Unit =
    bindAudioLifecycle(object : PlatformAudioPlayer {
        override fun stop() = recorder.cancel()
        override fun play(bytes: ByteArray, onComplete: () -> Unit, onError: (String) -> Unit) =
            onError("Cet adaptateur gère uniquement le cycle de vie de l’enregistreur.")
    }, onBackground)

internal const val RECORDING_RATE = 16000
internal const val MAX_RECORDING_SECONDS = 270
internal const val MAX_RECORDING_BYTES = RECORDING_RATE * 2 * MAX_RECORDING_SECONDS + 65536

/** Encode little-endian signed PCM, mono, 16 bits / 16 kHz, with a canonical WAV header. */
internal fun pcmRecordingToWav(pcm: ByteArray): ByteArray {
    require(pcm.isNotEmpty() && pcm.size % 2 == 0 && pcm.size <= RECORDING_RATE * 2 * MAX_RECORDING_SECONDS)
    val result = ByteArray(44 + pcm.size)
    fun text(offset: Int, value: String) { value.forEachIndexed { i, c -> result[offset + i] = c.code.toByte() } }
    fun number(offset: Int, value: Int, size: Int) { repeat(size) { result[offset + it] = (value ushr (8 * it)).toByte() } }
    text(0, "RIFF"); number(4, result.size - 8, 4); text(8, "WAVE")
    text(12, "fmt "); number(16, 16, 4); number(20, 1, 2); number(22, 1, 2)
    number(24, RECORDING_RATE, 4); number(28, RECORDING_RATE * 2, 4)
    number(32, 2, 2); number(34, 16, 2); text(36, "data"); number(40, pcm.size, 4)
    pcm.copyInto(result, 44)
    return result
}
