# Adaptive learning workspace

## Intent and content

The first question is: which course and lesson should I work on next?
The subjects select the curriculum; each LearningUnit is displayed as a course with its own lesson list. Course codes, edition and expected lesson counts are shown only when supplied by the source metadata. The available count always comes from actual lessons; an expected source count is labelled separately. Completion, XP, lives and streak use live application state. No invented achievements, percentages or trend charts.

The visual idea is a study atelier: paper ground, white working surfaces, architectural ink and a deliberate red action. The signature is the course directory beside the lesson workspace, with real progress embedded in each course. Large, rounded lesson controls, clear completion marks, short encouragement and rewarding end states carry the playful learning character. This is a Kotlin Compose Multiplatform UI; no web framework or new dependency is introduced.

## Composition

- Window width >=1000 dp: labelled 224 dp navigation sidebar.
- Width 700–999 dp: 104 dp rail with visible labels.
- Width <700 dp: compact bottom navigation and a two-line header when needed.
- Available lesson workspace >=880 dp: 260 dp course directory + 24 dp gutter + flexible lesson column. Below that, a course picker replaces the directory.
- Gutters: 16 dp compact, 24 dp larger workspace. Long quiz text is capped at 920 dp; quiz actions at 760 dp. Course and profile content scroll independently of app chrome.
- Quiz correction is in the scrollable body. Only the primary action is pinned, so long explanations cannot cover the question or the action.
- The root consumes safe drawing insets once. Platform inset behavior must be checked on actual iOS/Android hosts.

## Brand roles

Brand red #E3262E marks progress and reward. Action red #C91E28 supports white button text with stronger contrast. Ink #141414 is primary text/focus; graphite #4A4A4A is explanatory text; paper #F7F5F0 is the background; concrete #D9D7D2 separates neutral states; white is the work surface. No gradients or floating dashboard ornament. The symbol is a standard learning/reward star, not a claimed reproduction of a supplied logo.

## Learning behavior

Opening a lesson first presents its objective, learning notes and source links. The learner explicitly proceeds to exercises. The same notes remain available from the labelled help control with a scrollable dialog. Empty notes and unavailable exercise types have honest text. MatchingPairs offers no fake check action. Translation tokens wrap instead of overflowing and expose selection. Rewards show the actual lastAwardedXp state, including zero on a replay.

Material buttons provide press, keyboard activation and semantics. Custom action, course, lesson, answer and token controls also show a 3 dp ink outline when focused. Controls use at least 48 dp targets. Answers expose radio semantics and selection. Completion and locking are described with text and icons in addition to color. Corrective feedback uses a polite live region. Text has no fixed line limits and content scrolls at larger font sizes. Native screen reader behavior still requires device QA.

## Guidance applied

UI/UX Pro Max education search recommended playful block-based/flat learning UI, hierarchy and progress indicators. Its initial broad design-system result described an AI marketing page and was rejected; the narrower explicit product query matched education. No palette or font from that unrelated result was applied.

Design Taste app-screen draw: editorial-greeting, seed 665058642. Kept the tonal surface structure and product-specific material ground; adapted the composition for a learning directory and lesson tasks. User instructions override its red restriction, neutral action and greeting-heavy dashboard pattern: this app needs branded red actions and playful learning feedback. The supplied native KMP stack also overrides browser inspector recipes. No downloaded fonts or external imagery were introduced.

Compose Multiplatform guidance: shared native composables, callbacks rather than navigation objects in child controls, collected StateFlows and keyed lesson/course lazy lists. Accessibility guidance: semantic controls, text labels, visible keyboard focus, no color-only state and unclipped scrollable content.

## QA handoff

No build or visual validation was run by the UI subtask, as requested by the integration owner. Validate the actual native app at 1440×900, 1024×768, 768×1024 and 390×844; also short landscape windows. Check sidebar/rail/bottom transitions, long course names, every subject, independent course prerequisites, scrolling through last lesson, enlarged fonts, Tab/Shift+Tab/Enter/Space focus, notes before exercises, scrollable help and correction, token wrapping, completion with awarded versus replay XP, restart persistence status, and Android/iOS safe areas. Do not treat this design note as evidence that these checks passed.
