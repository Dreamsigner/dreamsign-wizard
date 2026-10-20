# Practice media boundary

The dependency catalogue pins Kotlin 2.2.21, Compose Multiplatform 1.9.3 and kotlinx.coroutines 1.8.1. `PracticeMedia` lives in commonMain and supplies player/recorder creation, resource loading, recording storage and foreground lifecycle binding. `NativePracticeMedia` delegates to the existing native APIs and bundled `Res.readBytes`; the default object does not create a device, ask permission or load a recording during initialization.

`LocalPracticeMedia` is a `staticCompositionLocalOf<PracticeMedia>` defaulting to `NativePracticeMedia`. It represents the media environment for a subtree, rather than a view model or a screen's business state. No setting or test mode is exposed in the application. A stable provider is normally supplied once; replacing it intentionally disposes the previous practice subtree.

## Test injection

Tests provide a concrete fake before composing either practice screen or the root application:

```kotlin
CompositionLocalProvider(LocalPracticeMedia provides fakeMedia) {
    DuolingoApp(viewModel = modelWithIsolatedProgressStore)
}
```

The fake implements every `PracticeMedia` method and never delegates to NativePracticeMedia. Supply fake PlatformAudioPlayer and PlatformAudioRecorder instances, synthetic resource bytes, an isolated recording map and lifecycle registration functions that merely retain the background callbacks. Also inject a view model with an isolated ProgressStore: this boundary replaces media storage, not the separate learner-progression store. All recording/playback callbacks run on the UI loop, as required by the real APIs. A fake recording used in oral UI tests must be a valid mono PCM16/16 kHz WAV because UI duration validation remains real; stubbing a success flag does not bypass it.

`loadResource(path)` is suspend and must respect cancellation, so tests can pause resource loading with a controlled deferred result and cancel it through stop, navigation or the captured background callback. `saveRecording(id, bytes)` and `loadRecording(id)` are blocking functions called on the existing worker dispatcher. A fake recording store must therefore be safe across UI/worker access; use a synchronized map or confine its mutations appropriately. It can throw a controlled storage error to exercise retry behavior. Do not point fake storage at user files.

Lifecycle bind methods return idempotent unbind closures. Their fake background handlers must emulate the native contract: stop the player, or cancel the recorder, then invoke onBackground on the UI loop. Disposal removes those registrations. Ending playback invokes completion once; explicit stop/cancel or a failed/stale operation does not. Tests should drive these events explicitly rather than infer completion from elapsed time.

Both listening and spoken UI read LocalPracticeMedia and key their entire practice subtree by `(media, exercise.id)`. Player/recorder instances are remembered with those same keys. Switching provider/exercise cancels the previous coroutine scope, unbinds its lifecycle observers and stops/cancels its fake or native media. All resource, storage and lifecycle operations in these screens route through the provider; pure WAV validation and view-model progression remain unchanged.

## Sources and validation boundary

- [Compose CompositionLocal](https://developer.android.com/develop/ui/compose/compositionlocal): a tree-scoped dependency with a default value; staticCompositionLocalOf suits a provider that normally stays stable.
- [Compose effect keys](https://developer.android.com/develop/ui/compose/side-effects#restarting-effects): changed dependencies restart effects and clean up previous ownership.
- [Compose Multiplatform raw resources](https://kotlinlang.org/docs/multiplatform/compose-multiplatform-resources-usage.html#raw-files): NativePracticeMedia uses the existing suspending resource API.

Compose Multiplatform patterns, source-driven development and Kotlin testing skills were read for this change. Test isolation follows the repository's existing framework; no Kotest, MockK or coverage dependency was added. This task does not own the root test files or build setup. No build, generation, device access or tests were run here; the root task supplies the fake providers and executes the UI tests separately. Successful fake-media tests establish UI behavior only, not physical sound or microphone quality.
