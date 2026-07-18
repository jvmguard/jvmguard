package dev.jvmguard.integration.tests.jvmguard.mapped

import dev.jvmguard.agent.config.transactions.MappedTransactionDef
import dev.jvmguard.agent.config.transactions.DurationType
import dev.jvmguard.agent.config.transactions.Policy
import dev.jvmguard.agent.config.transactions.naming.InstanceElement
import dev.jvmguard.agent.config.transactions.naming.MethodNameElement
import dev.jvmguard.agent.config.transactions.naming.TextElement
import dev.jvmguard.integration.JvmGuardTest
import dev.jvmguard.integration.Controller
import dev.jvmguard.integration.TestServerConnection
import dev.jvmguard.integration.TestVmManager
import dev.jvmguard.integration.tests.jvmguard.mapped.classes.naming.CAnno1
import dev.jvmguard.integration.tests.jvmguard.mapped.classes.naming.MAnno3
import dev.jvmguard.integration.util.TimeComparator
import dev.jvmguard.integration.util.TransactionTreeComparator
import dev.jvmguard.data.config.GroupConfig
import dev.jvmguard.data.transactions.TransactionDataType
import dev.jvmguard.data.transactions.TransactionTreeInterval

class MappedStaticNamingTest: JvmGuardTest() {

    override fun getJvmGuardOptions(runNo: Int, vmNo: Int, libraryNo: Int) =
        super.getJvmGuardOptions(runNo, vmNo, libraryNo) + " -Xmx64m"

    override fun modifyInitialRootConfig(rootConfig: GroupConfig) {
        val transactionDef = MappedTransactionDef()
        initDefaultNaming(transactionDef)
        initDefaultPolicy(transactionDef.policy)
        transactionDef.id = 100
        transactionDef.annotatedTarget = MappedTransactionDef.AnnotatedTarget.CLASS
        transactionDef.isInterceptSubclasses = true
        transactionDef.annotationName = CAnno1::class.java.name
        rootConfig.transactionSettings.transactionDefs.add(transactionDef)
    }


    private fun initDefaultNaming(transactionDef: MappedTransactionDef) {
        transactionDef.initDefault()
        transactionDef.naming.namingElements.add(TextElement("."))
        transactionDef.naming.namingElements.add(MethodNameElement())
    }

    private fun initDefaultPolicy(policy: Policy) {
        policy.slowValue = 500
        policy.slowDurationType = DurationType.MILLIS
        policy.verySlowValue = 10000
        policy.verySlowDurationType = DurationType.MILLIS
    }

    override fun connect(vmManager: TestVmManager, serverConnection: TestServerConnection, controller: Controller) {
        val comparator = TransactionTreeComparator(TimeComparator.NONE)
        waitForConnection(serverConnection, listOf("JVM"))

        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 1, comparator)

        modifyCurrentRootConfig(serverConnection) { rootConfig ->
            val transactionDef = MappedTransactionDef()
            initDefaultNaming(transactionDef)
            transactionDef.naming.namingElements.add(TextElement(": "))
            transactionDef.naming.namingElements.add(InstanceElement())
            initDefaultPolicy(transactionDef.policy)
            transactionDef.id = 102
            transactionDef.annotatedTarget = MappedTransactionDef.AnnotatedTarget.METHOD
            transactionDef.isInterceptSubclasses = true
            transactionDef.isUseDeclaringClassName = true
            transactionDef.annotationName = MAnno3::class.java.name
            rootConfig.transactionSettings.transactionDefs.add(transactionDef)
        }

        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 2, comparator)

        modifyCurrentRootConfig(serverConnection) { rootConfig ->
            val transactionDef = rootConfig.transactionSettings.transactionDefs.find { it.id == 100L } as MappedTransactionDef
            transactionDef.naming.namingElements.add(TextElement(" added"))
        }

        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 3, comparator)
    }
}
