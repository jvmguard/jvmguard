package com.jvmguard.integration

import com.jvmguard.collector.api.AgentConnection
import com.jvmguard.collector.main.VmManagerImpl
import com.jvmguard.data.vmdata.Connection
import com.jvmguard.data.vmdata.VM
import com.jvmguard.data.vmdata.VmIdentifier

class TestVmManager(private val delegate: VmManagerImpl) {

    val currentConnections: List<Connection>
        get() = delegate.currentConnections

    fun getConnection(vm: VM): AgentConnection = delegate.getConnection(vm)

    fun getGroupVM(groupIdentifier: VmIdentifier): VM = delegate.getGroupVM(groupIdentifier)

    fun terminate(vm: VM, closeConnection: Boolean) {
        delegate.terminate(vm, closeConnection)
    }
}
