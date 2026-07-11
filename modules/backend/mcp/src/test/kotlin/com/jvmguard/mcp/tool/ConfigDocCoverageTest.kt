package com.jvmguard.mcp.tool

import com.jvmguard.agent.comm.CodecRegistry
import com.jvmguard.agent.comm.CodecTypes
import com.jvmguard.agent.config.base.ConfigDoc
import com.jvmguard.agent.config.transactions.TransactionDef
import com.jvmguard.data.config.triggers.TriggerType
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Strict guard: every JSON key a group-config bean actually serializes must carry a [ConfigDoc]. Because the codec
 * writes a positional protocol (all keys, always) and Jackson serializes all fields, a fresh instance already
 * exposes the complete key set — so a new undocumented field fails the build.
 */
class ConfigDocCoverageTest {

    private val editableBeans = GroupConfigSchema.editableBeans

    @Test
    fun everySerializedKeyIsDocumented() {
        val problems = StringBuilder()
        var totalKeys = 0
        for (bean in editableBeans) {
            val keys = bean.serializedKeys()
            totalKeys += keys.size
            val missing = keys - documentedKeys(bean.type)
            if (missing.isNotEmpty()) {
                problems.append("${bean.type.simpleName}: undocumented keys ${missing.sorted()}\n")
            }
        }
        // Guard against a vacuous pass if serialization ever stopped emitting keys.
        assertTrue(totalKeys > 80, "expected many serialized keys, got $totalKeys — serialization may be broken")
        assertTrue(problems.isEmpty()) { "Add @ConfigDoc for these serialized config keys:\n$problems" }
    }

    @Test
    fun everyPolymorphicVariantIsCovered() {
        CodecTypes.registerAll()
        val covered: Set<String> = editableBeans.map { it.type.name }.toSet()

        val uncoveredTx = CodecRegistry.registeredPrototypes()
            .filter { TransactionDef::class.java.isAssignableFrom(it.javaClass) }
            .map { it.javaClass.name }
            .filterNot { it in covered }
        assertTrue(uncoveredTx.isEmpty(), "New TransactionDef subtype(s) not covered by ConfigDocCoverageTest: $uncoveredTx")

        val uncoveredTriggers = TriggerType.entries.map { it.createTrigger().javaClass.name }.filterNot { it in covered }
        assertTrue(uncoveredTriggers.isEmpty(), "New Trigger subtype(s) not covered by ConfigDocCoverageTest: $uncoveredTriggers")
    }

    private fun documentedKeys(clazz: Class<*>): Set<String> {
        val keys = HashSet<String>()
        var current: Class<*>? = clazz
        while (current != null && current != Any::class.java) {
            for (field in current.declaredFields) {
                field.getAnnotation(ConfigDoc::class.java) ?: continue
                keys.add(field.name)
            }
            current = current.superclass
        }
        return keys
    }
}
