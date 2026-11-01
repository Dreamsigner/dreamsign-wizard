package com.dreamsigner.wizard.ui.studio

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable data class StudioDiagnosticStrand(
    val strand: String,
    val floor: String,
    val ceiling: String,
    val misconception: String? = null,
    val sources: List<Int>,
)

@Serializable data class StudioDiagnosticAssessment(
    val version: Int,
    val strands: List<StudioDiagnosticStrand>,
    val summary: String,
)

internal const val STUDIO_DIAGNOSIS_REQUEST = "Conclue le diagnostic à partir des questions auxquelles j'ai répondu. Réponds uniquement par un bloc ```studio-diagnosis contenant un objet JSON avec version (1), strands (1 à 12 objets avec strand, floor, ceiling, misconception facultatif et sources) et summary. Pour chaque dépendance du sujet, décris ce que je sais faire de façon fiable, où ma compréhension s'arrête et les confusions observées. Sources est un tableau d'indices de messages de QCM déjà répondus, en commençant à 0. N'affirme aucun acquis sans preuve ; marque l'incertitude dans floor ou ceiling."

internal fun validStudioDiagnostic(assessment: StudioDiagnosticAssessment, answeredQuizIndices: Set<Int>): Boolean =
    assessment.version == 1 && assessment.strands.size in 1..12 &&
        assessment.strands.map { it.strand.trim().lowercase() }.distinct().size == assessment.strands.size &&
        assessment.summary.isNotBlank() && assessment.summary.length <= 3_000 &&
        assessment.strands.all { strand ->
            strand.strand.isNotBlank() && strand.strand.length <= 300 &&
                strand.floor.isNotBlank() && strand.floor.length <= 2_000 &&
                strand.ceiling.isNotBlank() && strand.ceiling.length <= 2_000 &&
                (strand.misconception == null || strand.misconception.length <= 2_000) &&
                strand.sources.isNotEmpty() && strand.sources.distinct().size == strand.sources.size &&
                strand.sources.all { it in answeredQuizIndices }
        }

internal fun parseStudioDiagnostic(reply: String, answeredQuizIndices: Set<Int>): StudioDiagnosticAssessment? {
    if (reply.length > 20_000) return null
    val match = Regex("(?s)\\A\\s*```studio-diagnosis\\s*\\n(.*?)\\n```\\s*\\z").matchEntire(reply) ?: return null
    return runCatching { Json.decodeFromString<StudioDiagnosticAssessment>(match.groupValues[1]) }.getOrNull()
        ?.takeIf { validStudioDiagnostic(it, answeredQuizIndices) }
}
