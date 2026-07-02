package com.jvmguard.integration.tests.jvmguard.devops

import com.jvmguard.agent.config.transactions.PojoTransactionDef
import com.jvmguard.agent.config.transactions.naming.MethodNameElement
import com.jvmguard.agent.config.transactions.naming.TextElement
import com.jvmguard.integration.JvmGuardTest
import com.jvmguard.integration.Controller
import com.jvmguard.integration.TestServerConnection
import com.jvmguard.integration.TestVmManager
import com.jvmguard.integration.tests.jvmguard.devops.classes.naming.DevopsMethodNaming
import com.jvmguard.integration.util.TimeComparator
import com.jvmguard.integration.util.TransactionTreeComparator
import com.jvmguard.data.config.GroupConfig
import com.jvmguard.data.transactions.TransactionDataType
import com.jvmguard.data.transactions.TransactionTreeInterval

class DevOpsMethodNamingTest : JvmGuardTest() {

    override fun getJvmGuardOptions(runNo: Int, vmNo: Int, libraryNo: Int) = super.getJvmGuardOptions(runNo, vmNo, libraryNo) + " -Xmx64m"

    override fun modifyInitialRootConfig(rootConfig: GroupConfig) {
        rootConfig.transactionSettings.transactionDefs.add(PojoTransactionDef().apply {
            id = 100
            initDefault()
            interceptionTarget = PojoTransactionDef.InterceptionTarget.METHOD
            isInterceptSubclasses = false
            declaringClassName = DevopsMethodNaming::class.java.name
            methodName = "pojo1"
            methodSignature = "()V"
            naming.group.usedValue = "group1"
            naming.namingElements.clear()
            naming.namingElements.add(MethodNameElement())
        })

        rootConfig.transactionSettings.transactionDefs.add(PojoTransactionDef().apply {
            id = 101
            initDefault()
            interceptionTarget = PojoTransactionDef.InterceptionTarget.METHOD
            isInterceptSubclasses = false
            declaringClassName = DevopsMethodNaming::class.java.name
            methodName = "pojo2"
            methodSignature = "()V"
            naming.namingElements.clear()
            naming.namingElements.add(MethodNameElement())
        })

        rootConfig.transactionSettings.transactionDefs.add(PojoTransactionDef().apply {
            id = 102
            initDefault()
            interceptionTarget = PojoTransactionDef.InterceptionTarget.METHOD
            isInterceptSubclasses = false
            declaringClassName = DevopsMethodNaming::class.java.name
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
