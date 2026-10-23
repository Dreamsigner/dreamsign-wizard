package com.duolingo.app.ui.math

/** A source range whose contents are a complete, safely renderable quotient expression. */
data class FractionExpressionSpan(val start: Int, val end: Int, val latex: String)

/** Finds mathematical quotient expressions in ordinary copy without consuming adjacent prose. */
fun findFractionExpressions(source: String): List<FractionExpressionSpan> {
    if (source.isEmpty()) return emptyList()
    // Hide recognized physical units while scanning so their slash and superscript remain literal.
    val scanSource = source.toCharArray().also { chars ->
        findPhysicalUnitExpressions(source).forEach { span ->
            for (index in span.start until span.end) chars[index] = ' '
        }
    }.concatToString()
    val spans = mutableListOf<FractionExpressionSpan>()
    var cursor = 0
    while (cursor < scanSource.length) {
        if (scanSource[cursor] == '`') {
            val close = scanSource.indexOf('`', cursor + 1)
            cursor = if (close < 0) scanSource.length else close + 1
            continue
        }
        if (scanSource[cursor] == '|' && scanSource.indexOf('|', cursor + 1) < 0) {
            // Do not reinterpret the contents of an unclosed absolute-value group as standalone math.
            break
        }
        if (scanSource.startsWith("https://", cursor) || scanSource.startsWith("http://", cursor)) {
            cursor = scanSource.indexOfAny(charArrayOf(' ', '\n', '\t'), cursor).let { if (it < 0) scanSource.length else it }
            continue
        }
        if (!isStartBoundary(scanSource, cursor)) { cursor++; continue }
        val parser = ExpressionParser(scanSource, cursor)
        val expression = parser.parseFormula()
        if (expression == null) { cursor++; continue }
        val end = parser.position
        if (end <= cursor) { cursor++; continue }
        if (!parser.sawSlash) { cursor = end; continue }
        if (!isEndBoundary(scanSource, end)) { cursor++; continue }
        val raw = source.substring(cursor, end)
        if (isKnownNonMathSlash(raw) || isDateOrPath(source, cursor, end)) { cursor = end; continue }
        spans += FractionExpressionSpan(cursor, end, expression.latex())
        cursor = end
    }
    return spans
}

/** Recognized slash-form physical units, retained as a single linear text span. */
data class PhysicalUnitSpan(val start: Int, val end: Int)

private const val UNIT_SYMBOLS = "(?:km|cm|mm|μm|nm|kg|mg|g|ms|min|h|s|m|A|K|mol|cd|kHz|MHz|Hz|kN|N|kPa|Pa|kJ|J|kW|W|mV|V|C|Ω|S|Wb|T|H|L|l|bar|atm|cal|eV|rad|sr)"
private const val UNIT_POWER = "(?:[²³]|\\^\\s*\\{?[23]\\}?)?"
private val physicalUnitPattern = Regex("(?<![\\p{L}\\p{N}_])$UNIT_SYMBOLS$UNIT_POWER(?:·$UNIT_SYMBOLS$UNIT_POWER)*\\s*/\\s*$UNIT_SYMBOLS$UNIT_POWER(?:·$UNIT_SYMBOLS$UNIT_POWER)*(?![\\p{L}\\p{N}_])")

fun findPhysicalUnitExpressions(source: String): List<PhysicalUnitSpan> {
    val codeRanges = Regex("`[^`\\n]*`").findAll(source).map { it.range }.toList()
    return physicalUnitPattern.findAll(source).mapNotNull { match ->
        val start = match.range.first
        val before = source.substring(0, start)
        val contextStart = before.indexOfLast { it.isWhitespace() || it in "([{<\\\"'" }.let { it + 1 }
        val preceding = before.substring(contextStart)
        val inUrl = preceding.contains("http://") || preceding.contains("https://")
        val inPath = preceding.startsWith("/") || preceding.startsWith("./") || preceding.startsWith("../") || Regex("^[A-Za-z]:/").containsMatchIn(preceding)
        if (inUrl || inPath || codeRanges.any { start in it }) null else PhysicalUnitSpan(start, match.range.last + 1)
    }.toList()
}

/** Converts upright TeX unit quotients to a linear upright unit while preserving the exponent. */
fun linearizeExplicitPhysicalUnitFractions(latex: String): String = explicitUnitFractionPattern.replace(latex) { match ->
    val numerator = match.groupValues[1]
    val denominator = match.groupValues[2]
    val exponent = match.groupValues[3]
    if (!isKnownPhysicalUnitRatio(numerator, denominator)) match.value
    else "\\mathrm{$numerator/$denominator${exponent.takeIf(String::isNotEmpty)?.let { "^{${it}}" } ?: ""}}"
}

