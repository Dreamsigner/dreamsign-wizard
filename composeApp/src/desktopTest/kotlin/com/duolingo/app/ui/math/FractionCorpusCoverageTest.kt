package com.duolingo.app.ui.math

import com.duolingo.app.data.SampleData
import com.duolingo.app.model.Exercise
import com.duolingo.app.ui.LessonBlockKind
import com.duolingo.app.ui.parseLessonDocument
import androidx.compose.ui.graphics.Color
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class FractionCorpusCoverageTest {
    @Test fun allPublishedTeachingFieldsHaveNoUnclassifiedMathematicalSlashAndTheirFractionsRender() {
        val missing = mutableListOf<String>()
        val invalid = mutableListOf<String>()
        var fields = 0
        var fractions = 0
        fun inspect(id: String, source: String) {
            fields++
            parseLessonDocument(source).filter { it.kind != LessonBlockKind.CODE }.forEach { block ->
                parseLearningText(block.text, true).forEach segmentLoop@ { segment ->
                    when (segment) {
                        is LearningTextSegment.Text, is LearningTextSegment.Strong -> {
                            val value = when (segment) {
                                is LearningTextSegment.Text -> segment.value
                                is LearningTextSegment.Strong -> segment.value
                                else -> error("Unreachable")
                            }
                            if ('/' !in value) return@segmentLoop
                            // Filesystem paths are literal references, never numerical ratios.
                            val cleaned = value.replace(Regex("(?:https?://\\S+|(?<![A-Za-z0-9])/(?:tmp|usr|home|opt|etc|var)/[A-Za-z0-9_./-]*|[A-Za-z]:/\\S+)"), "")
                            if ('/' in cleaned) missing += "$id\t${value}"
                        }
                        is LearningTextSegment.Math -> if ("\\frac" in segment.latex) {
                            fractions += Regex("\\\\frac\\{").findAll(segment.latex).count()
                            if (renderMathBitmap(segment.latex, 24f, Color.Black, segment.display) == null) invalid += "$id\t${segment.latex}"
                        }
                        else -> Unit // Inline code keeps its authored programming meaning.
                    }
                }
            }
        }
        SampleData.sampleSubjects.flatMap { it.units }.forEach { course -> course.lessons.forEach { lesson ->
            inspect("${lesson.id}.title", lesson.title)
            inspect("${lesson.id}.notes", lesson.learningNotes)
            inspect("${lesson.id}.objective", lesson.objective)
            lesson.exercises.forEach { exercise ->
                inspect("${exercise.id}.prompt", exercise.prompt)
                inspect("${exercise.id}.hint", exercise.hint)
                inspect("${exercise.id}.explanation", exercise.explanation)
                val options = when (exercise) {
                    is Exercise.MultipleChoice -> exercise.options
                    is Exercise.MathChallenge -> exercise.options.also { exercise.formula?.let { formula ->
                        val normalized = normalizeMathFormula(formula)
                        inspect("${exercise.id}.formula", normalized?.let { "$$it$" } ?: formula)
                    } }
                    is Exercise.CodeChallenge -> exercise.options
                    is Exercise.ReadingChoice -> exercise.options.also { inspect("${exercise.id}.passage", exercise.passage) }
                    is Exercise.ListeningChoice -> exercise.options.also { inspect("${exercise.id}.transcript", exercise.transcript) }
                    is Exercise.Translation -> exercise.targetTokens.also {
                        inspect("${exercise.id}.sourceSentence", exercise.sourceSentence)
                        exercise.correctSequence.forEachIndexed { i, value -> inspect("${exercise.id}.sequence$i", value) }
                    }
                    is Exercise.MatchingPairs -> exercise.pairs.flatMap { listOf(it.key, it.value) }
                    is Exercise.WrittenProduction -> exercise.reviewCriteria + exercise.exampleResponse
                    is Exercise.SpokenProduction -> exercise.reviewCriteria + exercise.exampleOutline + exercise.roleplayTurns
                }
                options.forEachIndexed { index, value -> inspect("${exercise.id}.option$index", value) }
            }
        } }
        val directory = File("build/reports/fraction-coverage").apply { mkdirs() }
        File(directory, "unclassified.tsv").writeText(missing.joinToString("\n"))
        File(directory, "invalid-latex.tsv").writeText(invalid.joinToString("\n"))
        File(directory, "summary.txt").writeText("fields=$fields\nrenderedFractions=$fractions\nunclassified=${missing.size}\ninvalid=${invalid.size}\n")
        assertTrue(missing.isEmpty(), "${missing.size} unclassified source fragments; see fraction-coverage/unclassified.tsv")
        assertTrue(invalid.isEmpty(), "${invalid.size} invalid fraction renderings; see fraction-coverage/invalid-latex.tsv")
        assertTrue(fractions > 900, "The whole catalogue must be exercised, not just screenshot examples: $fractions")
    }
}
