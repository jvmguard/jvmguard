package dev.jvmguard.integration.tests.jvmguard.mapped

import dev.jvmguard.agent.config.transactions.MappedTransactionDef
import dev.jvmguard.agent.config.transactions.naming.MethodNameElement
import dev.jvmguard.agent.config.transactions.naming.TextElement
import dev.jvmguard.integration.JvmGuardTest
import dev.jvmguard.integration.Controller
import dev.jvmguard.integration.TestServerConnection
import dev.jvmguard.integration.TestVmManager
import dev.jvmguard.integration.tests.jvmguard.mapped.classes.Annotation1
import dev.jvmguard.integration.tests.jvmguard.mapped.classes.Annotation2
import dev.jvmguard.integration.tests.jvmguard.mapped.classes.Annotation3
import dev.jvmguard.integration.util.TransactionTreeComparator
import dev.jvmguard.data.config.GroupConfig
import dev.jvmguard.data.transactions.TransactionDataType
import dev.jvmguard.data.transactions.TransactionTreeInterval
import org.junit.jupiter.api.Tag

@Tag("citest")
class MappedTest : JvmGuardTest() {

    override fun getJvmGuardOptions(runNo: Int, vmNo: Int, libraryNo: Int): String {
        return super.getJvmGuardOptions(runNo, vmNo, libraryNo) + " -Xmx64m"
    }

    @Override
    override fun modifyInitialRootConfig(rootConfig: GroupConfig) {
        var transactionDef = MappedTransactionDef()
        initNaming(transactionDef)
        transactionDef.id = 100
        transactionDef.annotatedTarget = MappedTransactionDef.AnnotatedTarget.METHOD
        transactionDef.isInterceptSubclasses = true
        transactionDef.className = "*.Inc*"
        transactionDef.annotationName = Annotation1::class.java.name

        rootConfig.transactionSettings.transactionDefs.add(transactionDef)

        transactionDef = MappedTransactionDef()
        initNaming(transactionDef)
        transactionDef.id = 101
        transactionDef.annotatedTarget = MappedTransactionDef.AnnotatedTarget.METHOD
        transactionDef.isInterceptSubclasses = false
        transactionDef.annotationName = Annotation3::class.java.name

        rootConfig.transactionSettings.transactionDefs.add(transactionDef)

        transactionDef = MappedTransactionDef()
        initNaming(transactionDef)
        transactionDef.id = 102
        transactionDef.annotatedTarget = MappedTransactionDef.AnnotatedTarget.CLASS
        transactionDef.isInterceptSubclasses = false
        transactionDef.annotationName = Annotation3::class.java.name

        rootConfig.transactionSettings.transactionDefs.add(transactionDef)
    }

    fun initNaming(transactionDef: MappedTransactionDef) {
        transactionDef.initDefault()
        transactionDef.naming.namingElements.add(TextElement("."))
        transactionDef.naming.namingElements.add(MethodNameElement())
    }

    override fun connect(vmManager: TestVmManager, serverConnection: TestServerConnection, controller: Controller) {
        val comparator = TransactionTreeComparator(dev.jvmguard.integration.util.TimeComparator.NONE)
        waitForConnection(serverConnection, listOf("JVM"))

        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 1, comparator)

        modifyCurrentRootConfig(serverConnection) { rootConfig ->
            val transactionDef = MappedTransactionDef()
            initNaming(transactionDef)
            transactionDef.id = 110
            transactionDef.annotatedTarget = MappedTransactionDef.AnnotatedTarget.METHOD
            transactionDef.isInterceptSubclasses = true
            transactionDef.className = "*.Inc*"
            transactionDef.annotationName = Annotation2::class.java.name

            rootConfig.transactionSettings.transactionDefs.add(transactionDef)
        }

        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 2, comparator)
    }
}
