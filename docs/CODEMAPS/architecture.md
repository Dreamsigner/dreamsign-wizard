<!-- Generated: 2026-10-23 | Updated: 2026-10-27 | Files scanned: 265 at baseline; focused studio update | Token estimate: ~550 (characters/4 heuristic) -->

# Architecture

```text
desktopMain/main.kt ── platform providers ──> commonMain/App.kt
                                              └─> DreamsignWizardApp / AppViewModel
                                                   ├─ embedded generated courses
                                                   ├─ local progress store
                                                   └─ course readers and exercises
                                              └─ Studio IA / injected StudioController
                                                   ├─ separate local sessions
                                                   └─ configured HTTP tutor provider
```

The repository is one Kotlin Multiplatform app (`:composeApp`), not a service monorepo. Shared Compose UI, learning models, curriculum access and exercise state live in `composeApp/src/commonMain`. Platform source sets supply entry points, persistence, media and document implementations. The maintained delivery path is Windows desktop: Gradle packages a Windows app image and per-user EXE; Android and iOS targets remain declared in Gradle and have source implementations, but README identifies no maintained distribution for them.

Course and lesson content is bundled with the app. Learner progress is stored on the device. There is no app-hosted API or remote learner database. Network use includes external course resources, the Windows GitHub-release updater, and explicit tutor requests to the user's configured provider. Studio IA (2026-10-26) keeps sessions separate from course progress and sends bounded context rather than the whole catalogue. `.github/workflows/build.yml` validates curriculum, runs desktop checks, and builds Windows artifacts; a tagged release can publish the installer.

Primary boundaries: `composeApp/src/commonMain/kotlin/com/dreamsigner/wizard/{ui,viewmodel,model,data,storage}`; platform code under `composeApp/src/{desktopMain,androidMain,iosMain}`; canonical curriculum under `curriculum/courses/`; generators under `tools/`.
