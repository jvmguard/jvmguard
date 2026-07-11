package com.jvmguard.common.config

import com.jvmguard.agent.config.VmType
import com.jvmguard.data.config.GlobalConfig
import com.jvmguard.data.config.GroupConfig
import com.jvmguard.data.config.sets.TriggerSet
import com.jvmguard.data.config.triggers.ThresholdTrigger
import com.jvmguard.data.user.AccessLevel
import com.jvmguard.data.user.User
import com.jvmguard.data.vmdata.VmIdentifier
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper

class ConfigStorageRoundTripTest {

    private val mapper: ObjectMapper = ConfigStorage.objectMapper()

    @Test
    fun userRoundTrips() {
        val user = User("login", "Full Name", "hash", "e@mail", AccessLevel.ADMIN)
        val back = roundTrip(user, User::class.java)
        assertEquals("login", back.loginName)
        assertEquals("Full Name", back.fullName)
        assertEquals(AccessLevel.ADMIN, back.accessLevel)
    }

    @Test
    fun inheritedModifiedFlagIsIgnored() {
        val user = User("login", "Full Name", "hash", "e@mail", AccessLevel.ADMIN)
        user.modified()
        val json = mapper.writeValueAsString(user)
        assertFalse(json.contains("\"modified\""), "the runtime dirty flag must not be persisted")
    }

    @Test
    fun globalConfigRoundTrips() {
        val back = roundTrip(GlobalConfig(), GlobalConfig::class.java)
        assertNotNull(back)
        assertNotNull(back.smtpConfig)
    }

    @Test
    fun guardrailConfigRoundTrips() {
        val config = GlobalConfig()
        config.guardrailConfig.mcpReadOnly = true
        config.guardrailConfig.apiAllowedIps = "10.0.0.0/8, ::1"

        val back = roundTrip(config, GlobalConfig::class.java)

        assertTrue(back.guardrailConfig.mcpReadOnly)
        assertEquals("10.0.0.0/8, ::1", back.guardrailConfig.apiAllowedIps)
    }

    @Test
    fun groupConfigRoundTripsNestedAgentBeans() {
        val gc = GroupConfig.createDefault()
        val back = roundTrip(gc, GroupConfig::class.java)
        assertNotNull(back.agentGroupConfig)
        assertNotNull(back.agentGroupConfig.recordingOptions)
        assertNotNull(back.agentGroupConfig.transactionSettings)
    }

    @Test
    fun groupGuardrailSettingsRoundTrip() {
        val gc = GroupConfig.createDefault()
        gc.guardrailSettings.isUsed = true
        gc.guardrailSettings.allowHeapDump = false
        gc.guardrailSettings.allowRunGc = false
        gc.guardrailSettings.allowMbeanMutations = false
        gc.guardrailSettings.maxRecordingSeconds = 120

        val back = roundTrip(gc, GroupConfig::class.java)

        assertTrue(back.guardrailSettings.isUsed)
        assertFalse(back.guardrailSettings.allowHeapDump)
        assertFalse(back.guardrailSettings.allowRunGc)
        assertFalse(back.guardrailSettings.allowMbeanMutations)
        assertEquals(120, back.guardrailSettings.maxRecordingSeconds)
        assertTrue(back.guardrailSettings.allowJps, "unset toggles keep their permissive default")
    }

    @Test
    fun polymorphicTriggerSetRoundTrips() {
        val set = TriggerSet()
        set.name = "ts"
        set.items.add(ThresholdTrigger())
        val json = mapper.writeValueAsString(set)
        assertTrue(json.contains("ThresholdTrigger"), "default typing must embed the concrete element type for the polymorphic Trigger list")
        val back = mapper.readValue(json, TriggerSet::class.java)
        assertEquals("ts", back.name)
        assertEquals(1, back.items.size)
        assertInstanceOf(ThresholdTrigger::class.java, back.items.first())
    }

    @Test
    fun poolGroupConfigIdentityRoundTrips() {
        val gc = GroupConfig.createDefault(VmIdentifier("Demo/Storefront", VmType.POOL))
        val back = roundTrip(gc, GroupConfig::class.java)
        assertEquals("Demo/Storefront", back.hierarchyPath, "hierarchyPath must survive the round trip")
        assertEquals(VmType.POOL, back.groupType, "groupType must survive so a pool is not reclassified as GROUP")
        assertEquals(gc.groupIdentifier, back.groupIdentifier, "the group identifier must be stable across reload")
    }

    private fun <T> roundTrip(bean: T, type: Class<T>): T =
        mapper.readValue(mapper.writeValueAsString(bean), type)
}
