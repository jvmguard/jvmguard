package com.jvmguard.integration.tests.jvmguard.mapped.filter

import com.jvmguard.agent.config.transactions.MappedTransactionDef
import com.jvmguard.agent.config.transactions.DeclaredTransactionDef
import com.jvmguard.integration.JvmGuardTest
import com.jvmguard.integration.Controller
import com.jvmguard.integration.TestServerConnection
import com.jvmguard.integration.TestVmManager
import com.jvmguard.integration.tests.jvmguard.mapped.classes.naming.MAnno1
import com.jvmguard.integration.tests.jvmguard.mapped.classes.naming.MAnno2
import com.jvmguard.integration.tests.jvmguard.mapped.filter.classes.Class1
import com.jvmguard.integration.util.TimeComparator
import com.jvmguard.integration.util.TransactionTreeComparator
import com.jvmguard.data.config.GroupConfig
import com.jvmguard.data.transactions.TransactionDataType
import com.jvmguard.data.transactions.TransactionTreeInterval

class MethodAnnoFilterTest : JvmGuardTest() {

    override fun getJvmGuardOptions(runNo: Int, vmNo: Int, libraryNo: Int) =
        super.getJvmGuardOptions(runNo, vmNo, libraryNo) + " -Xmx64m"

    override fun modifyInitialRootConfig(rootConfig: GroupConfig) {
        val declaredTransactionDef = DeclaredTransactionDef()
        declaredTransactionDef.id = 100
        declaredTransactionDef.className = Class1::class.java.name
        declaredTransactionDef.group.usedValue = "test"
        declaredTransactionDef.policy.isRuntimeExceptionAsError = false

        rootConfig.transactionSettings.transactionDefs.add(0, declaredTransactionDef)

        var mappedDef = MappedTransactionDef()
        mappedDef.initDefault()
        mappedDef.id = 101
        mappedDef.className = Class1::class.java.name
        mappedDef.annotationName = MAnno1::class.java.name
        mappedDef.annotatedTarget = MappedTransactionDef.AnnotatedTarget.METHOD
        mappedDef.policy.isRuntimeExceptionAsError = false
        rootConfig.transactionSettings.transactionDefs.add(mappedDef)

        mappedDef = MappedTransactionDef()
        mappedDef.initDefault()
        mappedDef.id = 102
        mappedDef.annotationName = MAnno1::class.java.name
        mappedDef.annotatedTarget = MappedTransactionDef.AnnotatedTarget.METHOD
        rootConfig.transactionSettings.transactionDefs.add(mappedDef)

    }

    override fun connect(vmManager: TestVmManager, serverConnection: TestServerConnection, controller: Controller) {
        val comparator = TransactionTreeComparator(TimeComparator.NONE)
        waitForConnection(serverConnection, listOf("JVM"))

        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 1, true, comparator)

        modifyCurrentRootConfig(serverConnection) { rootConfig ->
            val mappedDef = MappedTransactionDef()
            mappedDef.initDefault()
            mappedDef.id = 103
            mappedDef.annotationName = MAnno2::class.java.name
            mappedDef.annotatedTarget = MappedTransactionDef.AnnotatedTarget.METHOD
            rootConfig.transactionSettings.transactionDefs.add(mappedDef)
        }

        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 2, true, comparator)
    }
}
