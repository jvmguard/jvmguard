package com.jvmguard.common.config

import com.fasterxml.jackson.annotation.JsonFormat
import com.grack.nanojson.JsonObject
import com.jvmguard.agent.comm.CodecEntity
import com.jvmguard.agent.comm.CodecRegistry
import com.jvmguard.agent.comm.JsonAgentReader
import com.jvmguard.agent.comm.JsonAgentWriter
import tools.jackson.core.JsonGenerator
import tools.jackson.core.JsonParser
import tools.jackson.databind.*
import tools.jackson.databind.deser.Deserializers
import tools.jackson.databind.module.SimpleModule
import tools.jackson.databind.ser.Serializers
import com.grack.nanojson.JsonParser as NanoJsonParser

/**
 * Bridges agent CodecEntity beans  through the agent codec when they are nested inside a Jackson-serialized
 * server bean.
 */
class CodecEntityJacksonModule : SimpleModule("CodecEntityBridge") {
    override fun setupModule(context: SetupContext) {
        super.setupModule(context)
        context.addSerializers(CodecEntitySerializers)
        context.addDeserializers(CodecEntityDeserializers)
    }
}

private object CodecEntitySerializer : ValueSerializer<CodecEntity>() {
    override fun serialize(value: CodecEntity, gen: JsonGenerator, ctxt: SerializationContext) {
        val json = JsonObject()
        json.put("@type", value.codecType())
        try {
            value.writeState(JsonAgentWriter(json))
        } catch (e: Exception) {
            throw RuntimeException("could not serialize codec bean ${value.codecType()}", e)
        }
        // JsonObject/JsonArray are Map/List, so this emits a plain JSON object with no type wrapper
        ctxt.writeValue(gen, json)
    }
}

private object CodecEntityDeserializer : ValueDeserializer<CodecEntity>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): CodecEntity {
        val json = NanoJsonParser.`object`().from(ctxt.readTree(p).toString())
        val type = json.getString("@type", null)
        val bean = CodecRegistry.create(type)
        try {
            bean.readState(JsonAgentReader(json))
        } catch (e: Exception) {
            throw RuntimeException("could not deserialize codec bean $type", e)
        }
        return bean
    }
}

private object CodecEntitySerializers : Serializers.Base() {
    override fun findSerializer(
        config: SerializationConfig,
        type: JavaType,
        beanDesc: BeanDescription.Supplier,
        formatOverrides: JsonFormat.Value?,
    ): ValueSerializer<*>? =
        if (CodecEntity::class.java.isAssignableFrom(type.rawClass)) CodecEntitySerializer else null
}

private object CodecEntityDeserializers : Deserializers.Base() {
    override fun hasDeserializerFor(config: DeserializationConfig, valueType: Class<*>): Boolean =
        CodecEntity::class.java.isAssignableFrom(valueType)

    override fun findBeanDeserializer(
        type: JavaType,
        config: DeserializationConfig,
        beanDesc: BeanDescription.Supplier,
    ): ValueDeserializer<*>? =
        if (CodecEntity::class.java.isAssignableFrom(type.rawClass)) CodecEntityDeserializer else null
}
