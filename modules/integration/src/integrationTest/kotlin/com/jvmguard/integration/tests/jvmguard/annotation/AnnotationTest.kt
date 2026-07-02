package com.jvmguard.integration.tests.jvmguard.annotation

import com.jvmguard.agent.config.transactions.CustomAnnotatedTransactionDef
import com.jvmguard.agent.config.transactions.naming.MethodNameElement
import com.jvmguard.agent.config.transactions.naming.TextElement
import com.jvmguard.integration.JvmGuardTest
import com.jvmguard.integration.Controller
import com.jvmguard.integration.TestServerConnection
import com.jvmguard.integration.TestVmManager
import com.jvmguard.integration.tests.jvmguard.annotation.classes.Annotation1
import com.jvmguard.integration.tests.jvmguard.annotation.classes.Annotation2
import com.jvmguard.integration.tests.jvmguard.annotation.classes.Annotation3
import com.jvmguard.integration.util.TransactionTreeComparator
import com.jvmguard.data.config.GroupConfig
import com.jvmguard.data.transactions.TransactionDataType
import com.jvmguard.data.transactions.TransactionTreeInterval
import org.junit.jupiter.api.Tag

@Tag("citest")
class AnnotationTest : JvmGuardTest() {

    override fun getJvmGuardOptions(runNo: Int, vmNo: Int, libraryNo: Int): String {
        return super.getJvmGuardOptions(runNo, vmNo, libraryNo) + " -Xmx64m"
    }

    @Override
    override fun modifyInitialRootConfig(rootConfig: GroupConfig) {
        var transactionDef = CustomAnnotatedTransactionDef()
        initNaming(transactionDef)
        transactionDef.id = 100
        transactionDef.annotatedTarget = CustomAnnotatedTransactionDef.AnnotatedTarget.METHOD
        transactionDef.isInterceptSubclasses = true
        transactionDef.className = "*.Inc*"
        transactionDef.annotationName = Annotation1::class.java.name

        rootConfig.transactionSettings.transactionDefs.add(transactionDef)

        transactionDef = CustomAnnotatedTransactionDef()
        initNaming(transactionDef)
        transactionDef.id = 101
        transactionDef.annotatedTarget = CustomAnnotatedTransactionDef.AnnotatedTarget.METHOD
        transactionDef.isInterceptSubclasses = false
        transactionDef.annotationName = Annotation3::class.java.name

        rootConfig.transactionSettings.transactionDefs.add(transactionDef)

        transactionDef = CustomAnnotatedTransactionDef()
        initNaming(transactionDef)
        transactionDef.id = 102
        transactionDef.annotatedTarget = CustomAnnotatedTransactionDef.AnnotatedTarget.CLASS
        transactionDef.isInterceptSubclasses = false
        transactionDef.annotationName = Annotation3::class.java.name

        rootConfig.transactionSettings.transactionDefs.add(transactionDef)
    }

    fun initNaming(transactionDef: CustomAnnotatedTransactionDef) {
        transactionDef.initDefault()
        transactionDef.naming.namingElements.add(TextElement("."))
        transactionDef.naming.namingElements.add(MethodNameElement())
    }

    override fun connect(vmManager: TestVmManager, serverConnection: TestServerConnection, controller: Controller) {
        val comparator = TransactionTreeComparator(com.jvmguard.integration.util.TimeComparator.NONE)
        waitForConnection(serverConnection, listOf("JVM"))

        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 1, true, comparator)

        modifyCurrentRootConfig(serverConnection) { rootConfig ->
            val transactionDef = CustomAnnotatedTransactionDef()
            initNaming(transactionDef)
            transactionDef.id = 110
            transactionDef.annotatedTarget = CustomAnnotatedTransactionDef.AnnotatedTarget.METHOD
            transactionDef.isInterceptSubclasses = true
            transactionDef.className = "*.Inc*"
            transactionDef.annotationName = Annotation2::class.java.name

            rootConfig.transactionSettings.transactionDefs.add(transactionDef)
        }

        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 2, false, comparator)
    }
}
