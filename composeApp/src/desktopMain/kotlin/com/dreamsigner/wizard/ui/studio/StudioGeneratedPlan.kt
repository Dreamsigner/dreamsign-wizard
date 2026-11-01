package com.dreamsigner.wizard.ui.studio

import kotlinx.serialization.json.*

/** Indices are zero-based, matching StudioPlan and the session message list. */
internal data class GeneratedDiagnosis(val observation: String, val sources: List<Int>)
internal data class GeneratedPlanStep(val title: String, val prerequisites: List<Int>, val evidence: String, val sources: List<Int>)
internal data class GeneratedPlan(val diagnosis: List<GeneratedDiagnosis>, val steps: List<GeneratedPlanStep>)

/** A null result means that the caller must retain the original response as ordinary text. */
internal fun parseGeneratedPlan(response: String, visibleMessageIndices: Set<Int>): GeneratedPlan? {
    if (response.length > 20_000 || visibleMessageIndices.any { it < 0 }) return null
    val match = Regex("(?s)\\A\\s*```studio-plan\\s*\\n(.*?)\\n```\\s*\\z").matchEntire(response) ?: return null
    val root = try { Json.parseToJsonElement(match.groupValues[1]) as? JsonObject } catch (_: IllegalArgumentException) { null } ?: return null
    fun JsonObject.text(key: String, max: Int): String? {
        val value = this[key] as? JsonPrimitive ?: return null
        if (!value.isString) return null
        return value.content.trim().takeIf { it.isNotEmpty() && it.length <= max }
    }
    fun JsonObject.indices(key: String, allowed: (Int) -> Boolean, required: Boolean): List<Int>? {
        val values = this[key] as? JsonArray ?: return null
        if (values.size > 100 || (required && values.isEmpty())) return null
        val parsed = values.map { item ->
            val primitive = item as? JsonPrimitive ?: return null
            if (primitive.isString) return null
            primitive.intOrNull ?: return null
        }
        return parsed.takeIf { it.distinct().size == it.size && it.all(allowed) }
    }
    if (root.keys != setOf("version", "diagnosis", "steps") || (root["version"] as? JsonPrimitive)?.intOrNull != 1) return null
    val diagnoses = root["diagnosis"] as? JsonArray ?: return null
    val steps = root["steps"] as? JsonArray ?: return null
    if (diagnoses.size !in 1..20 || steps.size !in 1..20) return null
    val parsedDiagnosis = diagnoses.map { entry ->
        val item = entry as? JsonObject ?: return null
        if (item.keys != setOf("observation", "sources")) return null
        GeneratedDiagnosis(item.text("observation", 2000) ?: return null,
            item.indices("sources", { it in visibleMessageIndices }, true) ?: return null)
    }
    val parsedSteps = steps.mapIndexed { index, entry ->
        val item = entry as? JsonObject ?: return null
        if (item.keys != setOf("title", "prerequisites", "evidence", "sources")) return null
        GeneratedPlanStep(item.text("title", 500) ?: return null,
            item.indices("prerequisites", { it in 0 until index }, false) ?: return null,
            item.text("evidence", 2000) ?: return null,
            item.indices("sources", { it in visibleMessageIndices }, true) ?: return null)
    }
    return GeneratedPlan(parsedDiagnosis, parsedSteps)
}
