package com.dreamsigner.wizard.ui.studio

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class StudioProblem(
    val statement: String,
    val mode: String,
    val modelSolution: String,
    val hints: List<String> = emptyList(),
    val target: List<String> = emptyList(),
    val shownHints: Int = 0,
    val attempt: StudioProblemAttempt? = null,
    val result: StudioProblemResult? = null,
    val targetSnapshot: Map<String, String> = emptyMap(),
)

@Serializable
data class StudioProblemAttempt(val answer: String, val thinking: String, val submittedAtEpochMillis: Long)

@Serializable
data class StudioProblemResult(
    val outcome: String,
    val verdict: String,
    val missClass: String? = null,
    val retest: Boolean = false,
)

internal const val STUDIO_PROBLEM_REQUEST = "Propose un seul problème de pratique autonome adapté à mon objectif, au plan accepté et à mes erreurs précédentes. Réponds uniquement par un bloc ```studio-problem contenant un objet JSON avec statement, mode (recall, discrimination ou composition), modelSolution (solution complète préengagée), hints (tableau de 0 à 3 indices gradués sans dévoiler la solution) et target (tableau facultatif d'identifiants de propositions). N'affiche pas la solution dans statement ni dans un autre texte."
internal const val STUDIO_PRACTICE_ATTEMPT_PREFIX = "Évalue ma tentative sur le problème actif. Réponds uniquement par un bloc ```studio-verdict contenant un objet JSON avec outcome (solved, solvedWithHints ou movedOn), verdict (ce qui est juste et où le raisonnement casse, sans reformuler la solution déjà préengagée), missClass (cue, discrimination, execution, careless, structural ou null) et retest (booléen). Tentative :"

internal fun validStudioProblem(problem: StudioProblem): Boolean =
    problem.statement.isNotBlank() && problem.statement.length <= 4_000 &&
        problem.mode in setOf("recall", "discrimination", "composition") &&
        problem.modelSolution.isNotBlank() && problem.modelSolution.length <= 8_000 &&
        problem.hints.size <= 3 && problem.hints.all { it.isNotBlank() && it.length <= 2_000 } &&
        problem.target.size <= 20 && problem.target.distinct().size == problem.target.size &&
        problem.target.all { it.isNotBlank() && it.length <= 100 } &&
        problem.targetSnapshot.keys.all { it in problem.target } && problem.targetSnapshot.values.all { it.isNotBlank() && it.length <= 1_000 } &&
        problem.shownHints in 0..problem.hints.size &&
        (problem.attempt == null || (problem.attempt.answer.isNotBlank() && problem.attempt.answer.length <= 4_000 &&
            problem.attempt.thinking.length <= 3_000 && problem.attempt.submittedAtEpochMillis > 0)) &&
        (problem.result == null || (problem.result.outcome in setOf("solved", "solvedWithHints", "movedOn", "surrendered") &&
            problem.result.verdict.isNotBlank() && problem.result.verdict.length <= 4_000 &&
            problem.result.missClass in setOf(null, "cue", "discrimination", "execution", "careless", "structural") &&
            (problem.attempt != null || problem.result.outcome == "surrendered")))

internal fun parseStudioProblem(reply: String): StudioProblem? {
    if (reply.length > 20_000) return null
    val match = Regex("(?s)\\A\\s*```studio-problem\\s*\\n(.*?)\\n```\\s*\\z").matchEntire(reply) ?: return null
    return runCatching { Json.decodeFromString<StudioProblem>(match.groupValues[1]) }.getOrNull()
        ?.takeIf { it.shownHints == 0 && it.attempt == null && it.result == null && it.targetSnapshot.isEmpty() && validStudioProblem(it) }
}

internal fun studioProblemForTutor(problem: StudioProblem): String =
    "```studio-problem\n${Json.encodeToString(problem)}\n```"

internal fun parseStudioProblemVerdict(reply: String, shownHints: Int): StudioProblemResult? {
    if (reply.length > 8_000) return null
    val match = Regex("(?s)\\A\\s*```studio-verdict\\s*\\n(.*?)\\n```\\s*\\z").matchEntire(reply) ?: return null
    val value = runCatching { Json.decodeFromString<StudioProblemResult>(match.groupValues[1]) }.getOrNull() ?: return null
    if (value.outcome !in setOf("solved", "solvedWithHints", "movedOn") ||
        (value.outcome == "solvedWithHints") != (shownHints > 0 && value.outcome != "movedOn") ||
        value.verdict.isBlank() || value.verdict.length > 4_000 ||
        value.missClass !in setOf(null, "cue", "discrimination", "execution", "careless", "structural")) return null
    return value
}
