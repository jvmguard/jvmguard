package dev.jvmguard.integration.tests.jvmguard.matched

import dev.jvmguard.agent.config.transactions.MatchedTransactionDef
import dev.jvmguard.agent.config.transactions.naming.ClassNameElement
import dev.jvmguard.agent.config.transactions.naming.MethodNameElement
import dev.jvmguard.agent.config.transactions.naming.TextElement
import dev.jvmguard.integration.JvmGuardTest
import dev.jvmguard.integration.Controller
import dev.jvmguard.integration.TestServerConnection
import dev.jvmguard.integration.TestVmManager
import dev.jvmguard.integration.tests.jvmguard.matched.classes.BaseIf1
import dev.jvmguard.integration.tests.jvmguard.matched.classes.naming.Name1
import dev.jvmguard.integration.util.TimeComparator
import dev.jvmguard.integration.util.TransactionTreeComparator
import dev.jvmguard.data.config.GroupConfig
import dev.jvmguard.data.transactions.TransactionDataType
import dev.jvmguard.data.transactions.TransactionTreeInterval

class MatchedStaticNamingTest : JvmGuardTest() {

    override fun getJvmGuardOptions(runNo: Int, vmNo: Int, libraryNo: Int) = super.getJvmGuardOptions(runNo, vmNo, libraryNo) + " -Xmx64m"

    override fun modifyInitialRootConfig(rootConfig: GroupConfig) {
        rootConfig.transactionSettings.transactionDefs.add(MatchedTransactionDef().apply {
            id = 100
            initDefault()
            interceptionTarget = MatchedTransactionDef.InterceptionTarget.METHOD
            isInterceptSubclasses = false
            declaringClassName = Name1::class.java.name
            methodName = "invoke"
            methodSignature = "(Ldev/jvmguard/integration/tests/jvmguard/matched/classes/ParameterClass;Ljava/lang/String;I)Ljava/lang/String;"
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
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 1, comparator)

        modifyCurrentRootConfig(serverConnection) { rootConfig ->
            rootConfig.transactionSettings.transactionDefs.add(MatchedTransactionDef().apply {
                id = 101
                initDefault()
                interceptionTarget = MatchedTransactionDef.InterceptionTarget.CLASS
                methodInterceptionMode = MatchedTransactionDef.MethodInterceptionMode.ALL_PUBLIC
                isStaticMethods = true
                isInterceptSubclasses = true
                declaringClassName = BaseIf1::class.java.name
                naming.namingElements.clear()
                naming.namingElements.add(ClassNameElement(ClassNameElement.PackageMode.ABBREVIATED))
                naming.namingElements.add(TextElement("."))
                naming.namingElements.add(MethodNameElement())
            })
        }

        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 2, comparator)

        modifyCurrentRootConfig(serverConnection) { rootConfig ->
            rootConfig.transactionSettings.transactionDefs.first { it.id == 100L }.apply {
                val naming = naming.namingElements
                naming.clear()
                naming.add(ClassNameElement(ClassNameElement.PackageMode.FULL))
                naming.add(TextElement("."))
                naming.add(MethodNameElement())
            }
        }

        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 3, comparator)
    }

}
