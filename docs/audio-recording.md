# Local oral recording

## Contract and format

`createAudioRecorder()` supplies a native `PlatformAudioRecorder`. Call `start(maxDurationSeconds, onStarted, onComplete, onError)`, `stop()` and `cancel()` on the UI event loop. Callbacks return on Swing EDT, Android main Looper or iOS main queue. The request is bounded to at most 270 seconds; nonpositive limits report an error. Capture uses signed little-endian PCM, mono, 16 bits, 16,000 Hz, inside a WAV container compatible with the existing native player. Starting again cancels unfinished capture. No network, speech recognition, AI correction, external service or automatic transmission is implemented.

`onStarted` means the native recorder has actually started, after any native permission request. `stop` finalizes the captured sound and returns bytes once, or reports no usable recording. `cancel` and stale callbacks after cancel/replacement never deliver recording bytes. Stopping while permission is still pending discards that pending attempt because there is no recording to finalize. A failed/denied/cancelled attempt never modifies a saved recording. The caller must explicitly save successful bytes, and should retain the previous in-memory draft until that save succeeds.

Retain one recorder per screen. Bind `bindAudioRecorderLifecycle(recorder, onBackground)` and dispose by unbinding, cancelling pending UI jobs and calling `recorder.cancel()`. This reuses the native foreground observers, deliberately substituting cancel for finalizing stop. Stop/release playback before beginning capture and cancel capture before replay: the app currently owns one audio session. Show errors and native permission refusal in the app; do not mark a task complete merely because permission was requested or the microphone started.

## Platform implementation

- Windows: JavaSound TargetDataLine captures on a daemon worker. A byte cap and UI timer bound duration; stop/close unblock its read. Generation ownership suppresses stale started/completed callbacks, and the line closes in finally. No capture file is written. Device unavailability/Windows microphone privacy restrictions produce an error; this API does not change OS privacy settings.
- Android: MainActivity retains only a weak Activity reference for native RECORD_AUDIO requests at the first recording tap. Permission callbacks are delegated through onRequestPermissionsResult. Application context remains separate from the weak Activity. AudioRecord reads PCM on a daemon worker and releases in finally. A byte cap and Handler deadline bound duration. Cancellation removes pending permission callbacks and stops the input to unblock read. The manifest must declare RECORD_AUDIO (integration owner adds it). If the permission dialog itself pauses the Activity, the conservative lifecycle binding may cancel that attempt; the user can tap record again after granting permission. There is no background microphone service.
- iOS: NSMicrophoneUsageDescription explains local capture; AVAudioSession requests microphone permission only on start. AVAudioRecorder uses linear PCM settings and a private temporary WAV with recordForDuration. A strong delegate field protects the weak native delegate. Manual stop finalizes before reading bytes; automatic finish, decode failure and interruption are guarded by generation. Cancel/failure detach the delegate, stop capture, release the audio session and delete only the temporary file. An interruption never delivers a completed recording.

Native permission UI is intentionally left to the user inside the app. No microphone access has been requested or automated by this coding task.

## Private recording store

`saveAudioRecording(exerciseId, bytes)` and `loadAudioRecording(exerciseId)` are blocking IO APIs: use an IO dispatcher. IDs must match `[A-Za-z0-9][A-Za-z0-9_-]{0,127}`. The API never accepts a caller-provided path. Recording files have bounded sizes and must have RIFF/WAVE signatures; absent files return null and corrupt/unreadable files throw. Container signature checks are not a speech-quality assessment.

- Windows: `%LOCALAPPDATA%/AtelierApprentissage/recordings/<exerciseId>.wav` (user-home fallback only when LOCALAPPDATA is unavailable). Write a sibling temporary file, force the channel to disk, then ATOMIC_MOVE replacement. An unsupported atomic move fails instead of risking a destructive fallback. The temporary is removed in finally.
- Android: application filesDir/recordings. Write and fsync a sibling temporary file, then use Android Os.rename for atomic replacement on the same private filesystem. A failing rename preserves the previous destination. The temporary is removed in finally. No shared/external storage is used.
- iOS: application Documents/recordings. NSLock serializes access and NSData.writeToFile(atomically=true) replaces only after its temporary write succeeds. Foundation manages the atomic-write temporary. The recordings directory is marked NSURLIsExcludedFromBackupKey; failure to apply that exclusion prevents a save/load rather than silently promising private-only retention.

Android integration must exclude filesDir/recordings in fullBackupContent/dataExtractionRules because the existing manifest allows OS backup. Without that separate configuration, app-local storage alone does not prevent the operating system from backing up recordings. The recording code performs no application-level transmission.

No saved file is deleted by recorder cancellation. Failed saves throw so UI can keep the previous draft and explain that new audio was not saved. Each platform serializes its store operations within this process; a future multi-process writer would need additional coordination.

## Validation handoff

No builds, capture tests or microphone permission interactions were executed by this subtask. Desktop compilation/integration belongs to the root task; Android and iOS bindings require native CI/device builds. The Apple documentation pages expose little detail without JavaScript, so macOS compilation is still required for the generated Kotlin/Native method names.

Exercise: permission granted/refused/permanently refused; cancellation during permission; stop before initialization; start/start; stop twice; empty capture; manual stop; duration cap; cancel/background/OS interruption; navigate while initializing; unavailable microphone; captured WAV replay; saving/reloading after restart; invalid IDs; malformed files; disk-full/write-denied replacement retaining the earlier draft; temporary and native handle cleanup. Check UI state resets even when cancellation intentionally emits no completion callback. Audio presence and self-review do not certify intelligibility or TCF performance.

## API references consulted

- [Java TargetDataLine](https://docs.oracle.com/en/java/javase/17/docs/api/java.desktop/javax/sound/sampled/TargetDataLine.html).
- [Android AudioRecord](https://developer.android.com/reference/android/media/AudioRecord), [runtime permissions](https://developer.android.com/training/permissions/requesting), and [Os.rename](https://developer.android.com/reference/android/system/Os#rename(java.lang.String,%20java.lang.String)).
- [Apple AVAudioRecorder](https://developer.apple.com/documentation/avfaudio/avaudiorecorder), [requestRecordPermission](https://developer.apple.com/documentation/avfaudio/avaudiosession/requestrecordpermission(_:)), [NSData atomic write](https://developer.apple.com/documentation/foundation/nsdata/write(tofile:atomically:)), and [backup exclusion resource key](https://developer.apple.com/documentation/foundation/urlresourcekey/isexcludedfrombackupkey).
