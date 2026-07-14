package dev.jvmguard.integration.tests.jvmguard.cap

import dev.jvmguard.agent.config.transactions.MatchedTransactionDef
import dev.jvmguard.integration.JvmGuardTest
import dev.jvmguard.integration.Controller
import dev.jvmguard.integration.TestServerConnection
import dev.jvmguard.integration.TestVmManager
import dev.jvmguard.integration.config.VMConfig
import dev.jvmguard.integration.util.TimeComparator
import dev.jvmguard.integration.util.TransactionTreeComparator
import dev.jvmguard.data.config.GlobalConfig
import dev.jvmguard.data.transactions.CapType
import dev.jvmguard.data.transactions.TransactionDataType
import dev.jvmguard.data.transactions.TransactionTreeInterval
import java.util.*

class CapTest : JvmGuardTest() {

    override fun getJvmGuardOptions(runNo: Int, vmNo: Int, libraryNo: Int) = super.getJvmGuardOptions(runNo, vmNo, libraryNo) + " -Xmx64m"
    override fun getRunCount(vmConfig: VMConfig) = 2

    override fun modifyInitialGlobalConfig(globalConfig: GlobalConfig) {
        globalConfig.transactionCap = 30
    }

    override fun connect(vmManager: TestVmManager, serverConnection: TestServerConnection, controller: Controller) {
        val comparator = TransactionTreeComparator(TimeComparator.NONE)
        waitForConnection(serverConnection, listOf("JVM"))

        val runNo = Integer.getInteger("singleRunNo")
        if (runNo == 1) {
            waitForNextConfigRequest(serverConnection)
            checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 1, true, comparator)
            waitForNextConfigRequest(serverConnection)
            checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 2, true, comparator)
            assertEqual(serverConnection.caps, EnumSet.of(CapType.TRANSACTION))
            assertEqual(serverConnection.inboxItems.size, 1)
            assertTrue(serverConnection.inboxItems.find { it.name == "Too many transactions recorded" } != null)

            modifyCurrentRootConfig(serverConnection) { rootConfig ->
                rootConfig.transactionSettings.transactionDefs.add(MatchedTransactionDef().apply {
                    id = 100
                    className = "*.a.*"
                    isDiscard = true
                })
            }

            waitForNextConfigRequest(serverConnection)
            checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 3, true, comparator)
            serverConnection.resetCaps()
            waitForNextConfigRequest(serverConnection)
            checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 4, true, comparator)
        } else {
            waitForNextConfigRequest(serverConnection)
            checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 5, true, comparator)
            waitForNextConfigRequest(serverConnection)
            checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 6, true, comparator)
        }

    }
}
