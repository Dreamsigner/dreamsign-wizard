# Source-complete curriculum implementation plan

> **For agentic workers:** Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Represent every retained source course as a complete course unit, with an authored playable lesson for every source session and a verifiable coverage record.

**Architecture:** Keep source inventories separate from authored teaching content. Validate their correspondence before generating Kotlin catalogue data; expose course metadata and source-session provenance in the existing learning path and help. A source inventory alone never counts as playable content.

**Tech Stack:** Kotlin 2.0.0, Compose Multiplatform 1.6.11, kotlinx.serialization 1.6.3, Python standard library for source inventories and validation.

**Spec:** `docs/superpowers/specs/2026-10-19-source-complete-curriculum-design.md`

**Dernière décision utilisateur, 20 octobre : Windows uniquement.** Après
l'estimation d'avancement à environ 40 % du périmètre complet, l'utilisateur
retire Android et iOS des développements et builds actifs. Le code existant
reste conservé. Cette décision remplace l'extension multiplateforme décrite
ci-dessous pour la livraison active. Voir `docs/DELIVERY-SCOPE.md`.

**Évolution d’exécution, 20 octobre :** la demande ultérieure de livraison native
Windows/Android/iOS étend ce plan initialement centré sur Windows. La contrainte
provisoire Kotlin/Compose ci-dessous est remplacée par la migration documentée
dans `docs/platform-migration.md` ; ses preuves sont séparées de celles de l’ancienne
chaîne. La couverture complète des sources reste obligatoire et non atteinte.

## Global constraints

- Categories are Mathématiques, Informatique and TCF.
- Each unit represents one identified source course and edition.
- Preserve all source sessions, source order and distinct review/exam types.
- Include all OSSU options in the source inventory, not only the minimum degree path.
- Do not count equivalent alternatives twice. MIT 18.01SC replaces the three listed Calculus 1A/B/C courses if that alternative is selected.
- No title-only generated lessons, repeated filler QCMs or invented official session counts.
- Keep Kotlin 2.0.0, Compose Multiplatform 1.6.11 and the provided brand palette.
- In-app AI generation remains removed. Write content in the repository.
- Preserve the existing uncommitted theme correction; do not include generated build folders in commits.

## Review focus

1. A reachable landing page may conceal a missing syllabus; counts must stay unknown until the actual sequence is read.
2. Session numbering can include reviews/exams, as in MIT 18.01SC sessions 21/22 and final session 102.
3. An equivalent alternative can duplicate several source courses and falsely inflate course counts.
4. Existing lesson identifiers can refer to different content after reorganisation; avoid transferring credit to a different source session.
5. TCF listening, written production and spoken production cannot be validated by a generic text QCM or by an official score claim.

## Task 1: Source inventory

**Files:** `tools/inventory_sources.py`, `docs/curriculum/ossu-course-inventory.json`, `docs/curriculum/*-source-sessions.json`, `docs/curriculum/TCF-SOURCES.md`.

**Produces:** Per-course ordered source session manifests, editions, exact source URLs, session kinds, access status and evidence date. Null session count means unverified, never zero.

- [x] Inventory OSSU course rows; preserve core, advanced and project-option provenance.
- [x] Read every MIT 18.01SC part and exam index; include the final exam's own heading. Verify numbers 1 through 102 without omissions or duplicate source URLs.
- [x] Select official TCF reference and recommended preparation sources; document the lack of an official TCF lecture programme.
- [ ] Resolve full source sequences for the remaining courses and selected alternatives. A landing page alone does not complete this step.
- [ ] Record exact exercise/assignment structure where published; do not assume every source lesson has two exercises.

## Task 2: Authored content contract and completeness check

**Files:** new `tools/validate_curriculum.py`, new `curriculum/courses/*.json`; extend `composeApp/src/commonMain/kotlin/com/duolingo/app/model/Models.kt` only after the contract is fixed.

**Consumes:** Source manifests from Task 1.
**Produces:** Authored course content with stable `courseId`, edition, category, source-session key, title, objective, notes, exercises, hint, correction and resource URLs.

- [x] Define a source-session key as course ID + edition + source session number or chapter ID.
- [x] Validate exact source coverage and order, unique course/lesson/exercise IDs, nonempty teaching notes, meaningful exercise content and valid answer indices.
- [x] Exercise validation against a missing middle session, a duplicate ID, a false complete count and an invalid answer; all must be rejected with the offending ID.
- [x] Keep partial drafts out of the release catalogue; report authored count separately from expected count.

## Task 3: Full course authoring

**Files:** `curriculum/courses/*.json`, corresponding source manifests and a coverage report.

**Consumes:** Verified complete programmes and teaching material, not merely course titles.
**Produces:** Fully authored per-session lessons, reviewed against each course's source material.

- [x] Author all 102 MIT 18.01SC sessions, with the 93 teaching sessions, 4 reviews and 5 exams identified explicitly. Use varied, topic-specific exercises and worked corrections.
- [ ] Author the remaining verified mathematics courses in source order.
- [ ] Author the verified computer-science courses, including programming and project exercises where appropriate.
- [ ] Author TCF preparation based on the selected materials. Keep listening audio and production tasks distinct from reading/grammar QCMs.
- [ ] Check the mathematical answers, code results, prerequisite reachability and source provenance. An authored file without this review is not release-ready.

## Task 4: Catalogue and learning experience

**Files:** `SampleData.kt`, `Models.kt`, `AppViewModel.kt`, `AppUI.kt`, `LessonQuizScreen.kt`, new course-specific data files as needed.

**Consumes:** Validated authored courses only.
**Produces:** Category -> full source course/unit -> numbered source lesson -> supported interactive exercises.

- [ ] Add course identity/edition and source-session information without replacing published lesson IDs with unrelated content.
- [ ] Make each course independently startable, then follow its source order and explicit prerequisites.
- [ ] Display the real authored/source session coverage and session types.
- [ ] Show the lesson notes and exact reference links in help. Make long lessons/help scrollable and usable at normal Windows window sizes.
- [ ] Implement and verify the exercise interactions required by the authored content; do not silently treat unsupported exercises as correct.

## Task 5: Verification and Windows delivery

**Files:** catalogue coverage report, `README.md`, `.github/workflows/build.yml` if necessary; package outside tracked source.

- [ ] Run the completeness validator against every released course and verify the unsupported/missing-content cases from Tasks 2 and 4.
- [ ] Compile with `gradle :composeApp:compileKotlinDesktop` and package with `gradle :composeApp:createDistributable`.
- [ ] Inspect source course selection, first/middle/last lessons, help, correction, course progression and each non-QCM exercise type in the delivered Windows app.
- [ ] Verify ZIP integrity, launcher and bundled runtime. Report actual source coverage and persistence limitations.
- [ ] Commit and push the validated changes within the existing Git authorization. Do not report the entire curriculum finished while source or authorship coverage remains incomplete.
