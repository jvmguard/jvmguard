package dev.jvmguard.integration.tests.jvmguard.transactions

import dev.jvmguard.agent.config.VmType
import dev.jvmguard.agent.config.transactions.DeclaredTransactionDef
import dev.jvmguard.agent.config.transactions.DurationType
import dev.jvmguard.integration.JvmGuardTest
import dev.jvmguard.integration.Controller
import dev.jvmguard.integration.TestServerConnection
import dev.jvmguard.integration.TestVmManager
import dev.jvmguard.integration.util.TimeComparator
import dev.jvmguard.integration.util.TransactionTreeComparator
import dev.jvmguard.data.config.GroupConfig
import dev.jvmguard.data.transactions.TransactionDataType
import dev.jvmguard.data.transactions.TransactionTreeInterval
import dev.jvmguard.data.vmdata.VmIdentifier

class NestedOverdueTest : JvmGuardTest() {
    override fun getJvmGuardOptions(runNo: Int, vmNo: Int, libraryNo: Int) = super.getJvmGuardOptions(runNo, vmNo, libraryNo) + " -Xmx64m"

    override fun modifyInitialRootConfig(rootConfig: GroupConfig) {
        rootConfig.transactionSettings.transactionDefs.clear()

        rootConfig.transactionSettings.transactionDefs.add(DeclaredTransactionDef().apply {
            initDefault()
            group.usedValue = "policy1"
            policy.slowValue = 0
            policy.verySlowValue = 0
            policy.overdueValue = 0
        })

        rootConfig.transactionSettings.transactionDefs.add(DeclaredTransactionDef().apply {
            initDefault()
            group.usedValue = "policy2"
            policy.slowDurationType = DurationType.MILLIS
            policy.slowValue = 130
            policy.verySlowDurationType = DurationType.MILLIS
            policy.verySlowValue = 280
            policy.overdueDurationType = DurationType.MILLIS
            policy.overdueValue = 800
        })
    }

    override fun getGroupName(vmNo: Int) = "default/sub"

    override fun connect(vmManager: TestVmManager, serverConnection: TestServerConnection, controller: Controller) {
        val transactionTreeComparator = TransactionTreeComparator(TimeComparator.NONE)
        val vm = waitForConnections(serverConnection).first()

        waitForNextConfigRequest(serverConnection)

        val directGroupIdentifier = VmIdentifier(getGroupName(1), VmType.GROUP)

        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 1, true, transactionTreeComparator)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.OVERDUE, 1, true, transactionTreeComparator)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.OVERDUE, 1, true, transactionTreeComparator, vm)
        checkTree(
            serverConnection,
            TransactionTreeInterval.HOUR,
            TransactionDataType.OVERDUE,
            1,
            true,
            transactionTreeComparator,
            vmManager.getGroupVM(directGroupIdentifier)
        )
    }
}
