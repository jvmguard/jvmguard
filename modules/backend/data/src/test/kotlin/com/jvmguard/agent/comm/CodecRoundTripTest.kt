package com.jvmguard.agent.comm

import com.grack.nanojson.JsonObject
import com.grack.nanojson.JsonParser
import com.grack.nanojson.JsonWriter
import com.jvmguard.agent.config.AgentGroupConfig
import com.jvmguard.agent.config.recording.RetransformationType
import com.jvmguard.agent.config.telemetry.MBeanLineConfig
import com.jvmguard.agent.config.telemetry.MBeanTelemetryConfig
import com.jvmguard.agent.config.telemetry.TelemetryUnit
import com.jvmguard.agent.config.transactions.MatchedTransactionDef
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

class CodecRoundTripTest {

    @Test
    fun binaryRoundTrips() {
        assertPopulated(roundTripBinary(populated()))
    }

    @Test
    fun jsonRoundTrips() {
        val root = JsonObject()
        populated().writeState(JsonAgentWriter(root))
        val parsed = JsonParser.`object`().from(JsonWriter.string(root))
        val back = AgentGroupConfig()
        back.readState(JsonAgentReader(parsed))
        assertPopulated(back)
    }

    @Test
    fun jsonNestsSubBeansWithoutCollision() {
        val root = JsonObject()
        populated().writeState(JsonAgentWriter(root))
        assertNotNull(root.getObject("recordingOptions"), "recordingOptions must be a nested object")
        assertNotNull(root.getObject("transactionSettings"), "transactionSettings must be a nested object")
        assertFalse(
            root.has("retransformationType"),
            "retransformationType must NOT be inlined at the AgentGroupConfig level (would collide between RecordingOptions and TransactionSettings)",
        )
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun register() {
            CodecTypes.registerAll()
        }

        private fun roundTripBinary(original: AgentGroupConfig): AgentGroupConfig {
            val bout = ByteArrayOutputStream()
            DataOutputStream(bout).use { out ->
                original.writeState(BinaryAgentWriter(out))
            }
            val back = AgentGroupConfig()
            DataInputStream(ByteArrayInputStream(bout.toByteArray())).use { input ->
                back.readState(BinaryAgentReader(input))
            }
            return back
        }

        private fun populated(): AgentGroupConfig {
            val agc = AgentGroupConfig()
            agc.recordingOptions.setRetransformationType(RetransformationType.STARTUP)
            agc.transactionSettings.retransformationType = RetransformationType.ALWAYS
            val pojo = MatchedTransactionDef()
            pojo.declaringClassName = "com.example.Foo"
            pojo.methodName = "bar"
            agc.transactionSettings.transactionDefs.add(pojo)
            val mbean = MBeanTelemetryConfig("heap", TelemetryUnit.PLAIN, 0, true, false)
            mbean.lines.add(MBeanLineConfig("java.lang:type=Memory", "HeapMemoryUsage/used", "used"))
            agc.telemetrySettings.mbeanTelemetries.add(mbean)
            return agc
        }

        private fun assertPopulated(back: AgentGroupConfig) {
            assertEquals(RetransformationType.ALWAYS, back.transactionSettings.retransformationType)

            assertEquals(1, back.transactionSettings.transactionDefs.size)
            assertInstanceOf(MatchedTransactionDef::class.java, back.transactionSettings.transactionDefs[0])
            val pojoBack = back.transactionSettings.transactionDefs[0] as MatchedTransactionDef
            assertEquals("com.example.Foo", pojoBack.declaringClassName)
            assertEquals("bar", pojoBack.methodName)

            assertEquals(1, back.telemetrySettings.mbeanTelemetries.size)
            assertEquals("heap", back.telemetrySettings.mbeanTelemetries[0].name)
            assertEquals(1, back.telemetrySettings.mbeanTelemetries[0].lines.size)
        }
    }
}
