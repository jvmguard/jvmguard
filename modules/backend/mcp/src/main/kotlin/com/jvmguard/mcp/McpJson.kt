package com.jvmguard.mcp

import com.jvmguard.common.export.base.AbstractExport
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import java.io.ByteArrayOutputStream

object McpJson {

    private val mapper: ObjectMapper = JsonMapper.builder().build()

    fun write(value: Any): String = mapper.writeValueAsString(value)

    fun exportToJson(export: AbstractExport<*>): String {
        val baos = ByteArrayOutputStream()
        export.fileType(AbstractExport.FileType.JSON).pretty(true).export(baos)
        return baos.toString(Charsets.UTF_8)
    }
}
