package com.dreamsigner.wizard.ui.studio

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal const val STUDIO_QUIZ_REQUEST = "Propose-moi une question de diagnostic ou de pratique sous forme de QCM structuré, adaptée à mon objectif et à mes réponses précédentes."
internal const val STUDIO_PLAN_REQUEST = "À partir de notre diagnostic, propose un plan pédagogique sous forme d'un unique bloc ```studio-plan suivi d'un objet JSON puis ``` sans autre texte. L'objet a exactement les champs version (entier 1), diagnosis (1 à 20 objets {observation, sources}), steps (1 à 20 objets {title, prerequisites, evidence, sources}). Les sources sont les indices de messages affichés, en commençant à 0, en excluant cette demande ; les prerequisites sont les indices d'étapes précédentes, en commençant à 0. Chaque source doit correspondre à une preuve présente dans la conversation. Ne déduis aucun acquis sans preuve."

@Serializable
data class StudioQuizOption(val id: String, val label: String)

@Serializable
data class StudioQuiz(
    val question: String,
    val options: List<StudioQuizOption>,
    val multiSelect: Boolean = false,
    val correctIds: List<String>,
    val explanation: String,
)

@Serializable
data class StudioQuizAnswer(
    val selectedIds: List<String>,
    val dontKnow: Boolean,
    val thinking: String,
    val confidence: Int?,
    val correct: Boolean,
    val answeredAtEpochMillis: Long,
)

internal data class ParsedStudioReply(val text: String, val quiz: StudioQuiz?)

/** A structured question must be complete and internally consistent before it is presented. */
internal fun validStudioQuiz(quiz: StudioQuiz): Boolean {
    val ids = quiz.options.map { it.id }
    val labels = quiz.options.map { it.label.trim().lowercase() }
    return quiz.question.isNotBlank() && quiz.question.length <= 4_000 &&
        quiz.options.size in 2..8 &&
        quiz.options.all { it.id.matches(Regex("[A-Za-z0-9_-]{1,64}")) && it.label.isNotBlank() && it.label.length <= 1_000 } &&
        ids.distinct().size == ids.size && labels.distinct().size == labels.size &&
        quiz.correctIds.isNotEmpty() && quiz.correctIds.distinct().size == quiz.correctIds.size &&
        quiz.correctIds.all { it in ids } && (quiz.multiSelect || quiz.correctIds.size == 1) &&
        quiz.explanation.isNotBlank() && quiz.explanation.length <= 4_000
}

internal fun validStudioQuizAnswer(quiz: StudioQuiz, answer: StudioQuizAnswer): Boolean {
    val selected = answer.selectedIds
    return selected.distinct().size == selected.size && selected.all { id -> quiz.options.any { it.id == id } } &&
        (!answer.dontKnow || selected.isEmpty()) && (answer.dontKnow || selected.isNotEmpty()) &&
        (quiz.multiSelect || selected.size <= 1) && answer.thinking.length <= 3_000 &&
        (answer.confidence == null || answer.confidence in 0..100) && answer.answeredAtEpochMillis > 0 &&
        answer.correct == (!answer.dontKnow && selected.toSet() == quiz.correctIds.toSet())
}

/** The private answer key stays in the question object and never appears in the visible text. */
internal fun parseStudioTutorReply(reply: String): ParsedStudioReply {
    val marker = "```studio-quiz"
    val start = reply.indexOf(marker)
    if (start < 0) return ParsedStudioReply(reply, null)
    val bodyStart = start + marker.length
    val end = reply.indexOf("```", bodyStart)
    val content = if (end >= 0) reply.substring(bodyStart, end).trim() else ""
    val suffix = if (end >= 0) reply.substring(end + 3) else ""
    val parsed = if (content.length <= 16_000 && marker !in suffix) {
        runCatching { Json.decodeFromString<StudioQuiz>(content) }.getOrNull()?.takeIf(::validStudioQuiz)
    } else null
    // Ignore surrounding prose for valid quizzes: an unreliable model could reveal the answer there.
    return if (parsed != null) ParsedStudioReply("", parsed)
    else ParsedStudioReply("La question structurée est invalide. Demande au tuteur de la reformuler.", null)
}

/** Reconstructs the model's own question, including its key, for later tutor turns. */
internal fun studioMessageForTutor(message: StudioMessage): String = message.quiz?.let { quiz ->
    listOf(message.text, "```studio-quiz\n${Json.encodeToString(quiz)}\n```").filter { it.isNotBlank() }.joinToString("\n\n")
} ?: message.text
