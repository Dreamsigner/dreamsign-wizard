package com.dreamsigner.wizard.ui.studio

/** Only roles backed by Studio operations; this does not create background agents. */
enum class StudioModelRole { DIAGNOSTIC, TEACHING_PLAN, PRACTICE, GRAPH_EXTRACTION }
enum class StudioModelSlot { STRONG, CHEAP }

fun studioSlotFor(role: StudioModelRole): StudioModelSlot = when (role) {
    StudioModelRole.DIAGNOSTIC, StudioModelRole.TEACHING_PLAN, StudioModelRole.PRACTICE -> StudioModelSlot.STRONG
    StudioModelRole.GRAPH_EXTRACTION -> StudioModelSlot.CHEAP
}

/** Null fields inherit independently; a setting without a model is not executable. */
data class StudioModelSetting(
    val model: StudioModelChoice? = null,
    val reasoningEffort: ReasoningEffort? = null,
)

data class StudioResolvedModel(val model: StudioModelChoice, val reasoningEffort: ReasoningEffort?)

fun resolveStudioModel(
    role: StudioModelRole,
    slots: Map<StudioModelSlot, StudioModelSetting>,
    roleOverrides: Map<StudioModelRole, StudioModelSetting> = emptyMap(),
    sessionOverride: StudioModelSetting? = null,
): StudioResolvedModel? {
    val roleSetting = roleOverrides[role]
    val slot = slots[studioSlotFor(role)]
    val model = sessionOverride?.model ?: roleSetting?.model ?: slot?.model ?: return null
    val effort = sessionOverride?.reasoningEffort ?: roleSetting?.reasoningEffort ?: slot?.reasoningEffort
    return StudioResolvedModel(model, effort)
}

/** Unknown is distinct from a provider explicitly reporting an empty set or zero cost. */
sealed interface StudioCapability<out T> {
    data object Unknown : StudioCapability<Nothing>
    data class Known<T>(val value: T) : StudioCapability<T>
}

data class StudioModelCost(
    val inputPerMillionUsd: Double,
    val outputPerMillionUsd: Double,
    val cacheReadPerMillionUsd: Double,
    val cacheWritePerMillionUsd: Double,
)

data class StudioModelCapabilities(
    val supportedEfforts: StudioCapability<Set<ReasoningEffort>> = StudioCapability.Unknown,
    val acceptsImages: StudioCapability<Boolean> = StudioCapability.Unknown,
    val contextTokens: StudioCapability<Int> = StudioCapability.Unknown,
    val maxOutputTokens: StudioCapability<Int> = StudioCapability.Unknown,
    val cost: StudioCapability<StudioModelCost> = StudioCapability.Unknown,
)

enum class StudioEffortCompatibility { SUPPORTED, UNSUPPORTED, UNKNOWN }

/** Automatic omits an effort from the request, so it requires no provider capability. */
fun studioEffortCompatibility(
    effort: ReasoningEffort,
    capabilities: StudioModelCapabilities,
): StudioEffortCompatibility {
    if (effort == ReasoningEffort.AUTOMATIC) return StudioEffortCompatibility.SUPPORTED
    return when (val levels = capabilities.supportedEfforts) {
        StudioCapability.Unknown -> StudioEffortCompatibility.UNKNOWN
        is StudioCapability.Known -> if (effort in levels.value) StudioEffortCompatibility.SUPPORTED else StudioEffortCompatibility.UNSUPPORTED
    }
}
