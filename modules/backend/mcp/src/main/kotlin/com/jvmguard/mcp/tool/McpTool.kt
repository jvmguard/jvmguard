package com.jvmguard.mcp.tool

import com.jvmguard.mcp.McpToolContext
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification
import io.modelcontextprotocol.spec.McpSchema.*

abstract class McpTool(protected val ctx: McpToolContext) {

    abstract fun createSpecification(): SyncToolSpecification

    protected fun readOnly(title: String): ToolAnnotations =
        ToolAnnotations.builder().title(title).readOnlyHint(true).idempotentHint(true).build()

    protected fun action(title: String): ToolAnnotations =
        ToolAnnotations.builder().title(title).readOnlyHint(false).destructiveHint(false).idempotentHint(false).build()

    protected fun objectSchema(
        properties: Map<String, Map<String, Any>>,
        required: List<String> = emptyList(),
    ): Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to properties,
        "required" to required,
        "additionalProperties" to false,
    )

    protected fun stringProperty(description: String, enumValues: List<String>? = null): Map<String, Any> =
        buildMap {
            put("type", "string")
            put("description", description)
            enumValues?.let { put("enum", it) }
        }

    protected fun integerProperty(description: String): Map<String, Any> =
        mapOf("type" to "integer", "description" to description)

    protected fun booleanProperty(description: String): Map<String, Any> =
        mapOf("type" to "boolean", "description" to description)

    protected fun jsonResult(json: String): CallToolResult =
        CallToolResult.builder()
            .content(listOf(TextContent.builder(json).build()))
            .build()

    protected fun textResult(text: String): CallToolResult =
        CallToolResult.builder()
            .content(listOf(TextContent.builder(text).build()))
            .build()

    protected fun errorResult(message: String): CallToolResult =
        CallToolResult.builder()
            .isError(true)
            .content(listOf(TextContent.builder(message).build()))
            .build()

    protected fun handleError(e: Throwable): CallToolResult =
        errorResult(e.message ?: e.javaClass.simpleName)
}
