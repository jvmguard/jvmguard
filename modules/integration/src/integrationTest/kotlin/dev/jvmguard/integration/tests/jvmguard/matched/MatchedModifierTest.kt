package dev.jvmguard.integration.tests.jvmguard.matched

import dev.jvmguard.agent.config.transactions.MatchedTransactionDef
import dev.jvmguard.agent.config.transactions.naming.MethodNameElement
import dev.jvmguard.agent.config.transactions.naming.TextElement
import dev.jvmguard.integration.JvmGuardTest
import dev.jvmguard.integration.Controller
import dev.jvmguard.integration.TestServerConnection
import dev.jvmguard.integration.TestVmManager
import dev.jvmguard.integration.tests.jvmguard.matched.classes.modifier.BaseIf2
import dev.jvmguard.integration.tests.jvmguard.matched.classes.modifier.Modifier1
import dev.jvmguard.integration.tests.jvmguard.matched.classes.modifier.Modifier2
import dev.jvmguard.integration.util.TimeComparator
import dev.jvmguard.integration.util.TransactionTreeComparator
import dev.jvmguard.data.config.GroupConfig
import dev.jvmguard.data.transactions.TransactionDataType
import dev.jvmguard.data.transactions.TransactionTreeInterval

class MatchedModifierTest : JvmGuardTest() {

    override fun getJvmGuardOptions(runNo: Int, vmNo: Int, libraryNo: Int) = super.getJvmGuardOptions(runNo, vmNo, libraryNo) + " -Xmx64m"

    private fun MatchedTransactionDef.initTiming() {
        policy.slowValue = 0
        policy.verySlowValue = 0
    }

    override fun modifyInitialRootConfig(rootConfig: GroupConfig) {
        val transactionDefs = rootConfig.transactionSettings.transactionDefs
        transactionDefs.add(MatchedTransactionDef().apply {
            initTiming()
            id = 100
            initDefault()
            interceptionTarget = MatchedTransactionDef.InterceptionTarget.CLASS
            isInterceptSubclasses = true
            methodInterceptionMode = MatchedTransactionDef.MethodInterceptionMode.ALL_PUBLIC
            isStaticMethods = true
            declaringClassName = BaseIf2::class.java.name
            naming.namingElements.add(TextElement("."))
            naming.namingElements.add(MethodNameElement())
        })

        transactionDefs.add(MatchedTransactionDef().apply {
            initTiming()
            id = 101
            initDefault()
            interceptionTarget = MatchedTransactionDef.InterceptionTarget.CLASS
            isInterceptSubclasses = false
            methodInterceptionMode = MatchedTransactionDef.MethodInterceptionMode.ALL_PUBLIC
            isStaticMethods = true
            declaringClassName = Modifier1::class.java.name
            naming.namingElements.add(TextElement("."))
            naming.namingElements.add(MethodNameElement())
        })

        transactionDefs.add(MatchedTransactionDef().apply {
            initTiming()
            id = 102
            initDefault()
            interceptionTarget = MatchedTransactionDef.InterceptionTarget.CLASS
            isInterceptSubclasses = true
            methodInterceptionMode = MatchedTransactionDef.MethodInterceptionMode.IMPLEMENTING_PUBLIC
            declaringClassName = Modifier2::class.java.name
            naming.namingElements.add(TextElement("."))
            naming.namingElements.add(MethodNameElement())
        })
    }

    override fun connect(vmManager: TestVmManager, serverConnection: TestServerConnection, controller: Controller) {
        val comparator = TransactionTreeComparator(TimeComparator.NONE)
        waitForConnection(serverConnection, listOf("JVM"))

        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 1, true, comparator)
    }
}
