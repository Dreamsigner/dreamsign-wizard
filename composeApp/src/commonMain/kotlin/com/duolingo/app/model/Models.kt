package com.duolingo.app.model

import kotlinx.serialization.Serializable

@Serializable
enum class SubjectCategory {
    MATHEMATICS,
    COMPUTER_SCIENCE,
    LANGUAGES
}

@Serializable
data class Subject(
    val id: String,
    val title: String,
    val icon: String,
    val description: String,
    val category: SubjectCategory,
    val units: List<LearningUnit>,
    /** Bump only when the course structure or stable lesson identifiers change. */
    val curriculumVersion: Int = 1
)

@Serializable
data class LearningUnit(
    val id: String,
    val unitNumber: Int,
    val title: String,
    val description: String,
    val lessons: List<Lesson>,
    val sourceUrl: String = "",
    val courseCode: String = "",
    val edition: String = "",
    val expectedLessonCount: Int? = null
)

@Serializable
data class Lesson(
    val id: String,
    val title: String,
    val xpReward: Int = 15,
    val isCompleted: Boolean = false,
    val isLocked: Boolean = true,
    val exercises: List<Exercise>,
    val objective: String = "",
    val learningNotes: String = "",
    val sourceUrls: List<String> = emptyList(),
    val prerequisiteLessonIds: List<String> = emptyList(),
    /** Set false for elective lessons that should not wait for the preceding path item. */
    val requiresPreviousLesson: Boolean = true,
    /** Stable content schema version, independent from the app release version. */
    val contentVersion: Int = 1,
    val sourceSessionNumber: Int? = null,
    val sourceTitle: String = "",
    val sessionKind: String = "lesson"
)

@Serializable
sealed class Exercise {
    abstract val id: String
    abstract val prompt: String
    abstract val explanation: String
    abstract val hint: String

    @Serializable
    data class SpokenProduction(
        override val id: String,
        override val prompt: String,
        val maxDurationSeconds: Int,
        val preparationSeconds: Int = 0,
        val minimumPracticeSeconds: Int = 5,
        val reviewCriteria: List<String>,
        val exampleOutline: String,
        val roleplayTurns: List<String> = emptyList(),
        override val explanation: String,
        override val hint: String
    ) : Exercise()

    @Serializable
    data class ListeningChoice(
        override val id: String,
        override val prompt: String,
        val audioResourcePath: String,
        val transcript: String,
        val options: List<String>,
        val correctOptionIndex: Int,
        override val explanation: String,
        override val hint: String
    ) : Exercise()

    /** Guided practice completed by a learner's explicit review, never an official language score. */
    @Serializable
    data class WrittenProduction(
        override val id: String,
        override val prompt: String,
        val minWords: Int,
        val maxWords: Int,
        val reviewCriteria: List<String>,
        val exampleResponse: String,
        override val explanation: String,
        override val hint: String
    ) : Exercise()

    @Serializable
    data class MultipleChoice(
        override val id: String,
        override val prompt: String,
        val options: List<String>,
        val correctOptionIndex: Int,
        override val explanation: String,
        override val hint: String = "Repère les mots-clés de la question et élimine les choix qui contredisent la définition."
    ) : Exercise()

    @Serializable
    data class CodeChallenge(
        override val id: String,
        override val prompt: String,
        val codeSnippet: String,
        val options: List<String>,
        val correctOptionIndex: Int,
        override val explanation: String,
        override val hint: String = "Lis le code ligne par ligne et distingue l’adresse de la valeur stockée à cette adresse."
    ) : Exercise()

    @Serializable
    data class MathChallenge(
        override val id: String,
        override val prompt: String,
        val formula: String? = null,
        val options: List<String>,
        val correctOptionIndex: Int,
        override val explanation: String,
        override val hint: String = "Écris la formule avant de remplacer les valeurs, puis vérifie les unités et les signes."
    ) : Exercise()

    @Serializable
    data class Translation(
        override val id: String,
        override val prompt: String,
        val sourceSentence: String,
        val targetTokens: List<String>,
        val correctSequence: List<String>,
        override val explanation: String,
        override val hint: String = "Commence par repérer l’ordre des propositions et les marqueurs qui relient les idées."
    ) : Exercise()

    @Serializable
    data class MatchingPairs(
        override val id: String,
        override val prompt: String,
        val pairs: Map<String, String>,
        override val explanation: String,
        override val hint: String = "Associe d’abord chaque terme à sa définition la plus précise."
    ) : Exercise()
}

@Serializable
data class UserStats(
    val xp: Int = 0,
    val streakDays: Int = 0,
    val hearts: Int = 5,
    val maxHearts: Int = 5,
    val completedLessonIds: Set<String> = emptySet(),
    val activeSubjectId: String = "math_vectors",
    val lastStudyDate: String? = null,
    val writtenDrafts: Map<String, String> = emptyMap(),
    val textModeCompletedLessonIds: Set<String> = emptySet(),
    val spokenRecordingDurationsMillis: Map<String, Long> = emptyMap()
)
