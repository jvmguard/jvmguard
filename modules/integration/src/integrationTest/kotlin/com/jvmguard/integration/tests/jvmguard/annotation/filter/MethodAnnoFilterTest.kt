package com.jvmguard.integration.tests.jvmguard.annotation.filter

import com.jvmguard.agent.config.transactions.CustomAnnotatedTransactionDef
import com.jvmguard.agent.config.transactions.DevOpsAnnotatedTransactionDef
import com.jvmguard.integration.JvmGuardTest
import com.jvmguard.integration.Controller
import com.jvmguard.integration.TestServerConnection
import com.jvmguard.integration.TestVmManager
import com.jvmguard.integration.tests.jvmguard.annotation.classes.naming.MAnno1
import com.jvmguard.integration.tests.jvmguard.annotation.classes.naming.MAnno2
import com.jvmguard.integration.tests.jvmguard.annotation.filter.classes.Class1
import com.jvmguard.integration.util.TimeComparator
import com.jvmguard.integration.util.TransactionTreeComparator
import com.jvmguard.data.config.GroupConfig
import com.jvmguard.data.transactions.TransactionDataType
import com.jvmguard.data.transactions.TransactionTreeInterval

class MethodAnnoFilterTest : JvmGuardTest() {

    override fun getJvmGuardOptions(runNo: Int, vmNo: Int, libraryNo: Int) =
        super.getJvmGuardOptions(runNo, vmNo, libraryNo) + " -Xmx64m"

    override fun modifyInitialRootConfig(rootConfig: GroupConfig) {
        val devOpsTransactionDef = DevOpsAnnotatedTransactionDef()
        devOpsTransactionDef.id = 100
        devOpsTransactionDef.className = Class1::class.java.name
        devOpsTransactionDef.group.usedValue = "test"
        devOpsTransactionDef.policy.isRuntimeExceptionAsError = false

        rootConfig.transactionSettings.transactionDefs.add(0, devOpsTransactionDef)

        var customTransactionDef = CustomAnnotatedTransactionDef()
        customTransactionDef.initDefault()
        customTransactionDef.id = 101
        customTransactionDef.className = Class1::class.java.name
        customTransactionDef.annotationName = MAnno1::class.java.name
        customTransactionDef.annotatedTarget = CustomAnnotatedTransactionDef.AnnotatedTarget.METHOD
        customTransactionDef.policy.isRuntimeExceptionAsError = false
        rootConfig.transactionSettings.transactionDefs.add(customTransactionDef)

        customTransactionDef = CustomAnnotatedTransactionDef()
        customTransactionDef.initDefault()
        customTransactionDef.id = 102
        customTransactionDef.annotationName = MAnno1::class.java.name
        customTransactionDef.annotatedTarget = CustomAnnotatedTransactionDef.AnnotatedTarget.METHOD
        rootConfig.transactionSettings.transactionDefs.add(customTransactionDef)

    }

    override fun connect(vmManager: TestVmManager, serverConnection: TestServerConnection, controller: Controller) {
        val comparator = TransactionTreeComparator(TimeComparator.NONE)
        waitForConnection(serverConnection, listOf("JVM"))

        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 1, true, comparator)

        modifyCurrentRootConfig(serverConnection) { rootConfig ->
            val customTransactionDef = CustomAnnotatedTransactionDef()
            customTransactionDef.initDefault()
            customTransactionDef.id = 103
            customTransactionDef.annotationName = MAnno2::class.java.name
            customTransactionDef.annotatedTarget = CustomAnnotatedTransactionDef.AnnotatedTarget.METHOD
            rootConfig.transactionSettings.transactionDefs.add(customTransactionDef)
        }

        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 2, true, comparator)
    }
}
