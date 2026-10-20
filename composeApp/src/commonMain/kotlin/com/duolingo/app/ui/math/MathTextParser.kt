package com.duolingo.app.ui.math

/** A plain text or LaTeX segment parsed from the lightweight `$...$` / `$$...$$` syntax. */
sealed interface LearningTextSegment {
    data class Text(val value: String) : LearningTextSegment
    data class Math(val latex: String, val display: Boolean) : LearningTextSegment
}

/** Splits learning copy into text and math while leaving unmatched delimiters untouched. */
fun parseLearningText(source: String, recognizeLegacyMath: Boolean = false): List<LearningTextSegment> {
    val result = mutableListOf<LearningTextSegment>()
    var cursor = 0
    var textStart = 0
    while (cursor < source.length) {
        if (source[cursor] != '$' || (cursor > 0 && source[cursor - 1] == '\\')) {
            cursor++
            continue
        }
        val display = source.startsWith("\$\$", cursor)
        val delimiter = if (display) "\$\$" else "\$"
        val contentStart = cursor + delimiter.length
        val end = source.indexOf(delimiter, contentStart)
        if (end < 0) {
            cursor += delimiter.length
            continue
        }
        if (cursor > textStart) result += LearningTextSegment.Text(source.substring(textStart, cursor))
        val latex = source.substring(contentStart, end)
        if (latex.isNotBlank()) result += LearningTextSegment.Math(latex, display)
        else result += LearningTextSegment.Text(delimiter)
        cursor = end + delimiter.length
        textStart = cursor
    }
    if (textStart < source.length) result += LearningTextSegment.Text(source.substring(textStart))
    if (!recognizeLegacyMath) return result
    return result.flatMap { segment ->
        if (segment is LearningTextSegment.Text) parseLegacyMath(segment.value) else listOf(segment)
    }
}

private data class MathCandidate(val start: Int, val end: Int, val latex: String)

private fun parseLegacyMath(source: String): List<LearningTextSegment> {
    val matrixCell = "[-−+]?(?:\\d+(?:\\.\\d+)?|\\d+/\\d+)"
    val matrixRow = "\\[\\s*$matrixCell(?:\\s*,\\s*$matrixCell)+\\s*\\]"
    val candidates = buildList {
        Regex("\\[$matrixRow(?:\\s*,\\s*$matrixRow)+\\]").findAll(source).forEach { match ->
            normalizeMathFormula(match.value)?.let { add(MathCandidate(match.range.first, match.range.last + 1, it)) }
        }
        val powerPatterns = listOf(
            Regex("(?<![A-Za-z0-9])(?:[A-Za-z0-9](?:_\\{?[A-Za-z0-9]+\\}?)?)\\^(?:\\{[A-Za-z0-9]+\\}|[A-Za-z0-9]+)(?![A-Za-z0-9])"),
            Regex("(?<![A-Za-z0-9])[A-Za-z]_[0-9]+(?![A-Za-z0-9])"),
            Regex("(?<![A-Za-z0-9])[A-Za-z0-9][₀₁₂₃₄₅₆₇₈₉ₓₙₖₘₐₑᵢ]*[⁰¹²³⁴⁵⁶⁷⁸⁹ᵗⁿⁱʲ]+(?![A-Za-z0-9])"),
            Regex("(?<![A-Za-z0-9])[A-Za-z0-9][₀₁₂₃₄₅₆₇₈₉ₓₙₖₘₐₑᵢ]+(?![A-Za-z0-9])"),
        )
        powerPatterns.forEach { pattern -> pattern.findAll(source).forEach { match ->
                normalizeMathFormula(match.value)?.let { add(MathCandidate(match.range.first, match.range.last + 1, it)) }
            } }
        Regex("(?<![A-Za-z0-9])\\d+/\\d+(?![A-Za-z0-9])").findAll(source).forEach { match ->
            normalizeMathFormula(match.value)?.let { add(MathCandidate(match.range.first, match.range.last + 1, it)) }
        }
    }.sortedWith(compareBy<MathCandidate> { it.start }.thenByDescending { it.end })

    if (candidates.isEmpty()) return listOf(LearningTextSegment.Text(source))
    val segments = mutableListOf<LearningTextSegment>()
    var cursor = 0
    candidates.forEach { candidate ->
        if (candidate.start < cursor) return@forEach
        if (candidate.start > cursor) segments += LearningTextSegment.Text(source.substring(cursor, candidate.start))
        segments += LearningTextSegment.Math(candidate.latex, display = false)
        cursor = candidate.end
    }
    if (cursor < source.length) segments += LearningTextSegment.Text(source.substring(cursor))
    return segments
}

