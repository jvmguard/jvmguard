package com.jvmguard.integration.tests.jvmguard.matched

import com.jvmguard.agent.config.transactions.MatchedTransactionDef
import com.jvmguard.agent.config.transactions.naming.MethodNameElement
import com.jvmguard.agent.config.transactions.naming.TextElement
import com.jvmguard.integration.JvmGuardTest
import com.jvmguard.integration.Controller
import com.jvmguard.integration.TestServerConnection
import com.jvmguard.integration.TestVmManager
import com.jvmguard.integration.tests.jvmguard.matched.classes.modifier.BaseIf2
import com.jvmguard.integration.tests.jvmguard.matched.classes.modifier.Modifier1
import com.jvmguard.integration.tests.jvmguard.matched.classes.modifier.Modifier2
import com.jvmguard.integration.util.TimeComparator
import com.jvmguard.integration.util.TransactionTreeComparator
import com.jvmguard.data.config.GroupConfig
import com.jvmguard.data.transactions.TransactionDataType
import com.jvmguard.data.transactions.TransactionTreeInterval

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
