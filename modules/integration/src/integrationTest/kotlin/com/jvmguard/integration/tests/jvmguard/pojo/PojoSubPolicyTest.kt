package com.jvmguard.integration.tests.jvmguard.pojo

import com.jvmguard.agent.config.transactions.*
import com.jvmguard.agent.config.transactions.naming.ClassNameElement
import com.jvmguard.agent.config.transactions.naming.InstanceClassNameElement
import com.jvmguard.agent.config.transactions.naming.MethodNameElement
import com.jvmguard.agent.config.transactions.naming.TextElement
import com.jvmguard.integration.JvmGuardTest
import com.jvmguard.integration.Controller
import com.jvmguard.integration.TestServerConnection
import com.jvmguard.integration.TestVmManager
import com.jvmguard.integration.tests.jvmguard.pojo.classes.policy.Policy1
import com.jvmguard.integration.util.TimeComparator
import com.jvmguard.integration.util.TransactionTreeComparator
import com.jvmguard.data.config.GroupConfig
import com.jvmguard.data.transactions.TransactionDataType
import com.jvmguard.data.transactions.TransactionTreeInterval
import java.util.*

class PojoSubPolicyTest : JvmGuardTest() {

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
            naming.add(InstanceClassNameElement(ClassNameElement.PackageMode.NONE))
            naming.add(TextElement("."))
            naming.add(MethodNameElement())
            baseInit(this)

            policySubDefs.add(PolicySubDef(this).apply {
                id = 1000
                val policy = baseInit(this)
                policy.verySlowValue = 500
                filter = "*m2"
            })

            policySubDefs.add(PolicySubDef(this).apply {
                id = 1001
                policy = baseInit(this)
                policy.slowValue = 500
                policy.verySlowValue = 1000
                policy.overdueValue = 10000
                filter = ".*_[0-9]\\..*"
                comparisonType = ComparisonType.REGEX
            })
        })
    }

    private fun baseInit(policyDef: PolicyDef) = policyDef.policy.apply {
        slowDurationType = DurationType.MILLIS
        slowValue = 100
        verySlowDurationType = DurationType.MILLIS
        verySlowValue = 300
        overdueValue = 1000
    }

    override fun connect(vmManager: TestVmManager, serverConnection: TestServerConnection, controller: Controller) {
        val comparator = TransactionTreeComparator(TimeComparator.THIRTY_PERCENT)
        waitForConnection(serverConnection, listOf("JVM"))

        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, EnumSet.of(TransactionDataType.TRANSACTION, TransactionDataType.OVERDUE), 1, true, comparator)

        modifyCurrentRootConfig(serverConnection) { rootConfig ->
            rootConfig.transactionSettings.transactionDefs.first { it.id == 100L }.apply {
                policySubDefs.first { it.id == 1000L }.policy.isLoggedWarningAsError = true

                val policy = policySubDefs.first { it.id == 1001L }.policy
                policy.isRuntimeExceptionAsError = false
                policy.slowValue = 10000
                policy.verySlowValue = 10000

                policySubDefs.add(0, PolicySubDef(this).apply {
                    filter = "Policy1Sub1*"
                    isDiscard = true
                })
            }
        }

        waitForNextConfigRequest(serverConnection)
        checkTree(serverConnection, TransactionTreeInterval.HOUR, EnumSet.of(TransactionDataType.TRANSACTION, TransactionDataType.OVERDUE), 2, true, comparator)
    }
}
