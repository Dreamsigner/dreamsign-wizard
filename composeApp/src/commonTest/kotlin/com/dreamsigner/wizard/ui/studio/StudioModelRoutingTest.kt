package com.dreamsigner.wizard.ui.studio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StudioModelRoutingTest {
    private val strong = StudioModelChoice("api", "https://example.test", "strong")
    private val cheap = StudioModelChoice("api", "https://example.test", "cheap")
    private val alternate = StudioModelChoice("chatgpt", "", "alternate")
    private val slots = mapOf(
        StudioModelSlot.STRONG to StudioModelSetting(strong, ReasoningEffort.MEDIUM),
        StudioModelSlot.CHEAP to StudioModelSetting(cheap, ReasoningEffort.LOW),
    )

    @Test
    fun rolesUseTheirAssignedSlots() {
        for (role in listOf(StudioModelRole.DIAGNOSTIC, StudioModelRole.TEACHING_PLAN, StudioModelRole.PRACTICE)) {
            assertEquals(StudioResolvedModel(strong, ReasoningEffort.MEDIUM), resolveStudioModel(role, slots))
        }
        assertEquals(StudioResolvedModel(cheap, ReasoningEffort.LOW), resolveStudioModel(StudioModelRole.GRAPH_EXTRACTION, slots))
    }

    @Test
    fun overridesResolveEachFieldIndependently() {
        val overrides = mapOf(StudioModelRole.PRACTICE to StudioModelSetting(model = alternate))
        val session = StudioModelSetting(reasoningEffort = ReasoningEffort.HIGH)
        assertEquals(StudioResolvedModel(alternate, ReasoningEffort.HIGH), resolveStudioModel(StudioModelRole.PRACTICE, slots, overrides, session))
        assertEquals(StudioResolvedModel(alternate, ReasoningEffort.MEDIUM), resolveStudioModel(StudioModelRole.PRACTICE, slots, overrides))
        assertEquals(StudioResolvedModel(strong, ReasoningEffort.HIGH), resolveStudioModel(StudioModelRole.DIAGNOSTIC, slots, overrides, session))
        assertEquals(StudioResolvedModel(cheap, ReasoningEffort.HIGH), resolveStudioModel(StudioModelRole.PRACTICE, slots, overrides, StudioModelSetting(cheap, ReasoningEffort.HIGH)))
    }

    @Test
    fun noModelMeansAbsentEvenWhenEffortIsConfigured() {
        assertNull(resolveStudioModel(StudioModelRole.DIAGNOSTIC, emptyMap(), sessionOverride = StudioModelSetting(reasoningEffort = ReasoningEffort.HIGH)))
        assertEquals(StudioResolvedModel(alternate, null), resolveStudioModel(StudioModelRole.DIAGNOSTIC, emptyMap(), sessionOverride = StudioModelSetting(model = alternate)))
    }

    @Test
    fun compatibilityDistinguishesUnknownAndReportedLevels() {
        val unknown = StudioModelCapabilities()
        assertEquals(StudioEffortCompatibility.UNKNOWN, studioEffortCompatibility(ReasoningEffort.HIGH, unknown))
        assertEquals(StudioEffortCompatibility.SUPPORTED, studioEffortCompatibility(ReasoningEffort.AUTOMATIC, unknown))
        val known = StudioModelCapabilities(supportedEfforts = StudioCapability.Known(setOf(ReasoningEffort.LOW)))
        assertEquals(StudioEffortCompatibility.SUPPORTED, studioEffortCompatibility(ReasoningEffort.LOW, known))
        assertEquals(StudioEffortCompatibility.UNSUPPORTED, studioEffortCompatibility(ReasoningEffort.HIGH, known))
        assertEquals(StudioEffortCompatibility.UNSUPPORTED, studioEffortCompatibility(ReasoningEffort.LOW, StudioModelCapabilities(supportedEfforts = StudioCapability.Known(emptySet()))))
    }
}
