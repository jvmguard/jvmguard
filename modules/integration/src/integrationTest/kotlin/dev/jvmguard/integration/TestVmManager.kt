package dev.jvmguard.integration

import dev.jvmguard.collector.api.AgentConnection
import dev.jvmguard.collector.main.VmManagerImpl
import dev.jvmguard.data.vmdata.Connection
import dev.jvmguard.data.vmdata.VM
import dev.jvmguard.data.vmdata.VmIdentifier

class TestVmManager(private val delegate: VmManagerImpl) {

    val currentConnections: List<Connection>
        get() = delegate.currentConnections

    fun getConnection(vm: VM): AgentConnection = delegate.getConnection(vm)

    fun getGroupVM(groupIdentifier: VmIdentifier): VM = delegate.getGroupVM(groupIdentifier)

    fun terminate(vm: VM, closeConnection: Boolean) {
        delegate.terminate(vm, closeConnection)
    }
}
