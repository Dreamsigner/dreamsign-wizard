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
    val units: List<LearningUnit>
)

@Serializable
data class LearningUnit(
    val id: String,
    val unitNumber: Int,
    val title: String,
    val description: String,
    val lessons: List<Lesson>
)

@Serializable
data class Lesson(
    val id: String,
    val title: String,
    val xpReward: Int = 15,
    val isCompleted: Boolean = false,
    val isLocked: Boolean = true,
    val exercises: List<Exercise>
)

@Serializable
sealed class Exercise {
    abstract val id: String
    abstract val prompt: String
    abstract val explanation: String

    @Serializable
    data class MultipleChoice(
        override val id: String,
        override val prompt: String,
        val options: List<String>,
        val correctOptionIndex: Int,
        override val explanation: String
    ) : Exercise()

    @Serializable
    data class CodeChallenge(
        override val id: String,
        override val prompt: String,
        val codeSnippet: String,
        val options: List<String>,
        val correctOptionIndex: Int,
        override val explanation: String
    ) : Exercise()

    @Serializable
    data class MathChallenge(
        override val id: String,
        override val prompt: String,
        val formula: String? = null,
        val options: List<String>,
        val correctOptionIndex: Int,
        override val explanation: String
    ) : Exercise()

    @Serializable
    data class Translation(
        override val id: String,
        override val prompt: String,
        val sourceSentence: String,
        val targetTokens: List<String>,
        val correctSequence: List<String>,
        override val explanation: String
    ) : Exercise()

    @Serializable
    data class MatchingPairs(
        override val id: String,
        override val prompt: String,
        val pairs: Map<String, String>,
        override val explanation: String
    ) : Exercise()
}

@Serializable
data class UserStats(
    val xp: Int = 120,
    val streakDays: Int = 5,
    val hearts: Int = 5,
    val maxHearts: Int = 5,
    val completedLessonIds: Set<String> = emptySet(),
    val activeSubjectId: String = "math_vectors"
)

@Serializable
enum class IngestionSourceType {
    PDF_DOCUMENT,
    YOUTUBE_VIDEO
}

@Serializable
data class ResourceIngestionItem(
    val id: String,
    val title: String,
    val sourceType: IngestionSourceType,
    val sourceUrlOrPath: String,
    val status: String = "READY_TO_PROCESS", // READY_TO_PROCESS, TRANSCRIBING, GENERATING_LESSONS, COMPLETED
    val rawText: String = "",
    val generatedLessonsCount: Int = 0,
    val targetSubjectId: String
)
