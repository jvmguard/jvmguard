package dev.jvmguard.integration.tests.jvmguard.tracing

import dev.jvmguard.integration.Controller
import dev.jvmguard.integration.JvmGuardTest
import dev.jvmguard.integration.TestServerConnection
import dev.jvmguard.integration.TestVmManager
import dev.jvmguard.integration.util.TimeComparator
import dev.jvmguard.integration.util.TransactionTreeComparator
import dev.jvmguard.agent.config.transactions.OtelTransactionDef
import dev.jvmguard.data.config.GroupConfig
import dev.jvmguard.data.transactions.TransactionDataType
import dev.jvmguard.data.transactions.TransactionTreeInterval

class WithSpanDisabledTest : JvmGuardTest() {

    override fun modifyInitialRootConfig(rootConfig: GroupConfig) {
        rootConfig.transactionSettings.transactionDefs.removeIf { it is OtelTransactionDef }
    }

    override fun connect(vmManager: TestVmManager, serverConnection: TestServerConnection, controller: Controller) {
        val comparator = TransactionTreeComparator(TimeComparator.NONE)
        waitForConnection(serverConnection, listOf("JVM"))

        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 1, comparator)
    }
}
