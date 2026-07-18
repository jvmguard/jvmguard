package dev.jvmguard.integration.tests.jvmguard.matched

import dev.jvmguard.agent.config.transactions.ComparisonType
import dev.jvmguard.agent.config.transactions.MatchedTransactionDef
import dev.jvmguard.agent.config.transactions.ReentryInhibition
import dev.jvmguard.agent.config.transactions.naming.ClassNameElement
import dev.jvmguard.agent.config.transactions.naming.MethodNameElement
import dev.jvmguard.agent.config.transactions.naming.TextElement
import dev.jvmguard.integration.JvmGuardTest
import dev.jvmguard.integration.Controller
import dev.jvmguard.integration.TestServerConnection
import dev.jvmguard.integration.TestVmManager
import dev.jvmguard.integration.tests.jvmguard.matched.classes.policy.Policy1
import dev.jvmguard.integration.tests.jvmguard.matched.classes.policy.Policy1Sub2
import dev.jvmguard.integration.util.TimeComparator
import dev.jvmguard.integration.util.TransactionTreeComparator
import dev.jvmguard.data.config.GroupConfig
import dev.jvmguard.data.transactions.TransactionDataType
import dev.jvmguard.data.transactions.TransactionTreeInterval

class MatchedPolicyTest : JvmGuardTest() {

    override fun getJvmGuardOptions(runNo: Int, vmNo: Int, libraryNo: Int) = super.getJvmGuardOptions(runNo, vmNo, libraryNo) + " -Xmx64m"

    override fun modifyInitialRootConfig(rootConfig: GroupConfig) {
        rootConfig.transactionSettings.transactionDefs.add(MatchedTransactionDef().apply {
            id = 100
            initDefault()
            interceptionTarget = MatchedTransactionDef.InterceptionTarget.CLASS
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
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 1, comparator)

        modifyCurrentRootConfig(serverConnection) { rootConfig ->
            rootConfig.transactionSettings.transactionDefs.add(0, MatchedTransactionDef().apply {
                id = 101
                initDefault()
                interceptionTarget = MatchedTransactionDef.InterceptionTarget.CLASS
                isInterceptSubclasses = true
                declaringClassName = Policy1::class.java.name
                className = Policy1Sub2::class.java.name + "*"
                naming.isActive = false
                policy.isCheckedExceptionAsError = true
                policy.isLoggedWarningAsError = true
            })
        }

        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 2, comparator)

        modifyCurrentRootConfig(serverConnection) { rootConfig ->
            rootConfig.transactionSettings.transactionDefs.first { it.id == 100L }.apply {
                naming.namingElements.add(TextElement(" 2"))
            }

            rootConfig.transactionSettings.transactionDefs.add(0, MatchedTransactionDef().apply {
                id = 102
                initDefault()
                interceptionTarget = MatchedTransactionDef.InterceptionTarget.CLASS
                isInterceptSubclasses = true
                declaringClassName = Policy1::class.java.name
                className = ".*_2"
                comparisonType = ComparisonType.REGEX
                isDiscard = true
            })
        }

        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 3, comparator)
    }

}
