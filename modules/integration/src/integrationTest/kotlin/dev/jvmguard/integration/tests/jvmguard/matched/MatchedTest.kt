package dev.jvmguard.integration.tests.jvmguard.matched

import dev.jvmguard.agent.config.transactions.ComparisonType
import dev.jvmguard.agent.config.transactions.MatchedTransactionDef
import dev.jvmguard.agent.config.transactions.naming.ClassNameElement
import dev.jvmguard.agent.config.transactions.naming.MethodNameElement
import dev.jvmguard.agent.config.transactions.naming.TextElement
import dev.jvmguard.integration.Controller
import dev.jvmguard.integration.JvmGuardTest
import dev.jvmguard.integration.TestServerConnection
import dev.jvmguard.integration.TestVmManager
import dev.jvmguard.integration.tests.jvmguard.matched.classes.BaseIf1
import dev.jvmguard.integration.tests.jvmguard.matched.classes.SimpleMatched
import dev.jvmguard.integration.util.TimeComparator
import dev.jvmguard.integration.util.TransactionTreeComparator
import dev.jvmguard.data.config.GroupConfig
import dev.jvmguard.data.transactions.TransactionDataType
import dev.jvmguard.data.transactions.TransactionTreeInterval

class MatchedTest : JvmGuardTest() {

    override fun getJvmGuardOptions(runNo: Int, vmNo: Int, libraryNo: Int) = super.getJvmGuardOptions(runNo, vmNo, libraryNo) + " -Xmx64m"

    override fun modifyInitialRootConfig(rootConfig: GroupConfig) {
        rootConfig.transactionSettings.transactionDefs.add(MatchedTransactionDef().apply {
            id = 100
            initDefault()
            interceptionTarget = MatchedTransactionDef.InterceptionTarget.METHOD
            isInterceptSubclasses = false
            declaringClassName = SimpleMatched::class.java.name
            methodName = "invoke"
            methodSignature = "(Ldev/jvmguard/integration/tests/jvmguard/matched/classes/ParameterClass;Ljava/lang/String;I)Ljava/lang/String;"
            initTiming(this)
        })

        rootConfig.transactionSettings.transactionDefs.add(MatchedTransactionDef().apply {
            id = 101
            initBaseIf(this)
        })
    }

    override fun connect(vmManager: TestVmManager, serverConnection: TestServerConnection, controller: Controller) {
        val comparator = TransactionTreeComparator(TimeComparator.NONE)
        waitForConnection(serverConnection, listOf("JVM"))

        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 1, comparator)

        modifyCurrentRootConfig(serverConnection) { rootConfig ->
            (rootConfig.transactionSettings.transactionDefs.first { it.id == 100L } as MatchedTransactionDef).apply {
                methodName = "disabled"
            }

            (rootConfig.transactionSettings.transactionDefs.first { it.id == 101L } as MatchedTransactionDef).apply {
                isInterceptSubclasses = false
            }
        }

        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 2, comparator)

        modifyCurrentRootConfig(serverConnection) { rootConfig ->
            (rootConfig.transactionSettings.transactionDefs.first { it.id == 100L } as MatchedTransactionDef).apply {
                methodName = "invoke"
            }

            (rootConfig.transactionSettings.transactionDefs.first { it.id == 101L } as MatchedTransactionDef).apply {
                isInterceptSubclasses = true
            }
        }

        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 3, comparator)

        modifyCurrentRootConfig(serverConnection) { rootConfig ->
            rootConfig.transactionSettings.transactionDefs.add(0, MatchedTransactionDef().apply {
                id = 102
                initBaseIf(this)
                policy.isRuntimeExceptionAsError = false
                className = ".*\\.a\\..*"
                comparisonType = ComparisonType.REGEX
            })
        }

        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 4, comparator)
    }

    private fun initBaseIf(matchedDef: MatchedTransactionDef) {
        matchedDef.initDefault()
        matchedDef.interceptionTarget = MatchedTransactionDef.InterceptionTarget.CLASS
        matchedDef.methodInterceptionMode = MatchedTransactionDef.MethodInterceptionMode.IMPLEMENTING_PUBLIC
        matchedDef.isInterceptSubclasses = true
        matchedDef.declaringClassName = BaseIf1::class.java.name
        matchedDef.naming.namingElements.clear()
        matchedDef.naming.namingElements.add(ClassNameElement(ClassNameElement.PackageMode.ABBREVIATED))
        matchedDef.naming.namingElements.add(TextElement("."))
        matchedDef.naming.namingElements.add(MethodNameElement())
        initTiming(matchedDef)
    }

    private fun initTiming(transactionDef: MatchedTransactionDef) {
        transactionDef.policy.slowValue = 0
        transactionDef.policy.verySlowValue = 0
    }
}
