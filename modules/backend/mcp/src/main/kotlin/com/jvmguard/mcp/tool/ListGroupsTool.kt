package com.jvmguard.mcp.tool

import com.jvmguard.agent.config.VmType
import com.jvmguard.data.config.GroupConfig
import com.jvmguard.mcp.McpJson
import com.jvmguard.mcp.McpToolContext
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification
import io.modelcontextprotocol.spec.McpSchema.Tool

class ListGroupsTool(ctx: McpToolContext) : McpTool(ctx) {

    companion object {
        const val NAME = "list_groups"
    }

    override fun createSpecification(): SyncToolSpecification {
        val tool = Tool.builder(
            NAME,
            objectSchema(emptyMap()),
        ).description(
            "List all monitored groups and pools with their hierarchy paths. " +
                    "Use these paths in list_vms and other tools to target specific groups."
        ).annotations(readOnly("List groups")).build()
        return SyncToolSpecification(tool) { _, _ ->
            try {
                ctx.withConnection { conn ->
                    // dedup
                    val byPath = LinkedHashMap<String, GroupConfig>()
                    for (config in conn.groupConfigs) {
                        val existing = byPath[config.hierarchyPath]
                        if (existing == null || config.groupType == VmType.POOL) {
                            byPath[config.hierarchyPath] = config
                        }
                    }
                    val groups = byPath.values.map { config ->
                        mapOf(
                            "path" to config.hierarchyPath.ifEmpty { "<root>" },
                            "type" to config.groupType.name,
                            "isRoot" to config.isRoot,
                        )
                    }
                    jsonResult(McpJson.write(groups))
                }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }
}
