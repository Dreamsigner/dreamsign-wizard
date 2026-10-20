@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.duolingo.app.media

import kotlinx.cinterop.*
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioPlayerDelegateProtocol
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionInterruptionNotification
import platform.AVFAudio.setActive
import platform.Foundation.*
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

actual fun createAudioPlayer(): PlatformAudioPlayer = IosAudioPlayer()

/** https://developer.apple.com/documentation/avfaudio/avaudioplayerdelegate */
private class IosAudioPlayer : PlatformAudioPlayer {
    private var generation = 0L // Lifecycle/delegate work is serialized on the main queue.
    private var active: AVAudioPlayer? = null
    private var activeDelegate: PlayerDelegate? = null // AVAudioPlayer.delegate is weak.
    private var activeData: NSData? = null
    private var ownsAudioSession = false
    private var removeInterruptionObserver: (() -> Unit)? = null

    override fun play(bytes: ByteArray, onComplete: () -> Unit, onError: (String) -> Unit) {
        val recording = bytes.copyOf()
        onUi {
            stopCurrent()
            val token = generation
            if (recording.isEmpty()) {
                onError("L’enregistrement audio est vide.")
                return@onUi
            }
            try {
                val data = recording.usePinned { pinned ->
                    NSData.create(bytes = pinned.addressOf(0), length = recording.size.toULong())
                }
                val session = AVAudioSession.sharedInstance()
                if (!session.setCategory(AVAudioSessionCategoryPlayback, error = null) ||
                    !session.setActive(true, error = null)) {
                    onError("La sortie audio de l’appareil est indisponible.")
                    return@onUi
                }
                ownsAudioSession = true
                val player = AVAudioPlayer(data = data, error = null)
                val delegate = PlayerDelegate { success, message ->
                    onUi {
                        if (token == generation && active === player) {
                            stopCurrent()
                            if (success) onComplete() else onError(message)
                        }
                    }
                }
                active = player
                activeData = data
                activeDelegate = delegate
                val center = NSNotificationCenter.defaultCenter
                val observer = center.addObserverForName(AVAudioSessionInterruptionNotification, null, NSOperationQueue.mainQueue) {
                    if (token == generation && active === player) {
                        stopCurrent()
                        onError("La lecture audio a été interrompue. Relance l’enregistrement pour reprendre.")
                    }
                }
                removeInterruptionObserver = { center.removeObserver(observer) }
                player.delegate = delegate
                player.numberOfLoops = 0
                if (!player.prepareToPlay() || !player.play()) {
                    stopCurrent()
                    onError("Impossible de démarrer cet enregistrement audio.")
                }
            } catch (_: Exception) {
                if (token == generation) {
                    stopCurrent()
                    onError("Impossible de décoder ou de lire cet enregistrement audio.")
                }
            }
        }
    }

    override fun stop() = onUi { stopCurrent() }

    private fun stopCurrent() {
        generation++
        removeInterruptionObserver?.invoke()
        removeInterruptionObserver = null
        val player = active
        active = null
        // Detach the weak native delegate before stop; stop must not complete a lesson.
        player?.delegate = null
        player?.stop()
        activeDelegate = null
        activeData = null
        if (ownsAudioSession) {
            ownsAudioSession = false
            AVAudioSession.sharedInstance().setActive(false, error = null)
        }
    }

    private fun onUi(action: () -> Unit) {
        if (NSThread.isMainThread) action() else dispatch_async(dispatch_get_main_queue()) { action() }
    }
}

private class PlayerDelegate(private val finished: (Boolean, String) -> Unit) : NSObject(), AVAudioPlayerDelegateProtocol {
    override fun audioPlayerDidFinishPlaying(player: AVAudioPlayer, successfully: Boolean) {
        finished(successfully, "La lecture audio s’est terminée avec une erreur.")
    }

    override fun audioPlayerDecodeErrorDidOccur(player: AVAudioPlayer, error: NSError?) {
        finished(false, "Impossible de décoder cet enregistrement audio.")
    }

    override fun audioPlayerBeginInterruption(player: AVAudioPlayer) {
        finished(false, "La lecture audio a été interrompue. Relance l’enregistrement pour reprendre.")
    }
}
