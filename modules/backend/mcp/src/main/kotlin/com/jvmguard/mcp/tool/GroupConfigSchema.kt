package com.jvmguard.mcp.tool

import com.jvmguard.agent.comm.CodecEntity
import com.jvmguard.agent.comm.CodecRegistry
import com.jvmguard.agent.comm.CodecTypes
import com.jvmguard.agent.config.base.ConfigDoc
import com.jvmguard.agent.config.recording.RecordingOptions
import com.jvmguard.agent.config.telemetry.MBeanLineConfig
import com.jvmguard.agent.config.telemetry.MBeanTelemetryConfig
import com.jvmguard.agent.config.telemetry.TelemetrySettings
import com.jvmguard.agent.config.transactions.DeclaredTransactionDef
import com.jvmguard.agent.config.transactions.MappedTransactionDef
import com.jvmguard.agent.config.transactions.MatchedTransactionDef
import com.jvmguard.agent.config.transactions.Policy
import com.jvmguard.agent.config.transactions.PolicySubDef
import com.jvmguard.agent.config.transactions.TransactionDef
import com.jvmguard.agent.config.transactions.TransactionNaming
import com.jvmguard.agent.config.transactions.TransactionSettings
import com.jvmguard.agent.config.transactions.naming.ClassNameElement
import com.jvmguard.agent.config.transactions.naming.InstanceClassNameElement
import com.jvmguard.agent.config.transactions.naming.InstanceElement
import com.jvmguard.agent.config.transactions.naming.MethodNameElement
import com.jvmguard.agent.config.transactions.naming.MethodParameterElement
import com.jvmguard.agent.config.transactions.naming.TextElement
import com.jvmguard.data.config.external.ConfigDocKeys
import com.jvmguard.data.config.thresholds.Threshold
import com.jvmguard.data.config.thresholds.ThresholdSettings
import com.jvmguard.data.config.triggers.ConnectionTrigger
import com.jvmguard.data.config.triggers.PolicyTrigger
import com.jvmguard.data.config.triggers.ThresholdTrigger
import com.jvmguard.data.config.triggers.Trigger
import com.jvmguard.data.config.triggers.TriggerSettings
import com.jvmguard.data.config.triggers.TriggerType
import com.jvmguard.data.config.triggers.actions.EmailAction
import com.jvmguard.data.config.triggers.actions.HeapDumpAction
import com.jvmguard.data.config.triggers.actions.InboxAction
import com.jvmguard.data.config.triggers.actions.LogAction
import com.jvmguard.data.config.triggers.actions.RecordJfrAction
import com.jvmguard.data.config.triggers.actions.RecordJpsAction
import com.jvmguard.data.config.triggers.actions.ThreadDumpAction
import com.jvmguard.data.config.triggers.actions.WebhookAction
import com.jvmguard.data.vmdata.PersistentTelemetryIdentifier
import com.jvmguard.data.vmdata.ThresholdIdentifier
import java.lang.reflect.Field

object GroupConfigSchema {

    class EditableBean(val instance: Any, val codec: Boolean) {
        val type: Class<*> get() = instance.javaClass
        fun serializedKeys(): Set<String> =
            if (codec) ConfigDocKeys.codecKeys(instance as CodecEntity) else ConfigDocKeys.jacksonKeys(instance)
    }

    val editableBeans: List<EditableBean> =
        listOf(
            RecordingOptions(),
            TransactionSettings(),
            MatchedTransactionDef(),
            MappedTransactionDef(),
            DeclaredTransactionDef(),
            Policy(),
            PolicySubDef(),
            TransactionNaming(),
            ClassNameElement(),
            InstanceClassNameElement(),
            InstanceElement(),
            MethodNameElement(),
            MethodParameterElement(),
            TextElement(),
            TelemetrySettings(),
            MBeanTelemetryConfig(),
            MBeanLineConfig()
        ).map { EditableBean(it, true) } +
                listOf(
                    ThresholdSettings(),
                    Threshold(),
                    TriggerSettings(),
                    ConnectionTrigger(),
                    PolicyTrigger(),
                    ThresholdTrigger(),
                    RecordJpsAction(),
                    RecordJfrAction(),
                    ThreadDumpAction(),
                    HeapDumpAction(),
                    EmailAction(),
                    WebhookAction(),
                    LogAction(),
                    InboxAction(),
                    PersistentTelemetryIdentifier(),
                    ThresholdIdentifier(),
                ).map { EditableBean(it, false) }

