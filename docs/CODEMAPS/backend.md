<!-- Generated: 2026-10-23 | Updated: 2026-10-27 | Files scanned: 265 at baseline; focused studio update | Token estimate: ~450 (characters/4 heuristic) -->

# Backend and external calls

There is no server-side backend, application API, authentication service, or remote learner database in the inspected app. The equivalent app logic is local:

Studio IA (2026-10-26): desktop `ui/studio/DesktopStudioController.kt` also implements the common studio port. It owns session state/persistence and calls an injected `StudioTutor`; the production tutor uses Java HTTP Chat Completions against the user's provider. HTTPS is required remotely; loopback HTTP can omit a key. Requests are bounded/cancellable, redirects refused, and failed pending turns are retryable. This is an outbound adapter, not an app-hosted API. API keys stay in RAM.

```text
DreamsignWizardApp → AppViewModel → bundled catalogue + platform ProgressStore
```

`AppViewModel` loads and validates serialized `UserStats`, updates lesson and exercise state, and persists progress. `ProgressStore` is an `expect`/`actual` platform interface; Windows writes a local JSON file, Android uses private preferences, and iOS uses `NSUserDefaults`.

Network-facing desktop features are isolated behind platform services: `WindowsUpdateController` checks GitHub stable releases and verifies downloaded installer hashes; video/PDF readers access linked external resources. The optional French-track video downloader invokes local `yt-dlp`, FFmpeg/ffprobe and a supported JavaScript runtime. None of these is a course progress API.

Key files: `composeApp/src/commonMain/kotlin/com/dreamsigner/wizard/viewmodel/AppViewModel.kt`, `storage/ProgressStore.kt`; platform stores in `desktopMain`, `androidMain`, `iosMain`; updater in `desktopMain/kotlin/com/dreamsigner/wizard/update/`; download support in `desktopMain/kotlin/com/dreamsigner/wizard/ui/download/`.
