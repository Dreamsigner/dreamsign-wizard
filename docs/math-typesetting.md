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
The default is strict: text without delimiters stays one ordinary Compose `Text`
node. On known math fields only, pass `recognizeLegacyMath = true` to also
recognize numeric matrices, explicit ASCII powers (`x^2`), Unicode powers and
indices (`x²`, `u₁`), and standalone numeric fractions (`3/4`). It deliberately
leaves composite divisions such as `x/2+3` in their original form.
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