/** Plain-text alternative for accessibility and platforms without the desktop TeX renderer. */
fun mathAccessibilityDescription(latex: String): String = latex
    .replace("\\begin{bmatrix}", "matrice ")
    .replace("\\end{bmatrix}", "")
    .replace("\\\\", "; ")
    .replace("&", ", ")
    .replace(Regex("\\\\frac\\{([^{}]+)\\}\\{([^{}]+)\\}")) { "${it.groupValues[1]} sur ${it.groupValues[2]}" }
    .replace("\\sqrt", " racine carrée de ")
    .replace("\\sum", " somme ")
    .replace("\\int", " intégrale ")
    .replace("\\lim", " limite ")
    .replace("\\times", " fois ")
    .replace("\\cdot", " fois ")
    .replace("\\to", " tend vers ")
    .replace("\\prime", " prime ")
    .replace(Regex("\\\\(?:left|right)\\b"), "")
    .replace(Regex("\\\\(?:,|;|!|quad|qquad)"), " ")
    .replace(Regex("\\\\([A-Za-z]+)"), "$1")
    .replace(Regex("[{}]"), "")
    .replace(Regex("\\s+"), " ")
    .replace(Regex("\\s+([,;])"), "$1")
    .trim()

/**
 * Converts only the small, explicit notation subset used by existing exercise formulas.
 * Returns null for prose or notation whose structure cannot be preserved confidently.
 */
fun normalizeMathFormula(source: String): String? {
    val value = source.trim()
    if (value.isEmpty()) return null

    replaceNumericMatrix(value)?.let { return it }
    if ("[[" in value || "]]" in value) return null
    if ('\\' in value) return value // Explicit LaTeX supplied by curriculum authors.
    Regex("^([A-Za-z0-9]+)\\s*/\\s*([A-Za-z0-9]+)$").matchEntire(value)?.let {
        return "\\frac{${it.groupValues[1]}}{${it.groupValues[2]}}"
    }

    val limit = Regex("\\blim\\s*_?\\{?([A-Za-zₓ])\\s*(?:→|->)\\s*([A-Za-z0-9₀-₉]+)\\}?")
        .replace(value) { "\\lim_{${it.groupValues[1].replace('ₓ', 'x')}\\to ${it.groupValues[2].map(::subscriptDigit).joinToString("")}}" }

    val hasMathSignal = limit.any { it.isDigit() || it in "=<>±×·√∫Σ∑→−+-*/^_()[]|∩∪π′₀₁₂₃₄₅₆₇₈₉ₓₙₖₘₐₑᵢ⁰¹²³⁴⁵⁶⁷⁸⁹ᵗⁿⁱʲ" }
    if (!hasMathSignal) return null
    if (Regex("[A-Za-zÀ-ÿ]{3,}\\s+[A-Za-zÀ-ÿ]{3,}").containsMatchIn(limit)) return null

    val normalizedSource = Regex("_([A-Za-z0-9]+)(?![A-Za-z0-9])")
        .replace(limit) { "_{${it.groupValues[1]}}" }
        .let { sourceWithGroupedIndices ->
            Regex("\\^([A-Za-z0-9]+)(?![A-Za-z0-9])")
                .replace(sourceWithGroupedIndices) { "^{${it.groupValues[1]}}" }
        }
    val normalized = buildString {
        var index = 0
        while (index < normalizedSource.length) {
            val ch = normalizedSource[index]
            when {
                ch in "₀₁₂₃₄₅₆₇₈₉ₓₙₖₘₐₑᵢ" -> {
                    append("_{")
                    while (index < normalizedSource.length && normalizedSource[index] in "₀₁₂₃₄₅₆₇₈₉ₓₙₖₘₐₑᵢ") {
                        append(subscriptDigit(normalizedSource[index]))
                        index++
                    }
                    append('}')
                    continue
                }
                ch in "⁰¹²³⁴⁵⁶⁷⁸⁹ᵗⁿⁱʲ" -> {
                    append("^{")
                    while (index < normalizedSource.length && normalizedSource[index] in "⁰¹²³⁴⁵⁶⁷⁸⁹ᵗⁿⁱʲ") {
                        append(superscriptDigit(normalizedSource[index]))
                        index++
                    }
                    append('}')
                    continue
                }
                ch == '√' -> append("\\sqrt ")
                ch == '∫' -> append("\\int")
                ch == 'Σ' || ch == '∑' -> append("\\sum")
                ch == '∩' -> append("\\cap ")
                ch == '∪' -> append("\\cup ")
                ch == 'π' -> append("\\pi ")
                ch == '′' -> append("^{\\prime}")
                ch == '×' -> append("\\times ")
                ch == '·' -> append("\\cdot ")
                ch == '→' -> append("\\to ")
                ch == '−' -> append('-')
                else -> append(ch)
            }
            index++
        }
    }

    // Group simple square-root operands; leave more complex roots to authored LaTeX.
    val withRoots = Regex("\\\\sqrt\\s*\\(([^()]*)\\)")
        .replace(normalized) { "\\sqrt{${it.groupValues[1]}}" }
    if (Regex("[^A-Za-z0-9\\s\\\\{}_^=<>+*/().,:|\\[\\]-]").containsMatchIn(withRoots)) return null
    return withRoots
}

