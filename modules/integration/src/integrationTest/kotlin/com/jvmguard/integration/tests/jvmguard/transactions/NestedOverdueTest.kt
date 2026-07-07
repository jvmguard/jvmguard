package com.jvmguard.integration.tests.jvmguard.transactions

import com.jvmguard.agent.config.VmType
import com.jvmguard.agent.config.transactions.DeclaredTransactionDef
import com.jvmguard.agent.config.transactions.DurationType
import com.jvmguard.integration.JvmGuardTest
import com.jvmguard.integration.Controller
import com.jvmguard.integration.TestServerConnection
import com.jvmguard.integration.TestVmManager
import com.jvmguard.integration.util.TimeComparator
import com.jvmguard.integration.util.TransactionTreeComparator
import com.jvmguard.data.config.GroupConfig
import com.jvmguard.data.transactions.TransactionDataType
import com.jvmguard.data.transactions.TransactionTreeInterval
import com.jvmguard.data.vmdata.VmIdentifier

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
