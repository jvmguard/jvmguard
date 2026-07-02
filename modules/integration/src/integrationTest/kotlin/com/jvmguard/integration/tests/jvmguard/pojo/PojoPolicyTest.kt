package com.jvmguard.integration.tests.jvmguard.pojo

import com.jvmguard.agent.config.transactions.ComparisonType
import com.jvmguard.agent.config.transactions.PojoTransactionDef
import com.jvmguard.agent.config.transactions.ReentryInhibition
import com.jvmguard.agent.config.transactions.naming.ClassNameElement
import com.jvmguard.agent.config.transactions.naming.MethodNameElement
import com.jvmguard.agent.config.transactions.naming.TextElement
import com.jvmguard.integration.JvmGuardTest
import com.jvmguard.integration.Controller
import com.jvmguard.integration.TestServerConnection
import com.jvmguard.integration.TestVmManager
import com.jvmguard.integration.tests.jvmguard.pojo.classes.policy.Policy1
import com.jvmguard.integration.tests.jvmguard.pojo.classes.policy.Policy1Sub2
import com.jvmguard.integration.util.TimeComparator
import com.jvmguard.integration.util.TransactionTreeComparator
import com.jvmguard.data.config.GroupConfig
import com.jvmguard.data.transactions.TransactionDataType
import com.jvmguard.data.transactions.TransactionTreeInterval

class PojoPolicyTest : JvmGuardTest() {

    override fun getJvmGuardOptions(runNo: Int, vmNo: Int, libraryNo: Int) = super.getJvmGuardOptions(runNo, vmNo, libraryNo) + " -Xmx64m"

    override fun modifyInitialRootConfig(rootConfig: GroupConfig) {
        rootConfig.transactionSettings.transactionDefs.add(PojoTransactionDef().apply {
            id = 100
            initDefault()
            interceptionTarget = PojoTransactionDef.InterceptionTarget.CLASS
            isInterceptSubclasses = true
            declaringClassName = Policy1::class.java.name
            naming.reentryInhibition = ReentryInhibition.DEF
            val naming = naming.namingElements
            naming.clear()
            naming.add(ClassNameElement(ClassNameElement.PackageMode.NONE))
            naming.add(TextElement("."))
            naming.add(MethodNameElement())
        })
    }

    override fun connect(vmManager: TestVmManager, serverConnection: TestServerConnection, controller: Controller) {
        val comparator = TransactionTreeComparator(TimeComparator.THIRTY_PERCENT)
        waitForConnection(serverConnection, listOf("JVM"))

        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 1, true, comparator)

        modifyCurrentRootConfig(serverConnection) { rootConfig ->
            rootConfig.transactionSettings.transactionDefs.add(0, PojoTransactionDef().apply {
                id = 101
                initDefault()
                interceptionTarget = PojoTransactionDef.InterceptionTarget.CLASS
                isInterceptSubclasses = true
                declaringClassName = Policy1::class.java.name
                className = Policy1Sub2::class.java.name + "*"
                naming.isActive = false
                policy.isCheckedExceptionAsError = true
                policy.isLoggedWarningAsError = true
            })
        }

        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 2, true, comparator)

        modifyCurrentRootConfig(serverConnection) { rootConfig ->
            rootConfig.transactionSettings.transactionDefs.first { it.id == 100L }.apply {
                naming.namingElements.add(TextElement(" 2"))
            }

            rootConfig.transactionSettings.transactionDefs.add(0, PojoTransactionDef().apply {
                id = 102
                initDefault()
                interceptionTarget = PojoTransactionDef.InterceptionTarget.CLASS
                isInterceptSubclasses = true
                declaringClassName = Policy1::class.java.name
                className = ".*_2"
                comparisonType = ComparisonType.REGEX
                isDiscard = true
            })
        }

        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 3, true, comparator)
    }

}
