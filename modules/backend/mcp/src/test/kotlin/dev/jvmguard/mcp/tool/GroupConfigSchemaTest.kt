package dev.jvmguard.mcp.tool

import dev.jvmguard.agent.comm.CodecTypes
import dev.jvmguard.data.config.GroupConfig
import dev.jvmguard.data.config.external.RecordingConfig
import dev.jvmguard.data.config.triggers.PolicyTrigger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class GroupConfigSchemaTest {

    companion object {
        @BeforeAll
        @JvmStatic
        fun registerCodecs() {
            CodecTypes.registerAll()
        }
    }

    private val reference = GroupConfigSchema.reference()

    @Suppress("UNCHECKED_CAST")
    private fun sections() = reference["sections"] as List<Map<String, Any?>>

    @Suppress("UNCHECKED_CAST")
    private fun list(key: String) = reference[key] as List<Map<String, Any?>>

    @Test
    @Suppress("UNCHECKED_CAST")
    fun documentsRecordingOptionsWithEnumDomain() {
        val recording = sections().first { it["type"] == "RecordingOptions" }
        val field = (recording["fields"] as List<Map<String, Any?>>).first { it["key"] == "retransformationType" }
        assertEquals("enum", field["type"])
        val values = (field["values"] as List<Map<String, Any?>>).map { it["value"] }
        assertTrue(values.containsAll(listOf("ALWAYS", "FIRST_CONNECTION", "STARTUP")))
    }

    @Test
    fun cataloguesTransactionAndTriggerVariants() {
        val transactionClasses = list("transactionTypes").map { it["class"] }
        assertTrue(transactionClasses.containsAll(listOf("MatchedTransactionDef", "MappedTransactionDef", "DeclaredTransactionDef")))
        val triggerTypes = list("triggerTypes").map { it["type"] }
        assertTrue(triggerTypes.containsAll(listOf("CONNECTION", "POLICY", "THRESHOLD")))
    }

    @Test
    fun annotatedKeysMatchTheSerializedConfig() {
        val json = RecordingConfig.groupToJsonString(GroupConfig.createDefault(), includeGuardrails = false)
        assertTrue(json.contains("\"retransformationType\""), "the documented key must appear in the serialized config")
    }

    @Test
    fun serverConfigBeansExposeFullyQualifiedClassNames() {
        val policySection = sections().first { it["type"] == "PolicyTrigger" }
        assertEquals("dev.jvmguard.data.config.triggers.PolicyTrigger", policySection["className"])

        val triggerClassNames = list("triggerTypes").map { it["className"] }
        assertTrue(triggerClassNames.contains("dev.jvmguard.data.config.triggers.PolicyTrigger"))

        val actionClassNames = list("actionTypes").map { it["className"] }
        assertTrue(actionClassNames.contains("dev.jvmguard.data.config.triggers.actions.RecordJfrAction"))
    }

    @Test
    fun agentConfigCodecBeansDoNotExposeAClassName() {
        // The codec side keys on a simple @type discriminator, so a FQCN would only mislead the agent.
        val recording = sections().first { it["type"] == "RecordingOptions" }
        assertFalse(recording.containsKey("className"))
    }

    @Test
    fun theWorkedExampleIsValidInputForSetGroupConfig() {
        val example = reference["example"] as String

        // The agent must never see guardrail settings, even in the example.
        assertFalse(example.contains("guardrailSettings"), "the example must not leak guardrail settings")

        // It parses back through the exact facility set_group_config uses, and it carries the polymorphic types.
        val group = RecordingConfig.groupFromJsonString(example)
        val triggers = group.serverGroupConfig.triggerSettings.triggers
        assertTrue(triggers.any { it is PolicyTrigger }, "the example demonstrates a PolicyTrigger")
        assertTrue(triggers.first { it is PolicyTrigger }.triggerActions.isNotEmpty(), "with nested actions")
    }
}
