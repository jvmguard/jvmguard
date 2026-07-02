package com.jvmguard.integration.tests.jvmguard.mbean

import com.jvmguard.integration.JvmGuardTest
import com.jvmguard.integration.Controller
import com.jvmguard.integration.TestServerConnection
import com.jvmguard.integration.TestVmManager
import com.jvmguard.collector.connection.AgentConnectionImpl
import com.jvmguard.data.vmdata.VM

class MBeanNamesTest : JvmGuardTest() {

    override fun connect(vmManager: TestVmManager, serverConnection: TestServerConnection, controller: Controller) {
        var vm = waitForConnection(serverConnection, listOf("JVM")).first()

        waitForNextConfigRequest(serverConnection)
        checkNames(serverConnection, vm, 0, 1, 2, 3)

        modifyCurrentRootConfig(serverConnection) { }
        waitForNextConfigRequest(serverConnection)
        checkNames(serverConnection, vm, 0, 1, 2, 3, 4)

        modifyCurrentRootConfig(serverConnection) { }

        (vmManager.getConnection(vm) as AgentConnectionImpl).close()

        vm = waitForConnection(serverConnection, listOf("JVM")).first()
        sleep(1000 * 10)
        checkNames(serverConnection, vm, 0, 1, 2, 3, 4)
    }

    private fun checkNames(serverConnection: TestServerConnection, vm: VM, vararg expected: Int) {
        checkNamesOnce(serverConnection, vm, *expected)
        checkNamesOnce(serverConnection, vm, *expected)
    }

    private fun checkNamesOnce(serverConnection: TestServerConnection, vm: VM, vararg expected: Int) {
        val mBeanNames = serverConnection.getMBeanNames(vm, false).filter {
            it.startsWith(MBeanNamesWorkload.BEAN_PREFIX)
        }

        assertEqual(mBeanNames.size, expected.size)
        expected.forEach { number ->
            assertTrue(mBeanNames.find { it == MBeanNamesWorkload.BEAN_PREFIX + number } != null)
        }
    }
}
