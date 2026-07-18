package dev.jvmguard.integration.tests.jvmguard.declared

import dev.jvmguard.agent.config.transactions.*
import dev.jvmguard.agent.config.transactions.naming.MethodNameElement
import dev.jvmguard.agent.config.transactions.naming.TextElement
import dev.jvmguard.integration.JvmGuardTest
import dev.jvmguard.integration.Controller
import dev.jvmguard.integration.TestServerConnection
import dev.jvmguard.integration.TestVmManager
import dev.jvmguard.integration.tests.jvmguard.declared.classes.naming.*
import dev.jvmguard.integration.util.TimeComparator
import dev.jvmguard.integration.util.TransactionTreeComparator
import dev.jvmguard.data.config.GroupConfig
import dev.jvmguard.data.transactions.TransactionDataType
import dev.jvmguard.data.transactions.TransactionTreeInterval

class DeclaredClassNamingTest : JvmGuardTest() {

    override fun getJvmGuardOptions(runNo: Int, vmNo: Int, libraryNo: Int) = super.getJvmGuardOptions(runNo, vmNo, libraryNo) + " -Xmx64m"

    override fun modifyInitialRootConfig(rootConfig: GroupConfig) {
        rootConfig.transactionSettings.transactionDefs.clear()

        rootConfig.transactionSettings.transactionDefs.add(DeclaredTransactionDef().apply {
            id = 100
            initDefault()
            initDefaultPolicy(policy)
        })

        addPojoTransaction(rootConfig, ClassNaming3::class.java.name)
        addPojoTransaction(rootConfig, ClassNaming4::class.java.name)
        addPojoTransaction(rootConfig, ClassNaming5::class.java.name)
        addPojoTransaction(rootConfig, ClassNaming6::class.java.name)
        addPojoTransaction(rootConfig, ClassNaming7::class.java.name)
        addPojoTransaction(rootConfig, ClassNaming8::class.java.name)

    }

    private fun addPojoTransaction(rootConfig: GroupConfig, className: String) {
        rootConfig.transactionSettings.transactionDefs.add(MatchedTransactionDef().apply {
            initDefault()
            declaringClassName = className
            methodName = "pojo"
            methodSignature = "()V"
            interceptionTarget = MatchedTransactionDef.InterceptionTarget.METHOD
            naming.group.usedValue = "class1"
            naming.namingElements.add(TextElement(" "))
            naming.namingElements.add(MethodNameElement())
            initDefaultPolicy(policy)
        })
    }

    private fun initDefaultPolicy(policy: Policy) {
        policy.slowValue = 1000
        policy.slowDurationType = DurationType.MILLIS
        policy.verySlowValue = 10000
        policy.verySlowDurationType = DurationType.MILLIS
    }

    override fun connect(vmManager: TestVmManager, serverConnection: TestServerConnection, controller: Controller) {
        val comparator = TransactionTreeComparator(TimeComparator.NONE)
        waitForConnection(serverConnection, listOf("JVM"))

        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 1, comparator)

        modifyCurrentRootConfig(serverConnection) { rootConfig ->
            rootConfig.transactionSettings.transactionDefs.add(0, DeclaredTransactionDef().apply {
                id = 101
                group.usedValue = "class1"
                initDefaultPolicy(policy)
                policy.slowValue = 400
            })
        }

        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 2, comparator)

        modifyCurrentRootConfig(serverConnection) { rootConfig ->
            rootConfig.transactionSettings.transactionDefs.find { it.id == 101L }!!.apply {
                policySubDefs.add(PolicySubDef(this).apply {
                    filter = "*inner*"
                    isDiscard = true
                })
                policySubDefs.add(PolicySubDef(this).apply {
                    filter = "*m1"
                    initDefaultPolicy(policy)
                })
            }
        }

        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 3, comparator)
    }
}