private fun subscriptDigit(char: Char): Char = when (char) {
    '₀' -> '0'; '₁' -> '1'; '₂' -> '2'; '₃' -> '3'; '₄' -> '4'; '₅' -> '5'; '₆' -> '6'; '₇' -> '7'; '₈' -> '8'; '₉' -> '9'
    'ₓ' -> 'x'; 'ₙ' -> 'n'; 'ₖ' -> 'k'; 'ₘ' -> 'm'; 'ₐ' -> 'a'; 'ₑ' -> 'e'; 'ᵢ' -> 'i'
    else -> char
}

private fun superscriptDigit(char: Char): Char = when (char) {
    '⁰' -> '0'; '¹' -> '1'; '²' -> '2'; '³' -> '3'; '⁴' -> '4'; '⁵' -> '5'; '⁶' -> '6'; '⁷' -> '7'; '⁸' -> '8'; '⁹' -> '9'
    'ᵗ' -> 't'; 'ⁿ' -> 'n'; 'ⁱ' -> 'i'; 'ʲ' -> 'j'
    else -> char
}

private fun parseNumericMatrix(source: String): String? {
    if (!source.startsWith("[[") || !source.endsWith("]]")) return null
    val rows = source.substring(1, source.length - 1)
        .split(Regex("\\]\\s*,\\s*\\["))
    if (rows.size !in 2..4) return null
    val entries = rows.map { row -> row.removePrefix("[").removeSuffix("]").split(',').map(String::trim) }
    if (entries.any { it.size != entries.first().size } || entries.first().size !in 2..4) return null
    val numeric = Regex("[-−+]?(?:\\d+(?:\\.\\d+)?|\\d+/\\d+)")
    if (entries.flatten().any { !numeric.matches(it) }) return null
    val latexRows = entries.joinToString(" \\\\ ") { row ->
        row.joinToString(" & ") { entry ->
            val value = entry.replace('−', '-')
            Regex("([+-]?\\d+)/(\\d+)").matchEntire(value)?.let { "\\frac{${it.groupValues[1]}}{${it.groupValues[2]}}" } ?: value
        }
    }
    return "\\begin{bmatrix}$latexRows\\end{bmatrix}"
}

private fun replaceNumericMatrix(source: String): String? {
    val start = source.indexOf("[[")
    if (start < 0) return parseNumericMatrix(source)
    val endStart = source.indexOf("]]", start + 2)
    if (endStart < 0 || source.indexOf("[[", start + 2) >= 0) return null
    val matrix = parseNumericMatrix(source.substring(start, endStart + 2)) ?: return null
    return source.substring(0, start) + matrix + source.substring(endStart + 2)
}
