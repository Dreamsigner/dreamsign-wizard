package com.duolingo.app.media

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import java.io.ByteArrayOutputStream
import java.lang.ref.WeakReference

private const val MICROPHONE_REQUEST = 4831
private var recordingActivity = WeakReference<Activity>(null)
private var permissionInFlight = false
private val permissionWaiters = mutableListOf<(Boolean) -> Unit>()

fun initializeAndroidRecording(activity: Activity) { recordingActivity = WeakReference(activity) }

/** Return true if this permission result belongs to recording. Called from MainActivity. */
fun handleAndroidRecordingPermissionResult(requestCode: Int, grantResults: IntArray): Boolean {
    if (requestCode != MICROPHONE_REQUEST) return false
    permissionInFlight = false
    val callbacks = permissionWaiters.toList()
    permissionWaiters.clear()
    val granted = grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
    callbacks.forEach { it(granted) }
    return true
}

private fun requestMicrophone(callback: (Boolean) -> Unit): () -> Unit {
    val activity = recordingActivity.get()
    if (activity == null || activity.isFinishing || activity.isDestroyed) {
        callback(false)
        return {}
    }
    if (activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
        callback(true)
        return {}
    }
    permissionWaiters.add(callback)
    if (!permissionInFlight) {
        permissionInFlight = true
        try { activity.requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), MICROPHONE_REQUEST) }
        catch (_: Exception) {
            permissionInFlight = false
            val callbacks = permissionWaiters.toList(); permissionWaiters.clear()
            callbacks.forEach { it(false) }
        }
    }
    return { permissionWaiters.remove(callback); Unit }
}

actual fun createAudioRecorder(): PlatformAudioRecorder = AndroidAudioRecorder()

private class AndroidAudioRecorder : PlatformAudioRecorder {
    private class Capture(val token: Long) {
        @Volatile var stopped = false
        @Volatile var cancelled = false
        @Volatile var started = false
        @Volatile var recorder: AudioRecord? = null
    }
    private val main = Handler(Looper.getMainLooper())
    private var generation = 0L
    private var active: Capture? = null
    private var cancelPermission: (() -> Unit)? = null
    private var deadline: Runnable? = null

    override fun start(maxDurationSeconds: Int, onStarted: () -> Unit, onComplete: (ByteArray) -> Unit, onError: (String) -> Unit) = onUi {
        cancelCurrent()
        if (maxDurationSeconds <= 0) { onError("La durée maximale doit être positive."); return@onUi }
        val capture = Capture(generation)
        active = capture
        val limit = maxDurationSeconds.coerceAtMost(MAX_RECORDING_SECONDS) * RECORDING_RATE * 2
        cancelPermission = requestMicrophone { granted ->
            if (active === capture && generation == capture.token && !capture.cancelled) {
                if (!granted) {
                    active = null; generation++
                    onError("Le microphone n’est pas autorisé. Autorise-le dans les réglages de l’application pour enregistrer.")
                } else Thread({
                    val output = ByteArrayOutputStream()
                    var failure: String? = null
                    var input: AudioRecord? = null
                    try {
                        val minimum = AudioRecord.getMinBufferSize(RECORDING_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
                        check(minimum > 0)
                        val bufferSize = maxOf(minimum, 4096).let { it + it % 2 }
                        val recorder = AudioRecord(MediaRecorder.AudioSource.MIC, RECORDING_RATE,
                            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize)
                        input = recorder
                        check(recorder.state == AudioRecord.STATE_INITIALIZED)
                        capture.recorder = recorder
                        if (!capture.cancelled && !capture.stopped) {
                            recorder.startRecording()
                            check(recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING)
                            capture.started = true
                            onUi {
                                if (active === capture && generation == capture.token && !capture.stopped) {
                                    val timer = Runnable { if (active === capture && generation == capture.token) stop() }
                                    deadline = timer
                                    main.postDelayed(timer, maxDurationSeconds.coerceAtMost(MAX_RECORDING_SECONDS) * 1000L)
                                    onStarted()
                                }
                            }
                            val buffer = ByteArray(bufferSize)
                            while (!capture.cancelled && !capture.stopped && output.size() < limit) {
                                val count = recorder.read(buffer, 0, minOf(buffer.size, limit - output.size()), AudioRecord.READ_BLOCKING)
                                if (count > 0) output.write(buffer, 0, count)
                                else if (!capture.stopped && !capture.cancelled) error("Capture microphone interrompue")
                            }
                        }
                    } catch (_: Exception) {
                        if (!capture.stopped && !capture.cancelled) failure = "Impossible d’enregistrer le microphone. Vérifie les autorisations et les autres applications audio."
                    } finally {
                        capture.recorder = null
                        runCatching { input?.stop() }; runCatching { input?.release() }
                    }
                    val raw = output.toByteArray()
                    val pcm = if (raw.size % 2 == 0) raw else raw.copyOf(raw.size - 1)
                    onUi {
                        if (active === capture && generation == capture.token && !capture.cancelled) {
                            deadline?.let { main.removeCallbacks(it) }; deadline = null
                            active = null; generation++
                            val message = failure
                            if (message != null) onError(message)
                            else if (pcm.isEmpty()) onError("Aucun son n’a été enregistré.")
                            else onComplete(pcmRecordingToWav(pcm))
                        }
                    }
                }, "atelier-microphone").apply { isDaemon = true }.start()
            }
        }
    }

    override fun stop() = onUi {
        deadline?.let { main.removeCallbacks(it) }; deadline = null
        active?.let { capture ->
            // A worker that has just released its recorder still has a completion queued.
            // Do not discard that valid capture when Stop races with normal finalization.
            if (capture.recorder == null && !capture.started) cancelCurrent()
            else { capture.stopped = true; runCatching { capture.recorder?.stop() } }
        }
    }
    override fun cancel() = onUi { cancelCurrent() }
    private fun cancelCurrent() {
        deadline?.let { main.removeCallbacks(it) }; deadline = null
        generation++
        cancelPermission?.invoke(); cancelPermission = null
        active?.let { it.cancelled = true; it.stopped = true; runCatching { it.recorder?.stop() } }
        active = null
    }
    private fun onUi(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) action() else main.post { action() }
    }
}
