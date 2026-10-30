package com.dreamsigner.wizard.media

import java.io.ByteArrayInputStream
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.LineEvent
import javax.sound.sampled.LineListener
import javax.swing.SwingUtilities

actual fun createAudioPlayer(): PlatformAudioPlayer = DesktopAudioPlayer()

/** https://docs.oracle.com/en/java/javase/17/docs/api/java.desktop/javax/sound/sampled/Clip.html */
private class DesktopAudioPlayer : PlatformAudioPlayer {
    private var generation = 0L // All lifecycle state is confined to the Swing EDT.
    private var active: Clip? = null
    private var activeListener: LineListener? = null

    override fun play(bytes: ByteArray, onComplete: () -> Unit, onError: (String) -> Unit) {
        val recording = bytes.copyOf()
        onUi {
            stopCurrent()
            val token = generation
            if (recording.isEmpty()) {
                onError("L’enregistrement audio est vide.")
                return@onUi
            }
            Thread({
                var opened: Clip? = null
                try {
                    AudioSystem.getAudioInputStream(ByteArrayInputStream(recording)).use { stream ->
                        require(stream.format.encoding == AudioFormat.Encoding.PCM_SIGNED ||
                            stream.format.encoding == AudioFormat.Encoding.PCM_UNSIGNED) { "Format audio PCM attendu." }
                        opened = AudioSystem.getClip()
                        opened!!.open(stream)
                    }
                    val clip = checkNotNull(opened)
                    require(clip.frameLength > 0) { "L’enregistrement ne contient aucun son." }
                    onUi {
                        if (token != generation) {
                            clip.close()
                        } else {
                            active = clip
                            val listener = LineListener { event ->
                                if (event.type == LineEvent.Type.STOP || event.type == LineEvent.Type.CLOSE) {
                                    val finished = event.type == LineEvent.Type.STOP &&
                                        clip.frameLength > 0 && clip.longFramePosition >= clip.frameLength.toLong()
                                    onUi {
                                        if (token == generation && active === clip) {
                                            stopCurrent()
                                            if (finished) onComplete()
                                            else onError("La lecture audio a été interrompue avant la fin.")
                                        }
                                    }
                                }
                            }
                            activeListener = listener
                            clip.addLineListener(listener)
                            try {
                                clip.start()
                            } catch (_: Exception) {
                                stopCurrent()
                                onError("Impossible de démarrer la lecture audio sur cet appareil.")
                            }
                        }
                    }
                } catch (_: Exception) {
                    runCatching { opened?.close() }
                    onUi {
                        if (token == generation) {
                            stopCurrent()
                            onError("Impossible de lire cet enregistrement PCM WAV. Vérifie la sortie audio de l’appareil.")
                        }
                    }
                }
            }, "wizard-audio-loader").apply { isDaemon = true }.start()
        }
    }

    override fun stop() = onUi { stopCurrent() }

    private fun stopCurrent() {
        generation++ // Invalidates queued line events and still-loading workers before stop().
        val clip = active
        val listener = activeListener
        active = null
        activeListener = null
        if (clip != null) {
            if (listener != null) clip.removeLineListener(listener)
            runCatching { clip.stop() }
            runCatching { clip.flush() }
            runCatching { clip.close() }
        }
    }

    private fun onUi(action: () -> Unit) {
        if (SwingUtilities.isEventDispatchThread()) action() else SwingUtilities.invokeLater(action)
    }
}
