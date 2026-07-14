package dev.jvmguard.integration.tests.jvmguard.declared

import dev.jvmguard.agent.config.transactions.DeclaredTransactionDef
import dev.jvmguard.integration.JvmGuardTest
import dev.jvmguard.integration.Controller
import dev.jvmguard.integration.TestServerConnection
import dev.jvmguard.integration.TestVmManager
import dev.jvmguard.integration.util.TimeComparator
import dev.jvmguard.integration.util.TransactionTreeComparator
import dev.jvmguard.data.transactions.TransactionDataType
import dev.jvmguard.data.transactions.TransactionTreeInterval

class DeclaredFilterTest : JvmGuardTest() {

    override fun getJvmGuardOptions(runNo: Int, vmNo: Int, libraryNo: Int) = super.getJvmGuardOptions(runNo, vmNo, libraryNo) + " -Xmx64m"

    override fun connect(vmManager: TestVmManager, serverConnection: TestServerConnection, controller: Controller) {
        val comparator = TransactionTreeComparator(TimeComparator.NONE)
        waitForConnection(serverConnection, listOf("JVM"))

        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 1, true, comparator)

        modifyCurrentRootConfig(serverConnection) { rootConfig ->
            val declaredTransactionDef = DeclaredTransactionDef()
            declaredTransactionDef.id = 100
            declaredTransactionDef.className = "*.a.*"
            declaredTransactionDef.isDiscard = true

            rootConfig.transactionSettings.transactionDefs.add(0, declaredTransactionDef)
        }

        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 2, false, comparator)

        modifyCurrentRootConfig(serverConnection) { rootConfig ->
            val declaredTransactionDef = DeclaredTransactionDef()
            declaredTransactionDef.id = 101
            declaredTransactionDef.className = "*.b.*"
            declaredTransactionDef.policy.isRuntimeExceptionAsError = false

            rootConfig.transactionSettings.transactionDefs.add(1, declaredTransactionDef)
        }

        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 3, false, comparator)

        modifyCurrentRootConfig(serverConnection) { rootConfig ->
            val declaredTransactionDef = rootConfig.transactionSettings.transactionDefs.find { it.id == 101L }!!
            declaredTransactionDef.policy.isRuntimeExceptionAsError = true
        }

        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 4, false, comparator)

        modifyCurrentRootConfig(serverConnection) { rootConfig ->
            rootConfig.transactionSettings.transactionDefs.removeAll { it.id == 100L || it.id == 101L }
            rootConfig.transactionSettings.transactionDefs.add(0, DeclaredTransactionDef().apply {
                id = 103
                group.usedValue = "group1"
                policy.isRuntimeExceptionAsError = false
            })

            rootConfig.transactionSettings.transactionDefs.add(0, DeclaredTransactionDef().apply {
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