private val explicitUnitFractionPattern = Regex("\\\\frac\\s*\\{\\s*\\\\mathrm\\{([A-Za-zμΩ]+)\\}\\s*}\\s*\\{\\s*\\\\mathrm\\{([A-Za-zμΩ]+)\\}\\s*(?:\\^\\s*\\{?(\\d+)\\}?)?\\s*}")

private fun isKnownPhysicalUnitRatio(numerator: String, denominator: String): Boolean =
    numerator in physicalUnitSymbols && denominator in physicalUnitSymbols

private val physicalUnitSymbols = setOf(
    "km", "cm", "mm", "μm", "nm", "kg", "mg", "g", "ms", "min", "h", "s", "m", "A", "K", "mol", "cd",
    "Hz", "kHz", "MHz", "N", "kN", "Pa", "kPa", "J", "kJ", "W", "kW", "C", "V", "mV", "Ω", "S", "Wb",
    "T", "H", "L", "l", "bar", "atm", "cal", "eV", "rad", "sr",
)

/** Formats a numeric quantity followed by a recognized unit quotient in linear TeX. */
fun formatPhysicalQuantity(source: String): String? {
    val unit = findPhysicalUnitExpressions(source).singleOrNull() ?: return null
    if (source.isBlank()) return null
    val end = source.indexOfLast { !it.isWhitespace() } + 1
    if (unit.end != end) return null
    val rawUnit = source.substring(unit.start, unit.end)
    val renderedUnit = rawUnit.replace("²", "^{2}").replace("³", "^{3}")
    val prefix = source.substring(0, unit.start).trim()
    if (prefix.isEmpty()) return "\\mathrm{$renderedUnit}"
    if (!Regex("[+−-]?\\d+(?:[.,]\\d+)?|[+−-]?\\d+/\\d+").matches(prefix)) return null
    val number = formatFractionExpression(prefix) ?: prefix.replace('−', '-')
    return "$number\\,\\mathrm{$renderedUnit}"
}

/** Formats a whole quotient-bearing formula, returning null unless every character is parsed. */
fun formatFractionExpression(source: String): String? {
    val trimmedStart = source.indexOfFirst { !it.isWhitespace() }
    if (trimmedStart < 0) return null
    val end = source.indexOfLast { !it.isWhitespace() } + 1
    val text = source.substring(trimmedStart, end)
    if ('`' in text || text.contains("http://") || text.contains("https://")) return null
    val parser = ExpressionParser(text, 0)
    val expression = parser.parseFormula() ?: return null
    if (parser.position != text.length || !parser.sawSlash) return null
    if (isKnownNonMathSlash(text) || isDateOrPath(text, 0, text.length)) return null
    return expression.latex()
}

private sealed interface Expr {
    fun latex(): String
    data class Atom(val value: String) : Expr { override fun latex() = value }
    data class Group(val value: Expr, val open: Char) : Expr {
        override fun latex(): String = when (open) {
            '(' -> "\\left(${value.latex()}\\right)"
            '[' -> "\\left[${value.latex()}\\right]"
            '|' -> "\\left|${value.latex()}\\right|"
            else -> value.latex()
        }
    }
    data class Tuple(val values: List<Expr>) : Expr {
        override fun latex(): String = values.joinToString(",") { it.latex() }
    }
    data class Unary(val op: String, val value: Expr) : Expr {
        override fun latex(): String = op + value.latex()
    }
    data class Binary(val left: Expr, val op: String, val right: Expr) : Expr {
        override fun latex(): String = when (op) {
            "/" -> if (left is Unary && left.op == "-") "-\\frac{${left.value.latex()}}{${right.latex()}}"
                else "\\frac{${left.latex()}}{${right.latex()}}"
            "*" -> "${left.latex()}\\cdot ${right.latex()}"
            "" -> if (left.isNamedFunction()) "${left.latex()} ${right.latex()}" else "${left.latex()}${right.latex()}"
            else -> "${left.latex()}$op${right.latex()}"
        }
    }
    data class Power(val base: Expr, val exponent: String) : Expr {
        override fun latex(): String = "${base.latex()}^{$exponent}"
    }
    data class Call(val name: String, val args: List<Expr>) : Expr {
        override fun latex(): String = "${name.latexAtom()}\\left(${args.joinToString(",") { it.latex() }}\\right)"
    }
    data class Prime(val value: Expr) : Expr { override fun latex() = "${value.latex()}^{\\prime}" }
    data class Factorial(val value: Expr) : Expr { override fun latex() = "${value.latex()}!" }
}

