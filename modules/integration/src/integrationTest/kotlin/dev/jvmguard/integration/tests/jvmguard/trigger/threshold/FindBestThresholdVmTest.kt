package dev.jvmguard.integration.tests.jvmguard.trigger.threshold

import dev.jvmguard.agent.AgentConstants
import dev.jvmguard.agent.config.base.LogCategory
import dev.jvmguard.integration.JvmGuardTest
import dev.jvmguard.integration.Controller
import dev.jvmguard.integration.TestServerConnection
import dev.jvmguard.integration.TestVmManager
import dev.jvmguard.integration.config.VMConfig
import dev.jvmguard.data.config.GroupConfig
import dev.jvmguard.data.config.thresholds.Threshold
import dev.jvmguard.data.config.triggers.ThresholdTrigger
import dev.jvmguard.data.config.triggers.Trigger
import dev.jvmguard.data.config.triggers.actions.HeapDumpAction
import dev.jvmguard.data.config.triggers.actions.LogAction
import dev.jvmguard.data.config.triggers.actions.ThreadDumpAction
import dev.jvmguard.data.file.SnapshotFileType
import dev.jvmguard.data.vmdata.PersistentTelemetryIdentifier
import dev.jvmguard.data.vmdata.ThresholdIdentifier

class FindBestThresholdVmTest : JvmGuardTest() {
    override fun getJvmGuardOptions(runNo: Int, vmNo: Int, libraryNo: Int) = super.getJvmGuardOptions(runNo, vmNo, libraryNo) + " -Xmx64m"
    override fun getVmCount(vmConfig: VMConfig, runNo: Int) = 10

    override fun modifyInitialRootConfig(rootConfig: GroupConfig) {
        val identifier = PersistentTelemetryIdentifier("cu", "", AgentConstants.TELEMETRY_TYPE_DECLARED, "continuous")

        rootConfig.thresholdSettings.thresholds.add(Threshold().apply {
            telemetryIdentifier = identifier
            upperBound = 15
            isUpperBoundEnabled = true
            minimumTime = 0
            inhibitDuplicateTime = 0
            isInhibitDuplicateForContinuousViolation = false
            target = Threshold.Target.GROUP
        })

        rootConfig.thresholdSettings.thresholds.add(Threshold().apply {
            telemetryIdentifier = identifier
            lowerBound = 45
            isLowerBoundEnabled = true
            minimumTime = 0
            inhibitDuplicateTime = 0
            isInhibitDuplicateForContinuousViolation = false
            target = Threshold.Target.GROUP
            customName.usedValue = "lower"
        })

        rootConfig.triggerSettings.triggers.add(ThresholdTrigger().apply {
            thresholdIdentifier = ThresholdIdentifier(identifier)
            interval = Trigger.Interval.HOUR
            count = 8 // should hit after approx 2 minutes
            triggerActions.add(ThreadDumpAction())
            triggerActions.add(LogAction(LogCategory.WARNING, "thread"))
        })

        rootConfig.triggerSettings.triggers.add(ThresholdTrigger().apply {
            thresholdIdentifier = ThresholdIdentifier(identifier, "lower")
            interval = Trigger.Interval.NONE
            count = 3 // should hit after approx 30 seconds
            triggerActions.add(HeapDumpAction())
            triggerActions.add(LogAction(LogCategory.WARNING, "heap"))
        })
    }

    override fun connect(vmManager: TestVmManager, serverConnection: TestServerConnection, controller: Controller) {
        waitForConnections(serverConnection)

        sleep((3.5 * 60 * 1000).toLong())

        val snapshotFiles = serverConnection.getSnapshotFiles(null, null)
        assertEqual(snapshotFiles.size, 2)
        println(snapshotFiles)
        assertTrue(snapshotFiles.find { it.type == SnapshotFileType.THREAD_DUMP && it.vm.name == "JVM6" } != null)
        assertTrue(snapshotFiles.find { it.type == SnapshotFileType.HPZ && it.vm.name == "JVM3" } != null)
    }
}
