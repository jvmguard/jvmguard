package dev.jvmguard.common.config

import dev.jvmguard.agent.comm.CodecTypes
import dev.jvmguard.agent.config.VmType
import dev.jvmguard.agent.config.transactions.DeclaredTransactionDef
import dev.jvmguard.data.config.GlobalConfig
import dev.jvmguard.data.config.GroupConfig
import dev.jvmguard.data.config.sets.ActionSet
import dev.jvmguard.data.config.sets.TransactionDefSet
import dev.jvmguard.data.config.sets.TriggerSet
import dev.jvmguard.data.config.triggers.ThresholdTrigger
import dev.jvmguard.data.config.triggers.actions.WebhookAction
import dev.jvmguard.data.user.AccessLevel
import dev.jvmguard.data.user.User
import dev.jvmguard.data.vmdata.VmIdentifier
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper

class ConfigStorageRoundTripTest {

    private val mapper: ObjectMapper = ConfigStorage.objectMapper()

    companion object {
        @BeforeAll
        @JvmStatic
        fun registerCodecTypes() = CodecTypes.registerAll()
    }

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
        gc.guardrailSettings.allowMbeanMutations = false
        gc.guardrailSettings.allowConfigEdit = false
        gc.guardrailSettings.maxRecordingSeconds = 120

        val back = roundTrip(gc, GroupConfig::class.java)

        assertTrue(back.guardrailSettings.isUsed)
        assertFalse(back.guardrailSettings.allowHeapDump)
        assertFalse(back.guardrailSettings.allowMbeanMutations)
        assertFalse(back.guardrailSettings.allowConfigEdit)
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
    fun polymorphicActionSetRoundTrips() {
        val set = ActionSet()
        set.items.add(WebhookAction())
        val json = mapper.writeValueAsString(set)
        assertTrue(json.contains("\"@type\":\"WebhookAction\""), "TriggerAction polymorphism uses a short @type name")
        val back = mapper.readValue(json, ActionSet::class.java)
        assertEquals(1, back.items.size)
        assertInstanceOf(WebhookAction::class.java, back.items.first())
    }

    @Test
    fun codecBridgedTransactionDefSetRoundTrips() {
        val set = TransactionDefSet()
        set.items.add(DeclaredTransactionDef())
        val json = mapper.writeValueAsString(set)
        assertTrue(json.contains("\"@type\":\"DeclaredTransactionDef\""), "the nested agent bean is serialized through the codec bridge")
        assertFalse(json.contains("dev.jvmguard"), "no fully-qualified class names from the bridged agent bean")
        val back = mapper.readValue(json, TransactionDefSet::class.java)
        assertEquals(1, back.items.size)
        assertInstanceOf(DeclaredTransactionDef::class.java, back.items.first())
    }

    @Test
    fun poolGroupConfigIdentityRoundTrips() {
        val gc = GroupConfig.createDefault(VmIdentifier("Demo/Storefront", VmType.POOL))
        val back = roundTrip(gc, GroupConfig::class.java)
        assertEquals("Demo/Storefront", back.hierarchyPath, "hierarchyPath must survive the round trip")
        assertEquals(VmType.POOL, back.groupType, "groupType must survive so a pool is not reclassified as GROUP")
        assertEquals(gc.groupIdentifier, back.groupIdentifier, "the group identifier must be stable across reload")
    }

    @Test
    fun serializedShapeIsCompactAndStable() {
        val gc = GroupConfig.createDefault()
        gc.triggerSettings.triggers.add(ThresholdTrigger())
        val json = mapper.writeValueAsString(gc)

        // Polymorphism is a compact inline discriminator, on both the server and the agent side.
        assertTrue(json.contains("\"@type\":\"ThresholdTrigger\""), "server polymorphism uses a short @type name")
        assertTrue(json.contains("\"@type\":\"DeclaredTransactionDef\""), "agent beans keep their codec @type name")
        // No blanket default typing: no fully-qualified class names, no wrapper arrays around monomorphic values.
        assertFalse(json.contains("dev.jvmguard"), "no fully-qualified class names embedded in the config")
        assertFalse(json.contains("java.util.ArrayList"), "collections are not type-tagged")
        assertFalse(json.contains("\"id\":null"), "null ids are omitted")
    }

    private fun <T> roundTrip(bean: T, type: Class<T>): T =
        mapper.readValue(mapper.writeValueAsString(bean), type)
}
