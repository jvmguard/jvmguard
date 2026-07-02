package com.jvmguard.integration.tests.jvmguard.devops

import com.jvmguard.agent.config.transactions.*
import com.jvmguard.agent.config.transactions.naming.MethodNameElement
import com.jvmguard.agent.config.transactions.naming.TextElement
import com.jvmguard.integration.JvmGuardTest
import com.jvmguard.integration.Controller
import com.jvmguard.integration.TestServerConnection
import com.jvmguard.integration.TestVmManager
import com.jvmguard.integration.tests.jvmguard.devops.classes.naming.*
import com.jvmguard.integration.util.TimeComparator
import com.jvmguard.integration.util.TransactionTreeComparator
import com.jvmguard.data.config.GroupConfig
import com.jvmguard.data.transactions.TransactionDataType
import com.jvmguard.data.transactions.TransactionTreeInterval

class DevOpsClassNamingTest : JvmGuardTest() {

    override fun getJvmGuardOptions(runNo: Int, vmNo: Int, libraryNo: Int) = super.getJvmGuardOptions(runNo, vmNo, libraryNo) + " -Xmx64m"

    override fun modifyInitialRootConfig(rootConfig: GroupConfig) {
        rootConfig.transactionSettings.transactionDefs.clear()

        rootConfig.transactionSettings.transactionDefs.add(DevOpsAnnotatedTransactionDef().apply {
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
        rootConfig.transactionSettings.transactionDefs.add(PojoTransactionDef().apply {
            initDefault()
            declaringClassName = className
            methodName = "pojo"
            methodSignature = "()V"
            interceptionTarget = PojoTransactionDef.InterceptionTarget.METHOD
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
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 1, true, comparator)

        modifyCurrentRootConfig(serverConnection) { rootConfig ->
            rootConfig.transactionSettings.transactionDefs.add(0, DevOpsAnnotatedTransactionDef().apply {
                id = 101
                group.usedValue = "class1"
                initDefaultPolicy(policy)
                policy.slowValue = 400
            })
        }

        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 2, true, comparator)

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
        checkTree(serverConnection, TransactionTreeInterval.HOUR, TransactionDataType.TRANSACTION, 3, true, comparator)
    }
}
