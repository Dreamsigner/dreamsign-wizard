<!-- Generated: 2026-10-23 | Files scanned: 265 tracked files inventoried; focused source/config scan | Token estimate: ~400 (characters/4 heuristic) -->

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
