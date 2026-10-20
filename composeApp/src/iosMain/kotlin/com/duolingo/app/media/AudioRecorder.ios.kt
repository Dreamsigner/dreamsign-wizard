@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.duolingo.app.media

import platform.AVFAudio.*
import platform.CoreAudioTypes.kAudioFormatLinearPCM
import platform.Foundation.*
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

actual fun createAudioRecorder(): PlatformAudioRecorder = IosAudioRecorder()

private class IosAudioRecorder : PlatformAudioRecorder {
    private var generation = 0L
    private var active: AVAudioRecorder? = null
    private var activeDelegate: RecorderDelegate? = null // Native delegate is weak.
    private var temporaryPath: String? = null
    private var ownsAudioSession = false
    private var complete: ((ByteArray) -> Unit)? = null
    private var failed: ((String) -> Unit)? = null
    private var removeInterruptionObserver: (() -> Unit)? = null

    override fun start(maxDurationSeconds: Int, onStarted: () -> Unit, onComplete: (ByteArray) -> Unit, onError: (String) -> Unit) = onUi {
        cancelCurrent()
        if (maxDurationSeconds <= 0) { onError("La durée maximale doit être positive."); return@onUi }
        val token = generation
        AVAudioSession.sharedInstance().requestRecordPermission { granted ->
            onUi {
                if (generation == token) {
                    if (!granted) {
                        generation++
                        onError("Le microphone n’est pas autorisé. Autorise-le dans les réglages de l’application pour enregistrer.")
                    } else {
                        try {
                            val session = AVAudioSession.sharedInstance()
                            check(session.setCategory(AVAudioSessionCategoryPlayAndRecord, error = null))
                            check(session.setActive(true, error = null))
                            ownsAudioSession = true
                            val path = NSTemporaryDirectory() + "atelier-recording-" + NSUUID().UUIDString + ".wav"
                            temporaryPath = path
                            val settings = mapOf<Any?, Any?>(
                                AVFormatIDKey to kAudioFormatLinearPCM.toInt(),
                                AVSampleRateKey to RECORDING_RATE.toDouble(),
                                AVNumberOfChannelsKey to 1,
                                AVLinearPCMBitDepthKey to 16,
                                AVLinearPCMIsFloatKey to false,
                                AVLinearPCMIsBigEndianKey to false
                            )
                            val recorder = AVAudioRecorder(NSURL.fileURLWithPath(path), settings, null)
                            val delegate = RecorderDelegate { success, message ->
                                onUi {
                                    if (generation == token && active === recorder) finish(success, message)
                                }
                            }
                            active = recorder; activeDelegate = delegate
                            complete = onComplete; failed = onError
                            val center = NSNotificationCenter.defaultCenter
                            val observer = center.addObserverForName(AVAudioSessionInterruptionNotification, null, NSOperationQueue.mainQueue) {
                                if (generation == token && active === recorder) {
                                    finish(false, "Le microphone a été interrompu par le système. Le précédent brouillon est conservé.")
                                }
                            }
                            removeInterruptionObserver = { center.removeObserver(observer) }
                            recorder.delegate = delegate
                            check(recorder.prepareToRecord())
                            check(recorder.recordForDuration(maxDurationSeconds.coerceAtMost(MAX_RECORDING_SECONDS).toDouble()))
                            onStarted()
                        } catch (_: Exception) {
                            if (generation == token) {
                                cancelCurrent()
                                onError("Impossible de démarrer la capture du microphone.")
                            }
                        }
                    }
                }
            }
        }
    }

    override fun stop() = onUi {
        if (active != null) {
            // stop synchronously finalizes the WAV container. Detach before finalizing;
            // a queued delegate event cannot also deliver the same recording.
            active?.delegate = null
            active?.stop()
            finish(true, "Impossible de finaliser l’enregistrement.")
        } else cancelCurrent() // Permission pending: no recording to finalize.
    }
    override fun cancel() = onUi { cancelCurrent() }

    private fun finish(success: Boolean, message: String) {
        val completion = complete
        val failure = failed
        val path = temporaryPath
        val bytes = if (success && path != null) runCatching {
            val data = NSData.dataWithContentsOfFile(path) ?: error("Enregistrement illisible")
            recordingDataBytes(data)
        }.getOrNull() else null
        cancelCurrent() // Delete temporary only; this never modifies the saved draft.
        if (bytes != null) completion?.invoke(bytes)
        else failure?.invoke(if (success) "Aucun enregistrement WAV valide n’a été capturé." else message)
    }

    private fun cancelCurrent() {
        generation++
        removeInterruptionObserver?.invoke()
        removeInterruptionObserver = null
        active?.delegate = null
        active?.stop()
        active = null; activeDelegate = null
        complete = null; failed = null
        temporaryPath?.let { NSFileManager.defaultManager.removeItemAtPath(it, null) }
        temporaryPath = null
        if (ownsAudioSession) {
            ownsAudioSession = false
            AVAudioSession.sharedInstance().setActive(false, error = null)
        }
    }
    private fun onUi(action: () -> Unit) {
        if (NSThread.isMainThread) action() else dispatch_async(dispatch_get_main_queue()) { action() }
    }
}

private class RecorderDelegate(private val finished: (Boolean, String) -> Unit) : NSObject(), AVAudioRecorderDelegateProtocol {
    override fun audioRecorderDidFinishRecording(recorder: AVAudioRecorder, successfully: Boolean) {
        finished(successfully, "La capture audio s’est terminée avec une erreur.")
    }
    override fun audioRecorderEncodeErrorDidOccur(recorder: AVAudioRecorder, error: NSError?) {
        finished(false, "Impossible d’encoder la capture audio.")
    }
    override fun audioRecorderBeginInterruption(recorder: AVAudioRecorder) {
        finished(false, "L’enregistrement a été interrompu. Le brouillon précédent est conservé.")
    }
}
