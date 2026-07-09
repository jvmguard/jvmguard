package com.jvmguard.mcp.tool

import com.jvmguard.agent.config.VmType
import com.jvmguard.connector.api.ServerConnection
import com.jvmguard.data.vmdata.VM
import com.jvmguard.mcp.McpError

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
}
