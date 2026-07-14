package dev.jvmguard.collector.util

import dev.jvmguard.agent.mbean.MBeanListResult
import dev.jvmguard.collector.connection.AgentConnectionImpl
import dev.jvmguard.collector.vmdata.VmData
import dev.jvmguard.data.vmdata.Connection
import dev.jvmguard.data.vmdata.VM
import java.util.*

class CurrentConnectionEntry(
    val groupNames: Array<String>,
    val vm: VM,
    val vmData: VmData,
    val agentConnection: AgentConnectionImpl,
    val connection: Connection,
) {
    val committedDeclaredTelemetryFormats: MutableSet<String> = Collections.synchronizedSet(HashSet())

    private val mbeanGuard = Any()
    private var lastMBeanNames: Set<String>? = null

    fun getMBeanNames(result: MBeanListResult?): Collection<String> {
        synchronized(mbeanGuard) {
            if (result != null) {
                if (result.isChanged) {
                    lastMBeanNames = result.names
                }
                lastMBeanNames?.let { return it }
            }
        }
        return emptyList()
    }
}