/** Small Pratt-style parser. It deliberately rejects syntax it cannot render without guessing. */
private class ExpressionParser(private val source: String, start: Int) {
    var position: Int = start
        private set
    var sawSlash: Boolean = false
        private set
    private var nestedCommaContext: Int = 0
    private var absoluteValueDepth: Int = 0

    fun parseFormula(): Expr? = parseComparison()

    private fun parseComparison(): Expr? {
        var left = parseSum() ?: return null
        while (true) {
            val beforeSpace = position
            skipSpace()
            val op = listOf("<=", ">=", "=", "<", ">").firstOrNull { source.startsWith(it, position) }
            if (op == null) { position = beforeSpace; break }
            position += op.length
            val right = parseSum()
            if (right == null) { position = beforeSpace; break }
            left = Expr.Binary(left, op, right)
        }
        return left
    }

    private fun parseSum(): Expr? {
        var left = parseProduct() ?: return null
        while (true) {
            val beforeSpace = position
            skipSpace()
            val op = when { source.startsWith("+", position) -> "+"; source.startsWith("-", position) -> "-"; source.startsWith("−", position) -> "−"; else -> null }
            if (op == null) { position = beforeSpace; break }
            position++
            val right = parseProduct()
            if (right == null) { position = beforeSpace; break }
            left = Expr.Binary(left, if (op == "−") "-" else op, right)
        }
        return left
    }

    private fun parseProduct(): Expr? {
        var left = parseUnary() ?: return null
        while (true) {
            val beforeSpace = position
            skipSpace()
            val operandStart = position
            val spaced = position > beforeSpace
            when {
                source.startsWith("/", position) -> {
                    position++; sawSlash = true
                    val right = parseUnary() ?: return null
                    left = Expr.Binary(left, "/", right)
                }
                source.startsWith("*", position) || source.startsWith("×", position) || source.startsWith("·", position) -> {
                    position++; val right = parseUnary() ?: return null
                    left = Expr.Binary(left, "*", right)
                }
                startsPrimary(position) && (!spaced || left.isNamedFunction()) -> {
                    val right = parseUnary() ?: return null
                    left = Expr.Binary(left, "", right)
                }
                else -> { position = beforeSpace; return left }
            }
            if (position <= operandStart) return null
        }
    }

    private fun parseUnary(): Expr? {
        skipSpace()
        if (position < source.length && source[position] in "+-−") {
            val sign = if (source[position++] == '−') "-" else source[position - 1].toString()
            return Expr.Unary(sign, parseUnary() ?: return null)
        }
        if (position < source.length && source[position] == '√') {
            position++
            val operand = parsePrimary() ?: return null
            return Expr.Atom("\\sqrt{${operand.latex()}}")
        }
        return parsePower()
    }

    private fun parsePower(): Expr? {
        var value = parsePrimary() ?: return null
        while (position < source.length && source[position] == '′') { position++; value = Expr.Prime(value) }
        if (position < source.length && source[position] == '^') {
            position++
            val exponent = parseExponent() ?: return null
            value = Expr.Power(value, exponent)
        }
        if (value is Expr.Group && position < source.length && source[position] in UNICODE_SUPERSCRIPTS) {
            val start = position
            while (position < source.length && source[position] in UNICODE_SUPERSCRIPTS) position++
            val exponent = source.substring(start, position).normalizeAtomText().removePrefix("^{").removeSuffix("}")
            value = Expr.Power(value, exponent)
        }
        while (position < source.length && source[position] == '!') {
            position++
            value = Expr.Factorial(value)
        }
        if (value is Expr.Prime && position < source.length && source[position] == '(') {
            val argument = parsePrimary() ?: return null
            value = Expr.Binary(value, "", argument)
        }
        if (value.isNamedFunction()) {
            val beforeSpace = position
            skipSpace()
            if (position > beforeSpace && startsPrimary(position)) {
                val argument = parseUnary() ?: return null
                value = Expr.Binary(value, "", argument)
            } else position = beforeSpace
        }
        return value
    }

    private fun parseExponent(): String? {
        if (position >= source.length) return null
        if (source[position] == '{') {
            position++
            val start = position
            var depth = 1
            while (position < source.length && depth > 0) {
                if (source[position] == '{') depth++
                if (source[position] == '}') depth--
                position++
            }
            if (depth != 0) return null
            return source.substring(start, position - 1).takeIf { it.isNotBlank() }?.normalizeAtomText()
        }
        if (source[position] == '(') {
            val group = parsePrimary() ?: return null
            return "\\left(${group.latex()}\\right)"
        }
        val sign = when (source[position]) {
            '+' -> { position++; "+" }
            '-', '−' -> { position++; "-" }
            else -> ""
        }
        val start = position
        while (position < source.length && (source[position].isLetterOrDigit() || source[position] in "₀₁₂₃₄₅₆₇₈₉ₓₙₖₘₐₑᵢ⁰¹²³⁴⁵⁶⁷⁸⁹⁻ᵗⁿⁱʲᴮ")) position++
        return source.substring(start, position).takeIf { it.isNotEmpty() }?.normalizeAtomText()?.let { sign + it }
    }

