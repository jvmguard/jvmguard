package dev.jvmguard.integration.tests.jvmguard.modules.java21.vthread.many

import dev.jvmguard.integration.JvmGuardTest
import dev.jvmguard.integration.Controller
import dev.jvmguard.integration.TestServerConnection
import dev.jvmguard.integration.TestVmManager
import dev.jvmguard.integration.config.VMConfig
import dev.jvmguard.integration.util.TimeComparator
import dev.jvmguard.integration.util.TransactionTreeComparator
import dev.jvmguard.data.transactions.TransactionDataType
import dev.jvmguard.data.transactions.TransactionTreeInterval

import dev.jvmguard.integration.MinJdk

@MinJdk(21)
class VThreadManyTest : JvmGuardTest() {

    override fun getJvmGuardOptions(runNo: Int, vmNo: Int, libraryNo: Int) = super.getJvmGuardOptions(runNo, vmNo, libraryNo) + " -Xmx600m -XX:+HeapDumpOnOutOfMemoryError"
    override fun isRunOnVM(vmConfig: VMConfig) = vmConfig.isAtLeastJava(21)

    override fun connect(vmManager: TestVmManager, serverConnection: TestServerConnection, controller: Controller) {
        val comparator = TransactionTreeComparator(TimeComparator.NONE)
        waitForConnection(serverConnection, listOf("JVM"))
        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 1, true, comparator)
        modifyCurrentRootConfig(serverConnection) { }
        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 2, true, comparator)
    }
}
