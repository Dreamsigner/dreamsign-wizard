package com.dreamsigner.wizard.media

import java.io.ByteArrayOutputStream
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.TargetDataLine
import javax.swing.SwingUtilities
import javax.swing.Timer

actual fun createAudioRecorder(): PlatformAudioRecorder = DesktopAudioRecorder()

/** Tests inject a synthetic line; production keeps the normal JavaSound factory. */
internal class DesktopAudioRecorder(
    private val lineFactory: (AudioFormat) -> TargetDataLine = { AudioSystem.getTargetDataLine(it) }
) : PlatformAudioRecorder {
    private class Capture(val token: Long) {
        @Volatile var stopped = false
        @Volatile var cancelled = false
        @Volatile var line: TargetDataLine? = null
    }
    private var generation = 0L
    private var active: Capture? = null
    private var deadline: Timer? = null

    override fun start(maxDurationSeconds: Int, onStarted: () -> Unit, onComplete: (ByteArray) -> Unit, onError: (String) -> Unit) = onUi {
        cancelCurrent()
        if (maxDurationSeconds <= 0) { onError("La durée maximale doit être positive."); return@onUi }
        val capture = Capture(generation)
        active = capture
        val limit = maxDurationSeconds.coerceAtMost(MAX_RECORDING_SECONDS) * RECORDING_RATE * 2
        Thread({
            val output = ByteArrayOutputStream()
            var failure: String? = null
            var line: TargetDataLine? = null
            try {
                val format = AudioFormat(RECORDING_RATE.toFloat(), 16, 1, true, false)
                val input = lineFactory(format)
                line = input
                input.open(format)
                capture.line = input
                if (!capture.cancelled && !capture.stopped) {
                    input.start()
                    onUi {
                        if (active === capture && capture.token == generation && !capture.stopped) {
                            deadline = Timer(maxDurationSeconds.coerceAtMost(MAX_RECORDING_SECONDS) * 1000) {
                                if (active === capture && capture.token == generation) stop()
                            }.apply {
                                isRepeats = false; start()
                            }
                            onStarted()
                        }
                    }
                    val buffer = ByteArray(4096)
                    while (!capture.stopped && !capture.cancelled && output.size() < limit) {
                        val count = input.read(buffer, 0, minOf(buffer.size, limit - output.size()))
                        if (count > 0) output.write(buffer, 0, count)
                        else if (!capture.stopped && !capture.cancelled) error("Microphone interrompu")
                    }
                }
            } catch (_: Exception) {
                if (!capture.stopped && !capture.cancelled) failure = "Impossible de capturer le microphone. Vérifie le périphérique et les autorisations Windows."
            } finally {
                capture.line = null
                runCatching { line?.stop() }; runCatching { line?.close() }
            }
            val pcm = output.toByteArray()
            onUi {
                if (active === capture && capture.token == generation && !capture.cancelled) {
                    deadline?.stop(); deadline = null
                    active = null; generation++
                    val message = failure
                    if (message != null) onError(message)
                    else if (pcm.isEmpty()) onError("Aucun son n’a été enregistré.")
                    else onComplete(pcmRecordingToWav(pcm))
                }
            }
        }, "wizard-microphone").apply { isDaemon = true }.start()
    }

    override fun stop() = onUi {
        deadline?.stop(); deadline = null
        active?.let { capture ->
            capture.stopped = true
            runCatching { capture.line?.stop() }
            runCatching { capture.line?.close() } // Unblock TargetDataLine.read on its worker.
        }
    }
    override fun cancel() = onUi { cancelCurrent() }
    private fun cancelCurrent() {
        deadline?.stop(); deadline = null
        generation++
        active?.let { capture ->
            capture.cancelled = true; capture.stopped = true
            runCatching { capture.line?.stop() }; runCatching { capture.line?.close() }
        }
        active = null
    }
    private fun onUi(action: () -> Unit) {
        if (SwingUtilities.isEventDispatchThread()) action() else SwingUtilities.invokeLater { action() }
    }
}
