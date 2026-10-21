# Native local audio playback

## Contract

`PlatformAudioPlayer` exposes `play(bytes, onComplete, onError)` and `stop()`. `createAudioPlayer()` has desktop, Android and iOS actual implementations. Audio input is local PCM WAV data supplied by the caller; these players do not fetch URLs or synthesize speech. Keep one player instance per owning screen (for example with `remember`), then stop it when navigating, disposing that screen or moving the app to the background.

Call public methods from the platform UI event loop. Completion/error callbacks also run there: Swing EDT on desktop, main Looper on Android, main dispatch queue on iOS. Calls from another thread are marshalled to that loop, but cancellation ordering is only guaranteed relative to operations on the UI loop. A new play cancels the previous one; stop is safe during loading and repeated stop is harmless. Cancellation deliberately produces neither completion nor error. Every request has a generation identifier, so stale decoder events and queued completions from a stopped/replaced request are ignored. The caller should also reject a callback belonging to a lesson it no longer owns.

A genuine playback-end event produces onComplete once. Decode/start/device errors produce onError, never onComplete. This means transport playback finished: no player can establish that the user heard the recording, that device volume was audible or that headphones were worn. The VM should describe the feature as a completed playback, not certified listening comprehension.

## Platform lifetimes

### Windows / desktop

JavaSound `AudioSystem.getAudioInputStream` reads the bytes on a short-lived daemon loader thread. The stream closes after `Clip.open`; only PCM signed/unsigned formats with a positive frame length are accepted. The Clip is retained until stop/error/end, then its listener is removed before stopping/flushing/closing it. No temporary file is created. STOP is not sufficient evidence of completion: the current clip must also have reached its final frame. Unexpected earlier STOP/CLOSE reports an interruption. The Swing event queue serializes Clip start/release and callback delivery; loader threads from cancelled requests release their Clip when loading returns.

### Android

`initializeAndroidAudio(applicationContext)` is called in MainActivity before composing App; it retains no Activity. A daemon thread writes one WAV to the app cache. A MediaPlayer is then constructed on the main Looper with speech/media AudioAttributes and prepareAsync. The current instance and its cache file are retained until cleanup. Prepared/completion/error listeners check ownership; errors are consumed so MediaPlayer cannot also treat an error as completion. Cleanup clears listeners, calls release (also safe during preparing), then deletes the file. Cancelled loader work deletes its own file before returning. Normal stop/error/completion leaves no audio cache file; abrupt process termination or a filesystem deletion failure can leave an OS-manageable cache file. No shared storage or network permission is required for playback.

### iPhone / iPad

AVAudioPlayer owns an NSData copy and has a strongly retained NSObject delegate because its native delegate property is weak. Playback uses the app's AVAudioSession playback category, activated for the recording and deactivated when this player releases it. This app currently has one player; if future features use simultaneous audio, session ownership must become app-wide. Only `audioPlayerDidFinishPlaying(... successfully = true)` completes the recording. Failed completion, decode errors or interruptions report an error. Cleanup detaches the delegate before stop, releases the data/delegate/player and deactivates the audio session. No temporary file is written.

## Integration and QA

The caller supplies failure text to an accessible UI, exposes a stop/replay action and only marks playback complete from onComplete. Always call stop when replacing the owning lesson, including before loading another resource. Never set the completed flag after a delay, when bytes finish loading, on stop or on an error.

No builds or native playback tests were run by this implementation subtask. Review the desktop integration build, then listen on a real Windows audio device. Android needs emulator/device playback and interruption/background checks. iOS needs the macOS/Xcode build to verify generated Kotlin/Native Objective-C bindings, followed by simulator/device playback. Check valid WAV, invalid/empty input, repeated play, stop before load finishes, stop halfway, navigation while loading/playing, replay after completion, device interruption, unavailable output, one completion only, and Android temporary-file cleanup. Playback completion alone cannot prove sound audibility.

## API references consulted

- [Java SE 17 Clip](https://docs.oracle.com/en/java/javase/17/docs/api/java.desktop/javax/sound/sampled/Clip.html) and [DataLine events](https://docs.oracle.com/en/java/javase/17/docs/api/java.desktop/javax/sound/sampled/DataLine.html).
- [Android MediaPlayer state and resource management](https://developer.android.com/media/platform/mediaplayer/state-resources) and [MediaPlayer reference](https://developer.android.com/reference/android/media/MediaPlayer).
- [Apple AVAudioPlayerDelegate](https://developer.apple.com/documentation/avfaudio/avaudioplayerdelegate), [completion callback](https://developer.apple.com/documentation/avfaudio/avaudioplayerdelegate/audioplayerdidfinishplaying(_:successfully:)) and [stop](https://developer.apple.com/documentation/avfaudio/avaudioplayer/stop()). Apple pages require JavaScript for detailed web rendering; the Kotlin/Native binding check remains for the macOS build.
- [Kotlin Objective-C interop](https://kotlinlang.org/docs/native-objc-interop.html).

## Foreground lifecycle binding

`bindAudioLifecycle(player, onBackground)` installs native observers and returns an idempotent unbind closure. Bind and unbind on the UI loop. Every foreground-loss event calls `player.stop()` first, then `onBackground()` on that UI loop. The UI callback must cancel any pending resource-loading job and reset its playing state, so a loader cannot start new audio after the background event. The event never marks an exercise as heard. Dispose by unbinding and calling player.stop(); stopping and unregistering serve different purposes.

- Desktop: Toolkit.addAWTEventListener with WINDOW_EVENT_MASK watches WINDOW_DEACTIVATED events in this app's JVM and delivers on Swing EDT. It stops conservatively even when a separate window inside the same JVM takes focus, such as a native dialog. The unbind closure removes the exact listener from that Toolkit. This is an in-process AWT observer, not an operating-system global hook.
- Android: the initialized application context registers Application.ActivityLifecycleCallbacks. onActivityPaused stops audio and updates the UI on Android main. Any Activity pause is treated conservatively as foreground loss. No Activity is retained, and dispose unregisters the same callbacks object.
- iOS: NSNotificationCenter observes UIApplicationDidEnterBackgroundNotification on NSOperationQueue.mainQueue. Dispose removes its observer token and drops the closure retaining that token. A bound guard also rejects queued notification delivery after dispose.

Reference methods: [Toolkit AWT listeners](https://docs.oracle.com/en/java/javase/17/docs/api/java.desktop/java/awt/Toolkit.html#addAWTEventListener(java.awt.event.AWTEventListener,long)), [Android ActivityLifecycleCallbacks](https://developer.android.com/reference/android/app/Application.ActivityLifecycleCallbacks), [iOS background notification](https://developer.apple.com/documentation/uikit/uiapplication/didenterbackgroundnotification), and [NotificationCenter block observer](https://developer.apple.com/documentation/foundation/notificationcenter/addobserver(forname:object:queue:using:)). Native foreground/background and disposal behavior still requires platform execution; no build was run for this addition.
