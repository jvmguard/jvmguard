package com.jvmguard.mcp.tool

import com.jvmguard.mcp.McpJson
import java.security.MessageDigest

internal object McpAuditDetail {

    private const val MAX_CHARS = 512

    fun cap(value: Any?): Any? {
        if (value == null) return null
        val json = McpJson.write(value)
        if (json.length <= MAX_CHARS) return value
        return linkedMapOf(
            "truncated" to true,
            "length" to json.length,
            "sha256" to sha256(json),
            "preview" to json.take(MAX_CHARS),
        )
    }

    fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
}
