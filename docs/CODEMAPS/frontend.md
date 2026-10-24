<!-- Generated: 2026-10-23 | Updated: 2026-10-24 (sharing flow) | Files scanned: 265 tracked files inventoried; focused source/config scan | Token estimate: ~400 (characters/4 heuristic) -->

# Frontend and state

```text
Desktop main.kt → App.kt / theme → DuolingoApp
                                  ├─ Cours → catalogue/search/filters → CourseReaderScreen
                                  ├─ lesson → quiz/practice → AppViewModel
                                  ├─ Profil
                                  └─ Paramètres → injected Windows updater panel
```

`composeApp/src/commonMain/kotlin/com/duolingo/app/ui/AppUI.kt` owns the main navigation and screen composition. `CourseReaderScreen.kt` renders lesson notes, references, PDFs/videos and math rendering through platform providers. Quiz, listening, writing and speaking screens send user actions to `AppViewModel`; its `StateFlow`s expose answer, review, completion and progress state to Compose. Most short-lived view state is remembered by its screen; durable learner state is serialized in `UserStats` through `ProgressStore`.

`desktopMain/kotlin/main.kt` is the actual desktop entry point and supplies PDF, video, math and update implementations. Android and iOS have native entry points, but Windows is the documented maintained distribution.

`LessonQuizScreen` can open `ui/share/ChallengeShareDialog` for supported short questions. A restricted DTO carries only the lesson title, prompt, options and optional formula/code. The dialog copies text or captures the full Compose card; the injected desktop `ChallengeShareService` saves a PNG through a native file chooser. No answer, hint, explanation or learner state is exported. No deep-link route, web challenge, referral flow or analytics pipeline is implemented.
