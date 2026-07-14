package dev.jvmguard.mcp.tool

import dev.jvmguard.agent.config.VmType
import dev.jvmguard.connector.api.ServerConnection
import dev.jvmguard.data.vmdata.VM
import dev.jvmguard.mcp.McpError

object VmResolver {

    fun resolveVm(connection: ServerConnection, hierarchyPath: String): VM {
        // Pool members have a trailing slash which needs to be normalized.
        val normalized = hierarchyPath.trimEnd('/')
        return connection.namedVms
            .filter { it.type == VmType.NAMED || it.type == VmType.POOL }
            .find { it.hierarchyPath.trimEnd('/') == normalized }
            ?: throw McpError("VM not found: $hierarchyPath")
    }

    fun resolveVmOrNull(connection: ServerConnection, hierarchyPath: String?): VM? {
        if (hierarchyPath.isNullOrBlank()) return null
        return resolveVm(connection, hierarchyPath)
    }

    fun resolveLiveVm(connection: ServerConnection, hierarchyPath: String): VM {
        val vm = resolveVm(connection, hierarchyPath)
        if (vm.type != VmType.POOL) {
            return vm
        }
        return connection.getConnectedPooledVms(vm).firstOrNull()
            ?: throw McpError(
                "$hierarchyPath is a pool with no connected members. " +
                    "Call list_vms with connected=true to see live instances, or target a single VM."
            )
    }
}
