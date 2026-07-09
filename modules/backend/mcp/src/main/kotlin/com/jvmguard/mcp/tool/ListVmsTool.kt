package com.jvmguard.mcp.tool

import com.jvmguard.agent.config.VmType
import com.jvmguard.mcp.McpJson
import com.jvmguard.mcp.McpToolContext
import io.modelcontextprotocol.spec.McpSchema.Tool
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification

class ListVmsTool(ctx: McpToolContext) : McpTool(ctx) {

    companion object {
        const val NAME = "list_vms"
    }

    override fun createSpecification(): SyncToolSpecification {
        val tool = Tool.builder(
            NAME,
            objectSchema(
                mapOf(
                    "connected" to booleanProperty(
                        "If true, only return currently connected VMs. Default: false (all known VMs)."
                    ),
                ),
            ),
        ).description(
            "List monitored VMs and VM pools with their hierarchy path, host, port, and connection status. " +
                    "The 'kind' field is 'vm' for a single VM, 'pool' for a pool that aggregates several " +
                    "identical VMs, or 'instance' for one connected member of a pool. Reference an entry by its " +
                    "path in other tools; a pool aggregates its members, so reads such as get_telemetry work on " +
                    "the pool path. Pool members share a path, so use connected=true to see individual instances."
        ).annotations(readOnly("List VMs")).build()
        return SyncToolSpecification(tool) { _, request ->
            try {
                val connected = (request.arguments()["connected"] as? Boolean) ?: false
                ctx.withConnection { conn ->
                    val connectedPaths = conn.connectedVms.map { it.hierarchyPath.trimEnd('/') }.toSet()
                    val vms = (if (connected) conn.connectedVms else conn.namedVms)
                        .filter { it.type != VmType.GROUP }
                        .map { vm ->
                            mapOf(
                                "path" to vm.hierarchyPath.trimEnd('/'),
                                "name" to vm.name,
                                "kind" to when (vm.type) {
                                    VmType.POOL -> "pool"
                                    VmType.POOLED -> "instance"
                                    else -> "vm"
                                },
                                "host" to vm.hostName,
                                "port" to vm.port,
                                "connected" to (vm.hierarchyPath.trimEnd('/') in connectedPaths),
                            )
                        }
                    jsonResult(McpJson.write(vms))
                }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }
}