    private fun parsePrimary(): Expr? {
        skipSpace()
        if (position >= source.length) return null
        val open = source[position]
        if (open == '(' || open == '[' || open == '|') {
            position++
            if (open == '|') absoluteValueDepth++
            try {
                var value = parseComparison() ?: return null
                if (open == '(') {
                    val parts = mutableListOf(value)
                    while (true) {
                        skipSpace()
                        if (position >= source.length || source[position] != ',') break
                        position++
                        parts += parseComparison() ?: return null
                    }
                    if (parts.size > 1) value = Expr.Tuple(parts)
                }
                skipSpace()
                val expected = when (open) { '(' -> ')'; '[' -> ']'; else -> '|' }
                if (position >= source.length || source[position] != expected) return null
                position++
                return Expr.Group(value, open)
            } finally {
                if (open == '|') absoluteValueDepth--
            }
        }
        if (!(source[position].isLetterOrDigit() || source[position] in "π∞Δ∩∪∂")) return null
        val start = position
        if (source[position].isDigit()) {
            while (position < source.length && source[position].isDigit()) position++
            if (position + 1 < source.length && source[position] == '.' && source[position + 1].isDigit()) {
                position++
                while (position < source.length && source[position].isDigit()) position++
            } else if (nestedCommaContext == 0 && position > start && position + 1 < source.length && source[position] == ',' && source[position + 1].isDigit()) {
                position++
                while (position < source.length && source[position].isDigit()) position++
            }
            while (position < source.length && source[position] in UNICODE_SUPERSCRIPTS + UNICODE_SUBSCRIPTS) position++
        } else {
            while (position < source.length && (source[position].isLetterOrDigit() || source[position] in "π∞Δ∩∪∂" || source[position] in UNICODE_SUPERSCRIPTS + UNICODE_SUBSCRIPTS || source[position] in "_{}")) position++
        }
        val rawName = source.substring(start, position)
        var atom: Expr = Expr.Atom(rawName.normalizeAtomText().latexAtom())
        val afterName = position
        skipSpace()
        if (position < source.length && source[position] == '(' && afterName == position) {
            position++
            val args = mutableListOf<Expr>()
            skipSpace()
            if (position < source.length && source[position] == ')') position++
            else {
                while (true) {
                    nestedCommaContext++
                    val argument = parseComparison()
                    nestedCommaContext--
                    args += argument ?: return null
                    skipSpace()
                    if (position < source.length && source[position] == ',') { position++; continue }
                    if (position < source.length && source[position] == ')') { position++; break }
                    return null
                }
            }
            atom = Expr.Call(rawName.normalizeAtomText(), args)
        } else position = afterName
        return atom
    }

    private fun startsPrimary(at: Int): Boolean = at < source.length && (source[at] == '(' || source[at] == '[' || (source[at] == '|' && absoluteValueDepth == 0) || source[at] == '√' || source[at].isLetterOrDigit() || source[at] in "π∞Δ∩∪∂")
    private fun skipSpace() { while (position < source.length && source[position].isWhitespace()) position++ }
}

private fun Expr.isNamedFunction(): Boolean = when (this) {
    is Expr.Atom -> listOf("\\sin", "\\cos", "\\tan", "\\log", "\\ln", "\\exp", "\\max", "\\min")
        .any { value == it || value.startsWith("$it^") }
    is Expr.Power -> base.isNamedFunction()
    else -> false
}

private const val UNICODE_SUPERSCRIPTS = "⁰¹²³⁴⁵⁶⁷⁸⁹⁽⁾⁻⁺ˣᵗⁿⁱʲᴮᵖᵏᴺᵀᵃᵇᵈᵉᵍᵐᵘᶜʰʳʸˡᴴᴿᵅᵝ"
private const val UNICODE_SUBSCRIPTS = "₀₁₂₃₄₅₆₇₈₉ₓₙₖₘₐₑᵢ₊₋₌ₚⱼ"

