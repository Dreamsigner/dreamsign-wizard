<!-- Generated: 2026-10-23 | Files scanned: 265 tracked files inventoried; focused source/config scan | Token estimate: ~350 (characters/4 heuristic) -->

# Backend and external calls

There is no server-side backend, application API, authentication service, or remote learner database in the inspected app. The equivalent app logic is local:

```text
DuolingoApp → AppViewModel → bundled catalogue + platform ProgressStore
```

`AppViewModel` loads and validates serialized `UserStats`, updates lesson and exercise state, and persists progress. `ProgressStore` is an `expect`/`actual` platform interface; Windows writes a local JSON file, Android uses private preferences, and iOS uses `NSUserDefaults`.

Network-facing desktop features are isolated behind platform services: `WindowsUpdateController` checks GitHub stable releases and verifies downloaded installer hashes; video/PDF readers access linked external resources. The optional French-track video downloader invokes local `yt-dlp`, FFmpeg/ffprobe and a supported JavaScript runtime. None of these is a course progress API.

Key files: `composeApp/src/commonMain/kotlin/com/duolingo/app/viewmodel/AppViewModel.kt`, `storage/ProgressStore.kt`; platform stores in `desktopMain`, `androidMain`, `iosMain`; updater in `desktopMain/kotlin/com/duolingo/app/update/`; download support in `desktopMain/kotlin/com/duolingo/app/ui/download/`.
