package com.jvmguard.integration.tests.jvmguard.declared

import com.jvmguard.agent.config.transactions.MatchedTransactionDef
import com.jvmguard.agent.config.transactions.naming.MethodNameElement
import com.jvmguard.agent.config.transactions.naming.TextElement
import com.jvmguard.integration.JvmGuardTest
import com.jvmguard.integration.Controller
import com.jvmguard.integration.TestServerConnection
import com.jvmguard.integration.TestVmManager
import com.jvmguard.integration.tests.jvmguard.declared.classes.naming.DeclaredMethodNaming
import com.jvmguard.integration.util.TimeComparator
import com.jvmguard.integration.util.TransactionTreeComparator
import com.jvmguard.data.config.GroupConfig
import com.jvmguard.data.transactions.TransactionDataType
import com.jvmguard.data.transactions.TransactionTreeInterval

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
