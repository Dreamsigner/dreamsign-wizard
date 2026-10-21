# Math typesetting

The shared UI exposes `LearningFormula` and `LearningText` from
`com.duolingo.app.ui.math`. A desktop root can enable rendered LaTeX by wrapping
its content in the provider:

```kotlin
MathRenderingProvider(DesktopMathRenderer) {
    LearningText("Calculer \$\\frac{x^2}{2}\$ puis \$\$\\sum_{i=1}^{n} i\$\$.")
}
```

`LearningText` recognizes `$...$` inline and `$$...$$` display delimiters.
Inline equations use native Compose Text placeholders, so surrounding prose
wraps as one paragraph instead of a FlowRow of independently aligned fragments.
Numeric matrices use separate horizontally scrollable blocks. Desktop images
account for display density and font scale. Both mathematics and computer-science
notes, prompts, objectives, options, hints and corrections enable math rendering;
fenced code and inline code remain literal. See [lesson presentation](lesson-presentation.md).
The default is strict: text without delimiters stays one ordinary Compose `Text`
node. On known math fields only, pass `recognizeLegacyMath = true` to also
recognize numeric matrices, explicit ASCII powers (`x^2`), Unicode powers and
indices (`x²`, `u₁`), and quotient expressions (`3/4`, `x/2+3`, `[f(a+h)-f(a)]/h`).
A balanced expression parser preserves operator precedence, grouping, factorials,
function arguments and powers, including nested fractions. It produces LaTeX
`\frac{numerator}{denominator}` for display in the local renderer. Complex
notation such as integrals, norms and adjoints is explicitly authored in LaTeX
in the canonical course JSON instead of inferred from ambiguous plain text.

Source code, URLs, filesystem paths, dates and Prolog predicate arities remain
literal. Code examples use code spans or fenced blocks, so `/` retains its
programming meaning. Textual ratios and physical units have explicit LaTeX
annotations. The catalogue-wide `FractionCorpusCoverageTest` visits every
published teaching field and renders each detected fraction through the actual
desktop engine. It fails on unclassified slashes outside code and on invalid
fraction rendering; reports are saved under `build/reports/fraction-coverage`.
This gate covers the bundled course text, not the contents of external PDFs
or videos. Visual Compose tests also render the first two calculus lessons at
wide and narrow reading widths.
`LearningFormula` accepts one LaTeX expression and an optional `description`
for screen readers. Without a provider, common code displays a readable text
fallback, so Android and iOS remain usable without platform-specific math code.
The existing exercise formula normalizer handles numeric matrices, simple
fractions, square roots, common Unicode powers and indices, and basic limits.
Prose and malformed matrices stay as entered text. Unsupported LaTeX that the
desktop renderer cannot parse also falls back to its original visible text.

Desktop renders JLaTeXMath into an offscreen image and then a Compose image. The
formula therefore participates in Compose screenshots. Rendering is local and
does not use a WebView or runtime network access. The dependency excludes the
optional Greek and Cyrillic language-font modules; core mathematical symbols
such as `\alpha` and `\sum` still render. JLaTeXMath's core license is GPL-2.0-or-later
with a Classpath Exception for linking independent modules. Include
`docs/licences/THIRD-PARTY-NOTICES.txt` in the Windows package; it contains the
resolved core artifact hash and verbatim core/font license texts. It does not
choose a license for the application; the distributor must assess the actual
package and its obligations before release.

Sources: [JLaTeXMath project](https://github.com/opencollab/jlatexmath),
[project license](https://github.com/opencollab/jlatexmath/blob/master/LICENSE),
[Maven Central artifact](https://central.sonatype.com/artifact/org.scilab.forge/jlatexmath).
