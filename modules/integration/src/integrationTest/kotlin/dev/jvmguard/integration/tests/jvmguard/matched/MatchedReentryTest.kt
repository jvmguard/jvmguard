package dev.jvmguard.integration.tests.jvmguard.matched

import dev.jvmguard.agent.config.transactions.MatchedTransactionDef
import dev.jvmguard.agent.config.transactions.ReentryInhibition
import dev.jvmguard.agent.config.transactions.TransactionDef
import dev.jvmguard.agent.config.transactions.naming.ClassNameElement
import dev.jvmguard.agent.config.transactions.naming.MethodNameElement
import dev.jvmguard.agent.config.transactions.naming.MethodParameterElement
import dev.jvmguard.agent.config.transactions.naming.TextElement
import dev.jvmguard.integration.JvmGuardTest
import dev.jvmguard.integration.Controller
import dev.jvmguard.integration.TestServerConnection
import dev.jvmguard.integration.TestVmManager
import dev.jvmguard.integration.tests.jvmguard.matched.classes.reentry.Reentry1
import dev.jvmguard.integration.tests.jvmguard.matched.classes.reentry.Reentry2
import dev.jvmguard.integration.tests.jvmguard.matched.classes.reentry.Reentry3
import dev.jvmguard.integration.util.TimeComparator
import dev.jvmguard.integration.util.TransactionTreeComparator
import dev.jvmguard.data.config.GroupConfig
import dev.jvmguard.data.transactions.TransactionDataType
import dev.jvmguard.data.transactions.TransactionTreeInterval

class MatchedReentryTest : JvmGuardTest() {

    override fun getJvmGuardOptions(runNo: Int, vmNo: Int, libraryNo: Int) = super.getJvmGuardOptions(runNo, vmNo, libraryNo) + " -Xmx64m"

    private fun initTiming(transactionDef: TransactionDef) {
        transactionDef.policy.slowValue = 0
        transactionDef.policy.verySlowValue = 0
    }

    override fun modifyInitialRootConfig(rootConfig: GroupConfig) {
        rootConfig.transactionSettings.transactionDefs.forEach { initTiming(it) }

        rootConfig.transactionSettings.transactionDefs.add(MatchedTransactionDef().apply {
            initTiming(this)
            id = 100
            initDefault()
            interceptionTarget = MatchedTransactionDef.InterceptionTarget.METHOD
            isInterceptSubclasses = true
            declaringClassName = Reentry1::class.java.name
            methodName = "m1"
            methodSignature = "(I)V"
            naming.reentryInhibition = ReentryInhibition.DEF
            val naming = naming.namingElements
            naming.clear()
            naming.add(ClassNameElement(ClassNameElement.PackageMode.NONE))
            naming.add(TextElement("."))
            naming.add(MethodNameElement())
            naming.add(TextElement(": "))
            naming.add(MethodParameterElement(0, ""))
        })

        rootConfig.transactionSettings.transactionDefs.add(MatchedTransactionDef().apply {
            initTiming(this)
            id = 101
            initDefault()
            interceptionTarget = MatchedTransactionDef.InterceptionTarget.METHOD
            isInterceptSubclasses = false
            declaringClassName = Reentry2::class.java.name
            methodName = "m2"
            methodSignature = "()V"
            naming.reentryInhibition = ReentryInhibition.GROUP
            naming.group.usedValue = "group1"
            val naming = naming.namingElements
            naming.clear()
            naming.add(ClassNameElement(ClassNameElement.PackageMode.NONE))
            naming.add(TextElement("."))
            naming.add(MethodNameElement())
        })

        rootConfig.transactionSettings.transactionDefs.add(MatchedTransactionDef().apply {
            initTiming(this)
            id = 102
            initDefault()
            interceptionTarget = MatchedTransactionDef.InterceptionTarget.METHOD
            isInterceptSubclasses = true
            declaringClassName = Reentry2::class.java.name
            methodName = "m4"
            methodSignature = "(I)V"
            naming.reentryInhibition = ReentryInhibition.NAME
            val naming = naming.namingElements
            naming.clear()
            naming.add(ClassNameElement(ClassNameElement.PackageMode.NONE))
            naming.add(TextElement("."))
            naming.add(MethodNameElement())
            naming.add(TextElement(": "))
            naming.add(MethodParameterElement(0, ""))
        })

        rootConfig.transactionSettings.transactionDefs.add(MatchedTransactionDef().apply {
            initTiming(this)
            id = 103
            initDefault()
            interceptionTarget = MatchedTransactionDef.InterceptionTarget.CLASS
            isInterceptSubclasses = true
            declaringClassName = Reentry3::class.java.name
            naming.reentryInhibition = ReentryInhibition.DEF
            val naming = naming.namingElements
            naming.clear()
            naming.add(ClassNameElement(ClassNameElement.PackageMode.NONE))
            naming.add(TextElement("."))
            naming.add(MethodNameElement())
        })
    }

    override fun connect(vmManager: TestVmManager, serverConnection: TestServerConnection, controller: Controller) {
        val comparator = TransactionTreeComparator(TimeComparator.NONE)
        waitForConnection(serverConnection, listOf("JVM"))

        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 1, true, comparator)

        modifyCurrentRootConfig(serverConnection) { rootConfig ->
            val transactionDefs = rootConfig.transactionSettings.transactionDefs
            (transactionDefs.first { it.id == 103L } as MatchedTransactionDef).apply {
                methodInterceptionMode = MatchedTransactionDef.MethodInterceptionMode.ALL_PUBLIC
                isStaticMethods = true
            }

            transactionDefs.first { it.id == 100L }.apply {
                naming.reentryInhibition = ReentryInhibition.TYPE
            }

            transactionDefs.first { it.id == 102L }.apply {
                naming.reentryInhibition = ReentryInhibition.DEF
            }
        }

        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 2, true, comparator)

        modifyCurrentRootConfig(serverConnection) { rootConfig ->
            (rootConfig.transactionSettings.transactionDefs.first { it.id == 103L } as MatchedTransactionDef).apply {
                methodInterceptionMode = MatchedTransactionDef.MethodInterceptionMode.ALL_PUBLIC
                isStaticMethods = false
            }

            rootConfig.transactionSettings.transactionDefs.first { it.id == 100L }.apply {
                naming.reentryInhibition = ReentryInhibition.ALL
            }
        }

        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 3, true, comparator)

        modifyCurrentRootConfig(serverConnection) { rootConfig ->
            (rootConfig.transactionSettings.transactionDefs.first { it.id == 103L } as MatchedTransactionDef).apply {
                methodInterceptionMode = MatchedTransactionDef.MethodInterceptionMode.ALL_PUBLIC
                isStaticMethods = true
                naming.reentryInhibition = ReentryInhibition.NAME
            }
        }

        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 4, true, comparator)
    }

}
