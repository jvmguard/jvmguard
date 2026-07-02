package com.jvmguard.integration.tests.jvmguard.trigger.policy

import com.jvmguard.agent.config.base.LogCategory
import com.jvmguard.agent.config.transactions.ComparisonType
import com.jvmguard.agent.config.transactions.DevOpsAnnotatedTransactionDef
import com.jvmguard.agent.config.transactions.DurationType
import com.jvmguard.integration.JvmGuardTest
import com.jvmguard.integration.Controller
import com.jvmguard.integration.TestServerConnection
import com.jvmguard.integration.TestVmManager
import com.jvmguard.integration.config.VMConfig
import com.jvmguard.data.config.GroupConfig
import com.jvmguard.data.config.triggers.PolicyTrigger
import com.jvmguard.data.config.triggers.Trigger
import com.jvmguard.data.config.triggers.actions.HeapDumpAction
import com.jvmguard.data.config.triggers.actions.LogAction
import com.jvmguard.data.config.triggers.actions.ThreadDumpAction
import com.jvmguard.data.file.SnapshotFileType

class FindLastPolicyVmTest : JvmGuardTest() {
    override fun getJvmGuardOptions(runNo: Int, vmNo: Int, libraryNo: Int) = super.getJvmGuardOptions(runNo, vmNo, libraryNo) + " -Xmx64m"
    override fun getVmCount(vmConfig: VMConfig, runNo: Int) = 10

    override fun modifyInitialRootConfig(rootConfig: GroupConfig) {
        rootConfig.transactionSettings.transactionDefs.first { it is DevOpsAnnotatedTransactionDef }.policy.apply {
            slowDurationType = DurationType.MILLIS
            slowValue = 350
            verySlowDurationType = DurationType.MILLIS
            verySlowValue = 500
            overdueValue = 750
        }

        rootConfig.triggerSettings.triggers.add(PolicyTrigger().apply {
            id = 1
            filter = "*methodOne"
            isOverdue = true
            isVerySlow = false
            count = 5
            interval = Trigger.Interval.NONE
            triggerActions.add(ThreadDumpAction())
            triggerActions.add(LogAction(LogCategory.INFO, "overdue trigger"))
        })

        rootConfig.triggerSettings.triggers.add(PolicyTrigger().apply {
            id = 2
            filter = ".*methodOne"
            comparisonType = ComparisonType.REGEX
            isVerySlow = false
            isSlow = true
            count = 3
            interval = Trigger.Interval.HOUR
            triggerActions.add(HeapDumpAction())
            triggerActions.add(LogAction(LogCategory.INFO, "slow trigger"))
        })
    }

    override fun connect(vmManager: TestVmManager, serverConnection: TestServerConnection, controller: Controller) {
        waitForConnections(serverConnection)

        sleep((2.5 * 60 * 1000).toLong())

        val snapshotFiles = serverConnection.getSnapshotFiles(null, null)
        assertEqual(snapshotFiles.size, 2)
        println(snapshotFiles)
        assertTrue(snapshotFiles.find { it.type == SnapshotFileType.THREAD_DUMP && it.vm.name == "JVM6" } != null)
        assertTrue(snapshotFiles.find { it.type == SnapshotFileType.HPZ && it.vm.name == "JVM4" } != null)
    }
}
