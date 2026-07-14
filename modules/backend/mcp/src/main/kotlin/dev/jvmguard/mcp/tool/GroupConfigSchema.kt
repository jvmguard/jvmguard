package dev.jvmguard.mcp.tool

import dev.jvmguard.agent.comm.CodecEntity
import dev.jvmguard.agent.comm.CodecRegistry
import dev.jvmguard.agent.comm.CodecTypes
import dev.jvmguard.agent.config.VmType
import dev.jvmguard.agent.config.base.ConfigDoc
import dev.jvmguard.agent.config.recording.RecordingOptions
import dev.jvmguard.agent.config.transactions.ComparisonType
import dev.jvmguard.agent.config.telemetry.MBeanLineConfig
import dev.jvmguard.agent.config.telemetry.MBeanTelemetryConfig
import dev.jvmguard.agent.config.telemetry.TelemetrySettings
import dev.jvmguard.agent.config.transactions.DeclaredTransactionDef
import dev.jvmguard.agent.config.transactions.MappedTransactionDef
import dev.jvmguard.agent.config.transactions.MatchedTransactionDef
import dev.jvmguard.agent.config.transactions.Policy
import dev.jvmguard.agent.config.transactions.PolicySubDef
import dev.jvmguard.agent.config.transactions.TransactionDef
import dev.jvmguard.agent.config.transactions.TransactionNaming
import dev.jvmguard.agent.config.transactions.TransactionSettings
import dev.jvmguard.agent.config.transactions.naming.ClassNameElement
import dev.jvmguard.agent.config.transactions.naming.InstanceClassNameElement
import dev.jvmguard.agent.config.transactions.naming.InstanceElement
import dev.jvmguard.agent.config.transactions.naming.MethodNameElement
import dev.jvmguard.agent.config.transactions.naming.MethodParameterElement
import dev.jvmguard.agent.config.transactions.naming.TextElement
import dev.jvmguard.data.config.GroupConfig
import dev.jvmguard.data.config.external.ConfigDocKeys
import dev.jvmguard.data.config.external.RecordingConfig
import dev.jvmguard.data.config.thresholds.Threshold
import dev.jvmguard.data.config.thresholds.ThresholdSettings
import dev.jvmguard.data.config.triggers.ConnectionTrigger
import dev.jvmguard.data.config.triggers.PolicyTrigger
import dev.jvmguard.data.config.triggers.ThresholdTrigger
import dev.jvmguard.data.config.triggers.Trigger
import dev.jvmguard.data.config.triggers.TriggerSettings
import dev.jvmguard.data.config.triggers.TriggerType
import dev.jvmguard.data.config.triggers.actions.ActionType
import dev.jvmguard.data.config.triggers.actions.EmailAction
import dev.jvmguard.data.config.triggers.actions.HeapDumpAction
import dev.jvmguard.data.config.triggers.actions.InboxAction
import dev.jvmguard.data.config.triggers.actions.LogAction
import dev.jvmguard.data.config.triggers.actions.RecordJfrAction
import dev.jvmguard.data.config.triggers.actions.RecordJpsAction
import dev.jvmguard.data.config.triggers.actions.ThreadDumpAction
import dev.jvmguard.data.config.triggers.actions.TriggerAction
import dev.jvmguard.data.config.triggers.actions.WebhookAction
import dev.jvmguard.data.vmdata.PersistentTelemetryIdentifier
import dev.jvmguard.data.vmdata.ThresholdIdentifier
import dev.jvmguard.data.vmdata.VmIdentifier
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
                if (fields.isEmpty()) {
                    null
                } else buildMap<String, Any?> {
                    put("type", bean.type.simpleName)
                    // serverConfig beans are written as [className, {...}] tuples, so the agent needs the FQN
                    if (!bean.codec) put("className", bean.type.name)
                    put("fields", fields)
                }
            },
        )
        put("transactionTypes", transactionVariants())
        put("triggerTypes", triggerVariants())
        put("actionTypes", actionVariants())
        put("example", exampleConfigJson())
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
                put("className", triggerClass.name)
                classDoc(triggerClass)?.let { put("description", it) }
            }
        }

    private fun actionVariants(): List<Map<String, Any?>> =
        ActionType.entries.map { actionType ->
            val actionClass = actionType.createAction().javaClass
            buildMap {
                put("type", actionType.name)
                put("name", actionType.toString())
                put("class", actionClass.simpleName)
                put("className", actionClass.name)
                classDoc(actionClass)?.let { put("description", it) }
            }
        }

    private fun exampleConfigJson(): String {
        val group = GroupConfig.createDefault(VmIdentifier("Example/Group", VmType.GROUP))
        val server = group.serverGroupConfig
        val actions: MutableList<TriggerAction> = ActionType.entries.mapTo(ArrayList()) { it.createAction() }
        actions.forEachIndexed { index, action -> action.id = (index + 1).toLong() }
        val policyTrigger = PolicyTrigger().apply {
            filter = "*"
            comparisonType = ComparisonType.WILDCARD
            isVerySlow = true
            isError = true
            interval = Trigger.Interval.MINUTE
            count = 5
            inhibitionInterval = Trigger.Interval.HOUR
            inhibitionTime = 1
            triggerActions = actions
        }
        // Leaving the trigger ids null lets the setter assign them and advance lastId, so the example shows a
        // consistent id/lastId pairing to copy.
        server.triggerSettings.triggers = arrayListOf<Trigger>(policyTrigger, ConnectionTrigger(), ThresholdTrigger())
        server.thresholdSettings.thresholds = arrayListOf(Threshold().apply { id = 1L })
        return RecordingConfig.groupToJsonString(group, includeGuardrails = false)
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
                "entry in 'sections': it is a nested object whose fields are documented under that entry. " +
                "In serverConfig every object is a two element array of the form [className, fields] where className " +
                "is the fully qualified class name given as 'className' in the matching 'sections' entry and, for the " +
                "polymorphic 'triggers' and 'triggerActions' lists, in 'triggerTypes' and 'actionTypes'. The " +
                "'example' field is a complete populated group config in the same format as 'config', so you can copy " +
                "a trigger, action or threshold from it, keep its enum encoding, list wrappers and id numbering, and " +
                "change only the values. Give each new element a unique 'id' as shown there and keep the enclosing " +
                "'lastId' at least as high as the largest id you use."
}