    private val cached: Map<String, Any?> by lazy { build() }

    fun reference(): Map<String, Any?> = cached

    private fun build(): Map<String, Any?> = buildMap {
        CodecTypes.registerAll()
        put("editingGuide", EDITING_GUIDE)
        put(
            "sections",
            editableBeans.mapNotNull { bean ->
                val fields = docFields(bean.type, bean.serializedKeys())
                if (fields.isEmpty()) null else mapOf("type" to bean.type.simpleName, "fields" to fields)
            },
        )
        put("transactionTypes", transactionVariants())
        put("triggerTypes", triggerVariants())
    }

    private fun docFields(clazz: Class<*>, serializedKeys: Set<String>): List<Map<String, Any?>> {
        val fields = ArrayList<Map<String, Any?>>()
        var current: Class<*>? = clazz
        while (current != null && current != Any::class.java) {
            for (field in current.declaredFields) {
                val doc = field.getAnnotation(ConfigDoc::class.java) ?: continue
                if (field.name in serializedKeys) {
                    fields.add(fieldDoc(field, doc))
                }
            }
            current = current.superclass
        }
        return fields
    }

    private fun fieldDoc(field: Field, doc: ConfigDoc): Map<String, Any?> = buildMap {
        put("key", field.name)
        put("type", typeLabel(field.type))
        put("description", doc.value)
        if (field.type.isEnum) {
            put("values", enumValues(field.type))
        }
    }

    private fun enumValues(enumClass: Class<*>): List<Map<String, Any?>> =
        enumClass.enumConstants.map { constant ->
            val name = (constant as Enum<*>).name
            val doc = enumClass.getField(name).getAnnotation(ConfigDoc::class.java)
            buildMap {
                put("value", name)
                doc?.let { put("description", it.value) }
            }
        }

    private fun typeLabel(type: Class<*>): String = when {
        type.isEnum -> "enum"
        type == Boolean::class.javaPrimitiveType || type == Boolean::class.javaObjectType -> "boolean"
        type == Int::class.javaPrimitiveType || type == Int::class.javaObjectType -> "integer"
        type == Long::class.javaPrimitiveType || type == Long::class.javaObjectType -> "integer"
        type == String::class.java -> "string"
        Collection::class.java.isAssignableFrom(type) -> "array"
        else -> type.simpleName
    }

    private fun transactionVariants(): List<Map<String, Any?>> =
        CodecRegistry.registeredPrototypes()
            .filter { TransactionDef::class.java.isAssignableFrom(it.javaClass) }
            .map { prototype ->
                buildMap {
                    put("type", prototype.codecType())
                    put("class", prototype.javaClass.simpleName)
                    classDoc(prototype.javaClass)?.let { put("description", it) }
                }
            }

    private fun triggerVariants(): List<Map<String, Any?>> =
        TriggerType.entries.map { triggerType ->
            val triggerClass: Class<out Trigger> = triggerType.createTrigger().javaClass
            buildMap {
                put("type", triggerType.name)
                put("name", triggerType.toString())
                put("class", triggerClass.simpleName)
                classDoc(triggerClass)?.let { put("description", it) }
            }
        }

    private fun classDoc(clazz: Class<*>): String? = clazz.getAnnotation(ConfigDoc::class.java)?.value

    private const val EDITING_GUIDE =
        "To change a group's configuration: call get_group_config, edit the returned 'config' JSON string, then " +
                "pass the edited string to set_group_config. The config is a single group entry with 'agentConfig' " +
                "(recording options, transactions, telemetries) and 'serverConfig' (thresholds, triggers). Both are " +
                "polymorphic: keep every type-discriminator field (\"@type\" in agentConfig, the leading class-name " +
                "element of the [type, {...}] arrays in serverConfig) exactly as returned. Add list elements by " +
                "copying the shape of an existing one and changing values. The 'group' path and the agent-guardrail " +
                "settings are fixed by the server and ignored on write. 'sections' documents the fields of each bean " +
                "type, see transactionTypes and triggerTypes for the polymorphic variants and their discriminators. " +
                "A field whose 'type' is not one of enum, boolean, integer, string or array is the name of another " +
                "entry in 'sections': it is a nested object whose fields are documented under that entry."
}
