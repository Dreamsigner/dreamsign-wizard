package com.dreamsigner.wizard.media

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import java.io.File

@Volatile private var audioContext: Context? = null

/** Pass applicationContext, never retain an Activity. Called before composing App(). */
fun initializeAndroidAudio(context: Context) { audioContext = context.applicationContext }

internal fun androidAudioApplication(): android.app.Application =
    checkNotNull(audioContext as? android.app.Application) { "Initialize Android audio before binding its lifecycle." }

actual fun createAudioPlayer(): PlatformAudioPlayer = AndroidAudioPlayer()

/** https://developer.android.com/reference/android/media/MediaPlayer */
private class AndroidAudioPlayer : PlatformAudioPlayer {
    private val main = Handler(Looper.getMainLooper())
    private var generation = 0L
    private var active: MediaPlayer? = null
    private var activeFile: File? = null

    override fun play(bytes: ByteArray, onComplete: () -> Unit, onError: (String) -> Unit) {
        val recording = bytes.copyOf()
        onUi {
            stopCurrent()
            val token = generation
            val context = audioContext
            if (context == null || recording.isEmpty()) {
                onError(if (context == null) "Le lecteur audio n’est pas initialisé." else "L’enregistrement audio est vide.")
                return@onUi
            }
            Thread({
                var temporary: File? = null
                try {
                    val file = File.createTempFile("wizard-audio-", ".wav", context.cacheDir)
                    temporary = file
                    file.outputStream().use { it.write(recording) }
                    onUi {
                        if (token != generation) {
                            file.delete()
                        } else {
                            try {
                                val player = MediaPlayer()
                                active = player // Strong reference through preparation and playback.
                                activeFile = file
                                player.setAudioAttributes(AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_MEDIA)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                                player.setOnPreparedListener {
                                    if (token == generation && active === player) {
                                        try { player.start() } catch (_: Exception) {
                                            stopCurrent()
                                            onError("Impossible de démarrer la lecture audio.")
                                        }
                                    }
                                }
                                player.setOnCompletionListener {
                                    if (token == generation && active === player) {
                                        stopCurrent()
                                        onComplete()
                                    }
                                }
                                player.setOnErrorListener { _, _, _ ->
                                    if (token == generation && active === player) {
                                        stopCurrent()
                                        onError("Impossible de décoder ou de lire cet enregistrement audio.")
                                    }
                                    true // Consume errors: MediaPlayer must not also invoke completion.
                                }
                                player.setDataSource(file.absolutePath)
                                player.prepareAsync()
                            } catch (_: Exception) {
                                stopCurrent()
                                file.delete()
                                onError("Impossible d’ouvrir l’enregistrement audio sur cet appareil.")
                            }
                        }
                    }
                } catch (_: Exception) {
                    temporary?.delete()
                    onUi {
                        if (token == generation) {
                            stopCurrent()
                            onError("Impossible de préparer le fichier audio temporaire.")
                        }
                    }
                }
            }, "wizard-audio-loader").apply { isDaemon = true }.start()
        }
    }

    override fun stop() = onUi { stopCurrent() }

    private fun stopCurrent() {
        generation++
        val player = active
        val file = activeFile
        active = null
        activeFile = null
        if (player != null) {
            player.setOnPreparedListener(null)
            player.setOnCompletionListener(null)
            player.setOnErrorListener(null)
            // release is valid while preparing too; stop() is not valid in every MediaPlayer state.
            runCatching { player.release() }
        }
        file?.delete()
    }

    private fun onUi(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) action() else main.post { action() }
    }
}
