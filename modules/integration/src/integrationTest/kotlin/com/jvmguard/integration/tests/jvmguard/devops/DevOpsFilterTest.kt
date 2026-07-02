package com.jvmguard.integration.tests.jvmguard.devops

import com.jvmguard.agent.config.transactions.DevOpsAnnotatedTransactionDef
import com.jvmguard.integration.JvmGuardTest
import com.jvmguard.integration.Controller
import com.jvmguard.integration.TestServerConnection
import com.jvmguard.integration.TestVmManager
import com.jvmguard.integration.util.TimeComparator
import com.jvmguard.integration.util.TransactionTreeComparator
import com.jvmguard.data.transactions.TransactionDataType
import com.jvmguard.data.transactions.TransactionTreeInterval

class DevOpsFilterTest : JvmGuardTest() {

    override fun getJvmGuardOptions(runNo: Int, vmNo: Int, libraryNo: Int) = super.getJvmGuardOptions(runNo, vmNo, libraryNo) + " -Xmx64m"

    override fun connect(vmManager: TestVmManager, serverConnection: TestServerConnection, controller: Controller) {
        val comparator = TransactionTreeComparator(TimeComparator.NONE)
        waitForConnection(serverConnection, listOf("JVM"))

        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 1, true, comparator)

        modifyCurrentRootConfig(serverConnection) { rootConfig ->
            val devOpsTransactionDef = DevOpsAnnotatedTransactionDef()
            devOpsTransactionDef.id = 100
            devOpsTransactionDef.className = "*.a.*"
            devOpsTransactionDef.isDiscard = true

            rootConfig.transactionSettings.transactionDefs.add(0, devOpsTransactionDef)
        }

        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 2, false, comparator)

        modifyCurrentRootConfig(serverConnection) { rootConfig ->
            val devOpsTransactionDef = DevOpsAnnotatedTransactionDef()
            devOpsTransactionDef.id = 101
            devOpsTransactionDef.className = "*.b.*"
            devOpsTransactionDef.policy.isRuntimeExceptionAsError = false

            rootConfig.transactionSettings.transactionDefs.add(1, devOpsTransactionDef)
        }

        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 3, false, comparator)

        modifyCurrentRootConfig(serverConnection) { rootConfig ->
            val devOpsTransactionDef = rootConfig.transactionSettings.transactionDefs.find { it.id == 101L }!!
            devOpsTransactionDef.policy.isRuntimeExceptionAsError = true
        }

        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 4, false, comparator)

        modifyCurrentRootConfig(serverConnection) { rootConfig ->
            rootConfig.transactionSettings.transactionDefs.removeAll { it.id == 100L || it.id == 101L }
            rootConfig.transactionSettings.transactionDefs.add(0, DevOpsAnnotatedTransactionDef().apply {
                id = 103
                group.usedValue = "group1"
                policy.isRuntimeExceptionAsError = false
            })

            rootConfig.transactionSettings.transactionDefs.add(0, DevOpsAnnotatedTransactionDef().apply {
                id = 104
                group.usedValue = "group2"
                className = "*.a.*"
                isDiscard = true
            })
        }

        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 5, false, comparator)

    }
}
