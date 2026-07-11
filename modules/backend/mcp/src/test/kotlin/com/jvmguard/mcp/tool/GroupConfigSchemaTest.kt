package com.jvmguard.mcp.tool

import com.jvmguard.data.config.GroupConfig
import com.jvmguard.data.config.external.RecordingConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GroupConfigSchemaTest {

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
}