private fun String.normalizeAtomText(): String = buildString {
    val input = this@normalizeAtomText
    var i = 0
    while (i < input.length) {
        when (val c = input[i]) {
            '−' -> append('-')
            'π' -> append("\\pi ")
            '∩' -> append("\\cap ")
            '∪' -> append("\\cup ")
            '∂' -> append("\\partial ")
            'Σ' -> append("\\sum ")
            '×' -> append("\\times ")
            '·' -> append("\\cdot ")
            '′' -> append("^{\\prime}")
            in UNICODE_SUBSCRIPTS -> {
                append("_{")
                while (i < input.length && input[i] in UNICODE_SUBSCRIPTS) append(input[i++].toSubscript())
                append('}')
                continue
            }
            in UNICODE_SUPERSCRIPTS -> {
                append("^{")
                while (i < input.length && input[i] in UNICODE_SUPERSCRIPTS) append(input[i++].toSuperscript())
                append('}')
                continue
            }
            else -> append(c)
        }
        i++
    }
}

private fun String.latexAtom(): String {
    val functions = listOf("sin", "cos", "tan", "lim", "log", "ln", "exp", "max", "min")
    val function = functions.firstOrNull { name -> startsWith(name) && (length == name.length || !get(name.length).isLetterOrDigit()) }
        ?: return this
    return "\\$function${substring(function.length)}"
}

private fun Char.toSubscript(): Char = when (this) {
    '₀' -> '0'; '₁' -> '1'; '₂' -> '2'; '₃' -> '3'; '₄' -> '4'; '₅' -> '5'; '₆' -> '6'; '₇' -> '7'; '₈' -> '8'; '₉' -> '9'
    'ₓ' -> 'x'; 'ₙ' -> 'n'; 'ₖ' -> 'k'; 'ₘ' -> 'm'; 'ₐ' -> 'a'; 'ₑ' -> 'e'; 'ᵢ' -> 'i'; '₊' -> '+'; '₋' -> '-'; '₌' -> '='; 'ₚ' -> 'p'; 'ⱼ' -> 'j'; else -> this
}
private fun Char.toSuperscript(): Char = when (this) {
    '⁰' -> '0'; '¹' -> '1'; '²' -> '2'; '³' -> '3'; '⁴' -> '4'; '⁵' -> '5'; '⁶' -> '6'; '⁷' -> '7'; '⁸' -> '8'; '⁹' -> '9'
    '⁽' -> '('; '⁾' -> ')'; '⁻' -> '-'; '⁺' -> '+'; 'ˣ' -> 'x'; 'ᵗ' -> 't'; 'ⁿ' -> 'n'; 'ⁱ' -> 'i'; 'ʲ' -> 'j'
    'ᵖ' -> 'p'; 'ᵏ' -> 'k'; 'ᴺ' -> 'N'; 'ᵀ' -> 'T'; 'ᵃ' -> 'a'; 'ᵇ' -> 'b'; 'ᵈ' -> 'd'; 'ᵉ' -> 'e'; 'ᵍ' -> 'g'
    'ᵐ' -> 'm'; 'ᵘ' -> 'u'; 'ᶜ' -> 'c'; 'ʰ' -> 'h'; 'ʳ' -> 'r'; 'ʸ' -> 'y'; 'ˡ' -> 'l'; 'ᴮ' -> 'B'; 'ᴴ' -> 'H'; 'ᴿ' -> 'R'
    'ᵅ' -> 'α'; 'ᵝ' -> 'β'; else -> this
}

private fun isStartBoundary(s: String, i: Int): Boolean = i == 0 || !(s[i - 1].isLetterOrDigit() || s[i - 1] == '_' || s[i - 1] == '/' || s[i - 1] == '.')
private fun isEndBoundary(s: String, i: Int): Boolean {
    if (i == s.length) return true
    val next = s[i]
    if (next.isLetterOrDigit() || next == '_' || next == '/') return false
    return next != '.' || i + 1 == s.length || s[i + 1].isWhitespace() || s[i + 1] in ",;:!?)]}"
}
private fun isKnownNonMathSlash(raw: String): Boolean {
    val compact = raw.lowercase().filterNot(Char::isWhitespace).trimEnd('.', ',', ';', ':', '!', '?')
    return compact in setOf("vrai/faux", "vraie/fausse", "acceptant/rejetant", "et/ou", "ou/et")
}
private fun isDateOrPath(s: String, start: Int, end: Int): Boolean {
    val raw = s.substring(start, end)
    return Regex("^\\d{4}/\\d{1,2}(?:/\\d{1,2})?$|^[A-Za-z]:/|^\\.?\\.?/|^(?:[A-Za-z0-9_-]+/)+[A-Za-z0-9_-]+\\.[A-Za-z0-9]+$").matches(raw)
}
