package com.jvmguard.data.config.external

import com.grack.nanojson.JsonParser
import com.jvmguard.agent.comm.CodecTypes
import com.jvmguard.agent.config.recording.RetransformationType
import com.jvmguard.agent.config.telemetry.MBeanLineConfig
import com.jvmguard.agent.config.telemetry.MBeanTelemetryConfig
import com.jvmguard.agent.config.telemetry.TelemetryUnit
import com.jvmguard.agent.config.transactions.MatchedTransactionDef
import com.jvmguard.data.config.GroupConfig
import com.jvmguard.data.user.AccessLevel
import com.jvmguard.data.user.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

class ExportRoundTripTest {

    @Test
    fun recordingConfigRoundTripsAgentBeans() {
        val original = GroupConfig.createDefault()
        populate(original)

        val bout = ByteArrayOutputStream()
        RecordingConfig(listOf(original)).export(bout)

        val exported = bout.toString(StandardCharsets.UTF_8)
        assertTrue(
            exported.contains("\"retransformationType\":\"STARTUP\""),
            "the non-default RecordingOptions.retransformationType must be serialized into the agentConfig",
        )

        val root = JsonParser.`object`().from(exported)
        val back = RecordingConfig()
        back.fromJson(root)

        assertEquals(1, back.groupConfigs.size)
        val gcBack = back.groupConfigs.iterator().next()
        assertPopulated(gcBack)
    }

    @Test
    fun serverInitConfigRoundTrips() {
        val original = GroupConfig.createDefault()
        populate(original)

        val bout = ByteArrayOutputStream()
        val user = User("admin", "Tester", "HASH-of-PASSWORD", "integration_test@test.com", AccessLevel.ADMIN)
        user.apiKeyHash = "HASH-of-APIKEY"
        ServerInitConfig(
            null,
            listOf(user),
            listOf(original),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
        ).export(bout)

        val exported = bout.toString(StandardCharsets.UTF_8)
        assertTrue(exported.contains("\"serverConfig\""), "serverInit must carry a top-level serverConfig")
        assertTrue(exported.contains("\"type\":\"serverInit\""))
        assertTrue(exported.contains("HASH-of-PASSWORD"), "the user's passwordHash must be serialized into the serverConfig")

        val root = JsonParser.`object`().from(exported)
        val back = ServerInitConfig()
        back.fromJson(root)

        assertNotNull(back.serverConfig, "serverInit must carry a top-level serverConfig")
        assertEquals(1, back.serverConfig.users!!.size, "the user must survive the export/import round-trip")
        val userBack = back.serverConfig.users!!.iterator().next()
        assertEquals("admin", userBack.loginName)
        assertEquals("HASH-of-PASSWORD", userBack.passwordHash, "the user's passwordHash must survive the round-trip")
        assertEquals("HASH-of-APIKEY", userBack.apiKeyHash)
        assertEquals(AccessLevel.ADMIN, userBack.accessLevel)
        assertEquals(1, back.recordingConfig.groupConfigs.size)
        val gcBack = back.recordingConfig.groupConfigs.iterator().next()
        assertPopulated(gcBack)
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun register() {
            CodecTypes.registerAll()
        }

        private fun populate(groupConfig: GroupConfig) {
            groupConfig.agentGroupConfig.recordingOptions.setRetransformationType(RetransformationType.STARTUP)
            groupConfig.agentGroupConfig.transactionSettings.retransformationType = RetransformationType.ALWAYS

            val pojo = MatchedTransactionDef()
            pojo.declaringClassName = "com.example.Foo"
            pojo.methodName = "bar"
            groupConfig.agentGroupConfig.transactionSettings.transactionDefs.add(pojo)

            val mbean = MBeanTelemetryConfig("heap", TelemetryUnit.PLAIN, 0, true, false)
            mbean.lines.add(MBeanLineConfig("java.lang:type=Memory", "HeapMemoryUsage/used", "used"))
            groupConfig.agentGroupConfig.telemetrySettings.mbeanTelemetries.add(mbean)
        }

        private fun assertPopulated(gcBack: GroupConfig) {
            assertEquals(
                RetransformationType.ALWAYS,
                gcBack.agentGroupConfig.transactionSettings.retransformationType,
                "the non-default TransactionSettings.retransformationType must survive the round-trip",
            )

            assertEquals(
                2,
                gcBack.agentGroupConfig.transactionSettings.transactionDefs.size,
                "default DeclaredTransactionDef plus the added MatchedTransactionDef must survive",
            )
            assertInstanceOf(MatchedTransactionDef::class.java, gcBack.agentGroupConfig.transactionSettings.transactionDefs[1])
            val pojoBack = gcBack.agentGroupConfig.transactionSettings.transactionDefs[1] as MatchedTransactionDef
            assertEquals("com.example.Foo", pojoBack.declaringClassName)
            assertEquals("bar", pojoBack.methodName)

            assertEquals(1, gcBack.agentGroupConfig.telemetrySettings.mbeanTelemetries.size)
            assertEquals("heap", gcBack.agentGroupConfig.telemetrySettings.mbeanTelemetries[0].name)
            assertEquals(1, gcBack.agentGroupConfig.telemetrySettings.mbeanTelemetries[0].lines.size)
        }
    }
}
