package dev.jvmguard.integration.tests.jvmguard.matched

import dev.jvmguard.agent.config.transactions.MatchedTransactionDef
import dev.jvmguard.agent.config.transactions.naming.*
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

class MatchedNamingTest : JvmGuardTest() {

    override fun getJvmGuardOptions(runNo: Int, vmNo: Int, libraryNo: Int) = super.getJvmGuardOptions(runNo, vmNo, libraryNo) + " -Xmx64m"

    override fun modifyInitialRootConfig(rootConfig: GroupConfig) {
        rootConfig.transactionSettings.transactionDefs.add(MatchedTransactionDef().apply {
            id = 100
            initDefault()
            interceptionTarget = MatchedTransactionDef.InterceptionTarget.METHOD
            isInterceptSubclasses = true
            declaringClassName = Name1::class.java.name
            methodName = "invoke"
            methodSignature = "(Ldev/jvmguard/integration/tests/jvmguard/matched/classes/ParameterClass;Ljava/lang/String;I)Ljava/lang/String;"
            val naming = naming.namingElements
            naming.clear()
            naming.add(ClassNameElement(ClassNameElement.PackageMode.NONE))
            naming.add(TextElement("."))
            naming.add(MethodNameElement())
            naming.add(TextElement(": "))
            naming.add(ClassNameElement(ClassNameElement.PackageMode.ABBREVIATED))
            naming.add(TextElement("#"))
            naming.add(ClassNameElement(ClassNameElement.PackageMode.FULL))
            naming.add(TextElement("#"))
            naming.add(InstanceClassNameElement(ClassNameElement.PackageMode.NONE))
            naming.add(TextElement("#"))
            naming.add(InstanceClassNameElement(ClassNameElement.PackageMode.ABBREVIATED))
            naming.add(TextElement("#"))
            naming.add(InstanceClassNameElement(ClassNameElement.PackageMode.FULL))
            naming.add(TextElement("#"))
            naming.add(MethodNameElement())
            naming.add(TextElement("#"))
            naming.add(InstanceElement("instanceField.getClass().name"))
            naming.add(TextElement("#"))
            naming.add(InstanceElement("instanceField.getClass().abbrevName"))
            naming.add(TextElement("#"))
            naming.add(InstanceElement("instanceField.getClass().simpleName"))
            naming.add(TextElement("#"))
            naming.add(InstanceElement("instanceField.getClass().getName().nonexistent"))
            naming.add(TextElement("#"))
            naming.add(InstanceElement("nonexistent().getName()"))
        })

        rootConfig.transactionSettings.transactionDefs.add(MatchedTransactionDef().apply {
            id = 101
            initDefault()
            interceptionTarget = MatchedTransactionDef.InterceptionTarget.METHOD
            isInterceptSubclasses = true
            declaringClassName = Name1::class.java.name
            methodName = "invoke2"
            methodSignature = "(Ldev/jvmguard/integration/tests/jvmguard/matched/classes/ParameterClass;Ljava/lang/String;I)Ljava/lang/String;"
            val naming = naming.namingElements
            naming.clear()
            naming.add(ClassNameElement(ClassNameElement.PackageMode.NONE))
            naming.add(TextElement("."))
            naming.add(MethodNameElement())
            naming.add(TextElement(": "))
            naming.add(MethodParameterElement(0, "getClass().abbrevName"))
            naming.add(TextElement("#"))
            naming.add(MethodParameterElement(0, "getBaseValue()"))
            naming.add(TextElement("#"))
            naming.add(MethodParameterElement(0, "getSecondary().obj"))
            naming.add(TextElement("#"))
            naming.add(MethodParameterElement(0, "getSecondary().getValue()"))
            naming.add(TextElement("#"))
            naming.add(MethodParameterElement(0, "getSecondary().getValue()."))
            naming.add(TextElement("#"))
            naming.add(MethodParameterElement(0, "i"))
            naming.add(TextElement("#"))
            naming.add(MethodParameterElement(1, ""))
            naming.add(TextElement("#"))
            naming.add(MethodParameterElement(2, ""))
            naming.add(TextElement("#"))
            naming.add(MethodParameterElement(2, "nonexistent.nonexistent."))
            naming.add(MethodParameterElement(5, "nonexistent.nonexistent."))
            naming.add(TextElement("#"))
            naming.add(MethodParameterElement(2, "nonexistent.nonexistent"))
        })

        rootConfig.transactionSettings.transactionDefs.add(MatchedTransactionDef().apply {
            id = 102
            initDefault()
            interceptionTarget = MatchedTransactionDef.InterceptionTarget.METHOD
            isInterceptSubclasses = false
            declaringClassName = Name1::class.java.name
            methodName = "invoke3"
            methodSignature = "(Ldev/jvmguard/integration/tests/jvmguard/matched/classes/ParameterClass;Ljava/lang/String;I)Ljava/lang/String;"
            val naming = naming.namingElements
            naming.clear()
            naming.add(ClassNameElement(ClassNameElement.PackageMode.NONE))
            naming.add(TextElement("."))
            naming.add(MethodNameElement())
        })

        rootConfig.transactionSettings.transactionDefs.add(MatchedTransactionDef().apply {
            id = 103
            initDefault()
            interceptionTarget = MatchedTransactionDef.InterceptionTarget.CLASS
            methodInterceptionMode = MatchedTransactionDef.MethodInterceptionMode.ALL_PUBLIC
            isStaticMethods = true
            isInterceptSubclasses = true
            declaringClassName = BaseIf1::class.java.name
            val naming = naming.namingElements
            naming.clear()
            naming.add(ClassNameElement(ClassNameElement.PackageMode.ABBREVIATED))
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
            rootConfig.transactionSettings.transactionDefs.first { it.id == 102L }.apply {
                val naming = naming.namingElements
                naming.clear()
                naming.add(ClassNameElement(ClassNameElement.PackageMode.NONE))
                naming.add(TextElement("."))
                naming.add(MethodNameElement())
                naming.add(TextElement(": "))
                naming.add(MethodParameterElement(0, "getSecondary().getValue()"))
            }

            (rootConfig.transactionSettings.transactionDefs.first { it.id == 103L } as MatchedTransactionDef).apply {
                methodInterceptionMode = MatchedTransactionDef.MethodInterceptionMode.IMPLEMENTING_PUBLIC
            }
        }

        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 2, true, comparator)

        modifyCurrentRootConfig(serverConnection) { rootConfig ->
            (rootConfig.transactionSettings.transactionDefs.find { it.id == 100L } as MatchedTransactionDef).apply {
                isInterceptSubclasses = false
            }
        }

        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 3, true, comparator)
    }

}
