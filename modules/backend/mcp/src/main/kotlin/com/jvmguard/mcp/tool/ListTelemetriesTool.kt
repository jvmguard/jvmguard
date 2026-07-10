package com.jvmguard.mcp.tool

import com.jvmguard.mcp.McpJson
import com.jvmguard.mcp.McpToolContext
import io.modelcontextprotocol.spec.McpSchema.Tool
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification

class ListTelemetriesTool(ctx: McpToolContext) : McpTool(ctx) {

    companion object {
        const val NAME = "list_telemetries"
    }

    override fun createSpecification(): SyncToolSpecification {
        val tool = Tool.builder(
            NAME,
            objectSchema(emptyMap()),
        ).description(
            "List all available telemetry streams (e.g. cpu, heap, gc, threads, connections, transactions). " +
                    "Use the returned telemetry name in get_telemetry."
        ).annotations(readOnly("List telemetries")).build()
        return SyncToolSpecification(tool) { _, _ ->
            ctx.withConnection { conn ->
                val items = conn.idToTelemetryType.entries.map { (name, type) ->
                    mapOf(
                        "name" to name,
                        "category" to type.categoryName,
                        "displayName" to type.name,
                        "unit" to type.unit.name,
                    )
                }
                jsonResult(McpJson.write(items))
            }
        }
    }
}
