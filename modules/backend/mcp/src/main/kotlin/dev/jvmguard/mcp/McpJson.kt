package dev.jvmguard.mcp

import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper

object McpJson {

    private val mapper: ObjectMapper = JsonMapper.builder().build()

    fun write(value: Any): String = mapper.writeValueAsString(value)

}
