<!-- Generated: 2026-10-23 | Updated: 2026-10-27 | Files scanned: 265 at baseline; focused studio update | Token estimate: ~500 (characters/4 heuristic) -->

# Data and persistence

```text
curriculum/courses/*.json ── tools/validate_curriculum.py --generate
                                  ↓
composeApp/src/commonMain/.../data/generated/GeneratedCourses.kt
                                  ↓
SampleData / catalogue → AppViewModel ↔ UserStats JSON → platform ProgressStore
```

The canonical authored curriculum is JSON in `curriculum/courses/`; generated Kotlin under `composeApp/src/commonMain/kotlin/com/duolingo/app/data/generated/` is consumed by `SampleData` and should be regenerated with `tools/validate_curriculum.py --generate`, not edited as the source. `CourseResources.kt` is similarly generated from `curriculum/course-resources.json`. IDs are designed to remain stable as content evolves.

`UserStats` contains learner state such as XP, streak, active course, completed lessons, favorites and saved writing drafts. Shared code serializes it with kotlinx.serialization. Windows stores it at `%LOCALAPPDATA%/AtelierApprentissage/progress.json` using atomic replacement; Android uses private SharedPreferences and iOS uses standard user defaults. Progress is device-local and does not sync across devices. Desktop video resume positions are separately persisted by `VideoResumeStore`.

There is no server database, migration history or cross-device account model in this app. Curriculum validation and generated-file freshness checks run in CI.

Atelier IA (2026-10-26) stores `studio.json` in the existing Windows data directory, separately from `progress.json`. It contains goal/session identifiers, user/assistant messages, selected session, provider base URL and model. Atomic replacement preserves the prior file on failed writes. A final unanswered user message is recoverable for retry after reload. The provider key is never serialized. There is no cross-session knowledge graph yet.
