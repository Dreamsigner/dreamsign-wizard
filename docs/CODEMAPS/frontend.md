<!-- Generated: 2026-10-23 | Files scanned: 265 tracked files inventoried; focused source/config scan | Token estimate: ~400 (characters/4 heuristic) -->

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

The scan found no app-level share action, deep-link route, social publishing flow or analytics SDK/event pipeline. Existing course reference URLs and YouTube links open learning sources; they are not shareable lesson links. For francophone math/CS learners, Windows-only distribution is a material install-and-share barrier, and the code currently gives no referral or measurement loop to observe acquisition and return use.
