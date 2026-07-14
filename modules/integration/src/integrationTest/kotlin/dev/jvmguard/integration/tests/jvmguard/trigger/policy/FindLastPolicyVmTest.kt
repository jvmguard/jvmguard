package dev.jvmguard.integration.tests.jvmguard.trigger.policy

import dev.jvmguard.agent.config.base.LogCategory
import dev.jvmguard.agent.config.transactions.ComparisonType
import dev.jvmguard.agent.config.transactions.DeclaredTransactionDef
import dev.jvmguard.agent.config.transactions.DurationType
import dev.jvmguard.integration.JvmGuardTest
import dev.jvmguard.integration.Controller
import dev.jvmguard.integration.TestServerConnection
import dev.jvmguard.integration.TestVmManager
import dev.jvmguard.integration.config.VMConfig
import dev.jvmguard.data.config.GroupConfig
import dev.jvmguard.data.config.triggers.PolicyTrigger
import dev.jvmguard.data.config.triggers.Trigger
import dev.jvmguard.data.config.triggers.actions.HeapDumpAction
import dev.jvmguard.data.config.triggers.actions.LogAction
import dev.jvmguard.data.config.triggers.actions.ThreadDumpAction
import dev.jvmguard.data.file.SnapshotFileType

class FindLastPolicyVmTest : JvmGuardTest() {
    override fun getJvmGuardOptions(runNo: Int, vmNo: Int, libraryNo: Int) = super.getJvmGuardOptions(runNo, vmNo, libraryNo) + " -Xmx64m"
    override fun getVmCount(vmConfig: VMConfig, runNo: Int) = 10

    override fun modifyInitialRootConfig(rootConfig: GroupConfig) {
        rootConfig.transactionSettings.transactionDefs.first { it is DeclaredTransactionDef }.policy.apply {
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
