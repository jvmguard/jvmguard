package dev.jvmguard.data.config.external

import dev.jvmguard.agent.comm.CodecTypes
import dev.jvmguard.agent.config.VmType
import dev.jvmguard.agent.config.recording.RetransformationType
import dev.jvmguard.data.config.GroupConfig
import dev.jvmguard.data.vmdata.VmIdentifier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class GroupConfigJsonTest {

    private fun sampleGroup(): GroupConfig {
        val gc = GroupConfig.createDefault(VmIdentifier("Demo/Purchase", VmType.GROUP))
        gc.recordingOptions.setRetransformationType(RetransformationType.STARTUP)
        gc.guardrailSettings.isUsed = true
        gc.guardrailSettings.allowHeapDump = false
        return gc
    }

    @Test
    fun guardrailSettingsAreStrippedForTheAgentButKeptForExport() {
        val gc = sampleGroup()
        assertFalse(
            RecordingConfig.groupToJsonString(gc, includeGuardrails = false).contains("guardrailSettings"),
            "the agent must never see the guardrail settings",
        )
        assertTrue(
            RecordingConfig.groupToJsonString(gc, includeGuardrails = true).contains("guardrailSettings"),
            "a full export still carries them",
        )
    }

    @Test
    fun roundTripsTheEditableConfig() {
        val gc = sampleGroup()
        val json = RecordingConfig.groupToJsonString(gc, includeGuardrails = false)
        val back = RecordingConfig.groupFromJsonString(json)

        assertEquals("Demo/Purchase", back.hierarchyPath)
        // Re-serializing the parsed config reproduces the same JSON
        assertEquals(json, RecordingConfig.groupToJsonString(back, includeGuardrails = false))
        assertTrue(json.contains("STARTUP"))
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun registerCodecs() {
            CodecTypes.registerAll()
        }
    }
}
