package com.jvmguard.mcp.tool

import com.jvmguard.agent.config.VmType
import com.jvmguard.common.helper.ListModification
import com.jvmguard.data.config.GroupConfig
import com.jvmguard.data.config.external.RecordingConfig
import com.jvmguard.data.vmdata.VmIdentifier
import com.jvmguard.mcp.McpError
import com.jvmguard.mcp.McpJson
import com.jvmguard.mcp.McpToolContext
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification
import io.modelcontextprotocol.spec.McpSchema.Tool

private fun groupIdentifier(path: String): VmIdentifier = VmIdentifier(path, VmType.GROUP)

class GetGroupConfigTool(ctx: McpToolContext) : McpTool(ctx) {

    companion object {
        const val NAME = "get_group_config"
    }

    override fun createSpecification(): SyncToolSpecification {
        val tool = Tool.builder(
            NAME,
            objectSchema(
                mapOf(
                    "group" to stringProperty("VM group hierarchy path (from list_groups). Use \"\" for the root group."),
                    "includeSchema" to booleanProperty(
                        "If true (default), also return a schema that explains the config fields, enums and " +
                                "polymorphic types. Set false on repeat reads to save tokens."
                    ),
                ),
                listOf("group"),
            ),
        ).description(
            "Read the recording configuration (recording options, transactions, telemetries, thresholds, triggers) " +
                    "of a VM group as a JSON string. Edit that string and pass it back to set_group_config. The " +
                    "agent-guardrail settings are intentionally omitted."
        ).annotations(readOnly("Get group config")).build()
        return SyncToolSpecification(tool) { _, request ->
            val args = request.arguments()
            val group = args["group"] as String
            val includeSchema = (args["includeSchema"] as? Boolean) ?: true
            ctx.withConnection { conn ->
                val identifier = groupIdentifier(group)
                val existing = conn.groupConfigs.firstOrNull { it.groupIdentifier == identifier }
                    ?: throw McpError("No VM group \"$group\". Use list_groups to see the group paths.")
                jsonResult(
                    McpJson.write(
                        buildMap {
                            put("group", group)
                            put("config", RecordingConfig.groupToJsonString(existing, includeGuardrails = false))
                            if (includeSchema) put("schema", GroupConfigSchema.reference())
                        }
                    )
                )
            }
        }
    }
}

class SetGroupConfigTool(ctx: McpToolContext) : McpTool(ctx) {

    companion object {
        const val NAME = "set_group_config"
    }

    override fun createSpecification(): SyncToolSpecification {
        val tool = Tool.builder(
            NAME,
            objectSchema(
                mapOf(
                    "group" to stringProperty("VM group hierarchy path to write. Use \"\" for the root group."),
                    "config" to stringProperty(
                        "The full group config as a JSON string, in the shape returned by get_group_config. Edit the " +
                                "value from get_group_config; keep every type-discriminator field intact."
                    ),
                ),
                listOf("group", "config"),
            ),
        ).description(
            "Replace a VM group's recording configuration with the given JSON (same format as get_group_config / the " +
                    "config export). Requires profiler access. The group's identity and its agent-guardrail settings " +
                    "are preserved regardless of the submitted JSON."
        ).annotations(action("Set group config")).build()
        return SyncToolSpecification(tool) { _, request ->
            val args = request.arguments()
            val group = args["group"] as String
            val configJson = args["config"] as String
            val identifier = groupIdentifier(group)
            ctx.requireConfigEditAllowed(identifier)
            ctx.withConnection { conn ->
                val existing = conn.groupConfigs.firstOrNull { it.groupIdentifier == identifier }
                    ?: throw McpError("No VM group \"$group\". Use list_groups to see the group paths.")
                val parsed = try {
                    RecordingConfig.groupFromJsonString(configJson)
                } catch (e: Exception) {
                    throw McpError("Could not parse the config JSON: ${e.message}")
                }
                // Force the target group's identity and preserve its guardrail settings.
                val updated = GroupConfig(existing.groupIdentifier, parsed.agentGroupConfig, parsed.serverGroupConfig)
                updated.id = existing.id
                updated.guardrailSettings = existing.guardrailSettings
                val priorHash = McpAuditDetail.sha256(RecordingConfig.groupToJsonString(existing, includeGuardrails = false))
                val newHash = McpAuditDetail.sha256(RecordingConfig.groupToJsonString(updated, includeGuardrails = false))
                conn.modifyGroupConfigs(ListModification(listOf(updated), emptyList(), emptyList(), GroupConfig::class.java))
                ctx.recordAuditDetail(
                    mapOf(
                        "group" to group,
                        "priorConfigSha256" to priorHash,
                        "newConfigSha256" to newHash,
                        "changed" to (priorHash != newHash),
                    )
                )
                jsonResult(McpJson.write(mapOf("status" to "ok", "group" to group)))
            }
        }
    }
}
