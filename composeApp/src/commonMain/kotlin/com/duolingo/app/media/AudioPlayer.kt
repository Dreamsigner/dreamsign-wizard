package com.duolingo.app.media

/**
 * One local recording at a time. Keep this instance for the lifetime of its owning screen.
 * [play] replaces any earlier recording. [stop] cancels loading/playback and never calls
 * onComplete. Neither cancellation nor decoder failure counts as a completed listening.
 *
 * Call play/stop from the platform UI thread. All callbacks run on that same UI event loop
 * (Swing EDT, Android main Looper, iOS main queue), at most once per current playback.
 * Call stop on navigation, screen disposal and backgrounding. PCM WAV bytes are supported
 * on all targets; no network access or text-to-speech service is performed by this API.
 */
interface PlatformAudioPlayer {
    fun play(bytes: ByteArray, onComplete: () -> Unit, onError: (String) -> Unit)
    fun stop()
}

expect fun createAudioPlayer(): PlatformAudioPlayer
