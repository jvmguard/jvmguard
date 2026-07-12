package com.jvmguard.data.config.external

import com.grack.nanojson.JsonObject
import com.grack.nanojson.JsonParser
import com.jvmguard.agent.comm.CodecEntity
import com.jvmguard.agent.comm.JsonAgentWriter
import com.jvmguard.common.config.ConfigStorage

object ConfigDocKeys {

    fun codecKeys(bean: CodecEntity): Set<String> {
        val root = JsonObject()
        bean.writeState(JsonAgentWriter(root))
        return root.keys.toSet()
    }

    fun jacksonKeys(bean: Any): Set<String> =
        keysOf(JsonParser.any().from(ConfigStorage.objectMapper().writeValueAsString(bean)))

    // "@type" is the polymorphic discriminator
    private fun keysOf(node: Any?): Set<String> =
        if (node is JsonObject) node.keys.toSet() - "@type" else emptySet()
}
