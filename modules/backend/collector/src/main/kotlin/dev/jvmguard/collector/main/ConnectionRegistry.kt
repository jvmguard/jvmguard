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

    @Synchronized
    fun get(vm: VM): CurrentConnectionEntry? {
        return vm2Connection[vm]
    }

    @Synchronized
    fun put(vm: VM, connectionEntry: CurrentConnectionEntry) {
        vm2Connection[vm] = connectionEntry
    }

    @Synchronized
    fun remove(vm: VM) {
        vm2Connection.remove(vm)
    }

    @Synchronized
    fun containsKey(vm: VM): Boolean {
        return vm2Connection.containsKey(vm)
    }

    @Synchronized
    fun size(): Int {
        return vm2Connection.size
    }

    @Synchronized
    fun values(): List<CurrentConnectionEntry> {
        return ArrayList(vm2Connection.values)
    }

    @Synchronized
    fun keySet(): Set<VM> {
        return HashSet(vm2Connection.keys)
    }

    @Synchronized
    fun getInstanceVm(instanceId: Long): VM? {
        return instanceId2Vm.get(instanceId)
    }

    @Synchronized
    fun putInstanceVm(instanceId: Long, vm: VM) {
        instanceId2Vm.put(instanceId, vm)
    }

    @Synchronized
    fun removeInstanceVm(instanceId: Long) {
        instanceId2Vm.remove(instanceId)
    }

    @Synchronized
    fun getConnections(): Array<Map.Entry<VM, CurrentConnectionEntry>> {
        return vm2Connection.entries.toTypedArray()
    }

    @Synchronized
    fun getLiveConnection(vm: VM): AgentConnectionImpl? {
        val connectionEntry = vm2Connection[vm]
        if (connectionEntry != null) {
            val agentConnection = connectionEntry.agentConnection
            if (agentConnection.isAlive) {
                return agentConnection
            }
        }
        return null
    }
}
