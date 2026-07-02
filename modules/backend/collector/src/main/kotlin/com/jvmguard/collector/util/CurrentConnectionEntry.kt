package com.jvmguard.collector.util

import com.jvmguard.agent.mbean.MBeanListResult
import com.jvmguard.collector.connection.AgentConnectionImpl
import com.jvmguard.collector.vmdata.VmData
import com.jvmguard.data.vmdata.Connection
import com.jvmguard.data.vmdata.VM
import java.util.*

class CurrentConnectionEntry(
    val groupNames: Array<String>,
    val vm: VM,
    val vmData: VmData,
    val agentConnection: AgentConnectionImpl,
    val connection: Connection,
) {
    val committedDevOpsTelemetryFormats: MutableSet<String> = Collections.synchronizedSet(HashSet())

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
