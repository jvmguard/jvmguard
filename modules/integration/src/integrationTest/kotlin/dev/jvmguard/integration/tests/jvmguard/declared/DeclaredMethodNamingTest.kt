package dev.jvmguard.integration.tests.jvmguard.declared

import dev.jvmguard.agent.config.transactions.MatchedTransactionDef
import dev.jvmguard.agent.config.transactions.naming.MethodNameElement
import dev.jvmguard.agent.config.transactions.naming.TextElement
import dev.jvmguard.integration.JvmGuardTest
import dev.jvmguard.integration.Controller
import dev.jvmguard.integration.TestServerConnection
import dev.jvmguard.integration.TestVmManager
import dev.jvmguard.integration.tests.jvmguard.declared.classes.naming.DeclaredMethodNaming
import dev.jvmguard.integration.util.TimeComparator
import dev.jvmguard.integration.util.TransactionTreeComparator
import dev.jvmguard.data.config.GroupConfig
import dev.jvmguard.data.transactions.TransactionDataType
import dev.jvmguard.data.transactions.TransactionTreeInterval

class DeclaredMethodNamingTest : JvmGuardTest() {

    override fun getJvmGuardOptions(runNo: Int, vmNo: Int, libraryNo: Int) = super.getJvmGuardOptions(runNo, vmNo, libraryNo) + " -Xmx64m"

    override fun modifyInitialRootConfig(rootConfig: GroupConfig) {
        rootConfig.transactionSettings.transactionDefs.add(MatchedTransactionDef().apply {
            id = 100
            initDefault()
            interceptionTarget = MatchedTransactionDef.InterceptionTarget.METHOD
            isInterceptSubclasses = false
            declaringClassName = DeclaredMethodNaming::class.java.name
            methodName = "pojo1"
            methodSignature = "()V"
            naming.group.usedValue = "group1"
            naming.namingElements.clear()
            naming.namingElements.add(MethodNameElement())
        })

        rootConfig.transactionSettings.transactionDefs.add(MatchedTransactionDef().apply {
            id = 101
            initDefault()
            interceptionTarget = MatchedTransactionDef.InterceptionTarget.METHOD
            isInterceptSubclasses = false
            declaringClassName = DeclaredMethodNaming::class.java.name
            methodName = "pojo2"
            methodSignature = "()V"
            naming.namingElements.clear()
            naming.namingElements.add(MethodNameElement())
        })

        rootConfig.transactionSettings.transactionDefs.add(MatchedTransactionDef().apply {
            id = 102
            initDefault()
            interceptionTarget = MatchedTransactionDef.InterceptionTarget.METHOD
            isInterceptSubclasses = false
            declaringClassName = DeclaredMethodNaming::class.java.name
            methodName = "pojo3"
            methodSignature = "()V"
            naming.namingElements.clear()
            naming.namingElements.add(TextElement("ntest5"))
        })
    }

    override fun connect(vmManager: TestVmManager, serverConnection: TestServerConnection, controller: Controller) {
        val comparator = TransactionTreeComparator(TimeComparator.NONE)
        waitForConnection(serverConnection, listOf("JVM"))

        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 1, true, comparator)
    }
}
