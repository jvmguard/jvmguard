package dev.jvmguard.collector.main

import dev.jvmguard.collector.connection.AgentConnectionImpl
import dev.jvmguard.collector.util.CurrentConnectionEntry
import dev.jvmguard.data.vmdata.VM
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import org.springframework.stereotype.Component

@Component
class ConnectionRegistry {

    private val vm2Connection = HashMap<VM, CurrentConnectionEntry>()
    private val instanceId2Vm = Long2ObjectOpenHashMap<VM>()

    fun get(vm: VM): CurrentConnectionEntry? {
        return vm2Connection[vm]
    }

    fun put(vm: VM, connectionEntry: CurrentConnectionEntry) {
        vm2Connection[vm] = connectionEntry
    }

    fun remove(vm: VM) {
        vm2Connection.remove(vm)
    }

    fun containsKey(vm: VM): Boolean {
        return vm2Connection.containsKey(vm)
    }

    fun size(): Int {
        return vm2Connection.size
    }

    fun values(): Collection<CurrentConnectionEntry> {
        return vm2Connection.values
    }

    fun keySet(): Set<VM> {
        return vm2Connection.keys
    }

    fun getInstanceVm(instanceId: Long): VM? {
        return instanceId2Vm.get(instanceId)
    }

    fun putInstanceVm(instanceId: Long, vm: VM) {
        instanceId2Vm.put(instanceId, vm)
    }

    fun removeInstanceVm(instanceId: Long) {
        instanceId2Vm.remove(instanceId)
    }

    @Synchronized
    fun getConnections(): Array<Map.Entry<VM, CurrentConnectionEntry>> {
        return vm2Connection.entries.toTypedArray()
    }

    fun getLiveConnection(vm: VM): AgentConnectionImpl? {
        val connectionEntry = synchronized(this) { vm2Connection[vm] }
        if (connectionEntry != null) {
            val agentConnection = connectionEntry.agentConnection
            if (agentConnection.isAlive) {
                return agentConnection
            }
        }
        return null
    }
}
