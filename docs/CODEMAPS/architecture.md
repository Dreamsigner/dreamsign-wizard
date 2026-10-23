<!-- Generated: 2026-10-23 | Files scanned: 265 tracked files inventoried; focused source/config scan | Token estimate: ~450 (characters/4 heuristic) -->

# Architecture

```text
desktopMain/main.kt ── platform providers ──> commonMain/App.kt
                                              └─> DuolingoApp / AppViewModel
                                                   ├─ embedded generated courses
                                                   ├─ local progress store
                                                   └─ course readers and exercises
```

The repository is one Kotlin Multiplatform app (`:composeApp`), not a service monorepo. Shared Compose UI, learning models, curriculum access and exercise state live in `composeApp/src/commonMain`. Platform source sets supply entry points, persistence, media and document implementations. The maintained delivery path is Windows desktop: Gradle packages a Windows app image and per-user EXE; Android and iOS targets remain declared in Gradle and have source implementations, but README identifies no maintained distribution for them.

Course and lesson content is bundled with the app. Learner progress is stored on the device. There is no application API or remote database in the inspected runtime. Network use is for external course resources and the Windows GitHub-release updater. `.github/workflows/build.yml` validates curriculum, runs desktop checks, and builds Windows artifacts; a tagged release can publish the installer.

Primary boundaries: `composeApp/src/commonMain/kotlin/com/duolingo/app/{ui,viewmodel,model,data,storage}`; platform code under `composeApp/src/{desktopMain,androidMain,iosMain}`; canonical curriculum under `curriculum/courses/`; generators under `tools/`.
