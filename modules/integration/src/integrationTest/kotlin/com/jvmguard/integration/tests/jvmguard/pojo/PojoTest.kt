package com.jvmguard.integration.tests.jvmguard.pojo

import com.jvmguard.agent.config.transactions.ComparisonType
import com.jvmguard.agent.config.transactions.PojoTransactionDef
import com.jvmguard.agent.config.transactions.naming.ClassNameElement
import com.jvmguard.agent.config.transactions.naming.MethodNameElement
import com.jvmguard.agent.config.transactions.naming.TextElement
import com.jvmguard.integration.Controller
import com.jvmguard.integration.JvmGuardTest
import com.jvmguard.integration.TestServerConnection
import com.jvmguard.integration.TestVmManager
import com.jvmguard.integration.tests.jvmguard.pojo.classes.BaseIf1
import com.jvmguard.integration.tests.jvmguard.pojo.classes.SimplePojo
import com.jvmguard.integration.util.TimeComparator
import com.jvmguard.integration.util.TransactionTreeComparator
import com.jvmguard.data.config.GroupConfig
import com.jvmguard.data.transactions.TransactionDataType
import com.jvmguard.data.transactions.TransactionTreeInterval

class PojoTest : JvmGuardTest() {

    override fun getJvmGuardOptions(runNo: Int, vmNo: Int, libraryNo: Int) = super.getJvmGuardOptions(runNo, vmNo, libraryNo) + " -Xmx64m"

    override fun modifyInitialRootConfig(rootConfig: GroupConfig) {
        rootConfig.transactionSettings.transactionDefs.add(PojoTransactionDef().apply {
            id = 100
            initDefault()
            interceptionTarget = PojoTransactionDef.InterceptionTarget.METHOD
            isInterceptSubclasses = false
            declaringClassName = SimplePojo::class.java.name
            methodName = "invoke"
            methodSignature = "(Lcom/jvmguard/integration/tests/jvmguard/pojo/classes/ParameterClass;Ljava/lang/String;I)Ljava/lang/String;"
            initTiming(this)
        })

        rootConfig.transactionSettings.transactionDefs.add(PojoTransactionDef().apply {
            id = 101
            initBaseIf(this)
        })
    }

    override fun connect(vmManager: TestVmManager, serverConnection: TestServerConnection, controller: Controller) {
        val comparator = TransactionTreeComparator(TimeComparator.NONE)
        waitForConnection(serverConnection, listOf("JVM"))

        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 1, true, comparator)

        modifyCurrentRootConfig(serverConnection) { rootConfig ->
            (rootConfig.transactionSettings.transactionDefs.first { it.id == 100L } as PojoTransactionDef).apply {
                methodName = "disabled"
            }

            (rootConfig.transactionSettings.transactionDefs.first { it.id == 101L } as PojoTransactionDef).apply {
                isInterceptSubclasses = false
            }
        }

        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 2, false, comparator)

        modifyCurrentRootConfig(serverConnection) { rootConfig ->
            (rootConfig.transactionSettings.transactionDefs.first { it.id == 100L } as PojoTransactionDef).apply {
                methodName = "invoke"
            }

            (rootConfig.transactionSettings.transactionDefs.first { it.id == 101L } as PojoTransactionDef).apply {
                isInterceptSubclasses = true
            }
        }

        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 3, true, comparator)

        modifyCurrentRootConfig(serverConnection) { rootConfig ->
            rootConfig.transactionSettings.transactionDefs.add(0, PojoTransactionDef().apply {
                id = 102
                initBaseIf(this)
                policy.isRuntimeExceptionAsError = false
                className = ".*\\.a\\..*"
                comparisonType = ComparisonType.REGEX
            })
        }

        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 4, true, comparator)
    }

    private fun initBaseIf(pojoDev: PojoTransactionDef) {
        pojoDev.initDefault()
        pojoDev.interceptionTarget = PojoTransactionDef.InterceptionTarget.CLASS
        pojoDev.methodInterceptionMode = PojoTransactionDef.MethodInterceptionMode.IMPLEMENTING_PUBLIC
        pojoDev.isInterceptSubclasses = true
        pojoDev.declaringClassName = BaseIf1::class.java.name
        pojoDev.naming.namingElements.clear()
        pojoDev.naming.namingElements.add(ClassNameElement(ClassNameElement.PackageMode.ABBREVIATED))
        pojoDev.naming.namingElements.add(TextElement("."))
        pojoDev.naming.namingElements.add(MethodNameElement())
        initTiming(pojoDev)
    }

    private fun initTiming(transactionDef: PojoTransactionDef) {
        transactionDef.policy.slowValue = 0
        transactionDef.policy.verySlowValue = 0
    }
}
