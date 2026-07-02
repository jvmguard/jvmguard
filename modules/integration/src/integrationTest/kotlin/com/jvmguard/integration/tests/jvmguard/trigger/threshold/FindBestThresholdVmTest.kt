package com.jvmguard.integration.tests.jvmguard.trigger.threshold

import com.jvmguard.agent.AgentConstants
import com.jvmguard.agent.config.base.LogCategory
import com.jvmguard.integration.JvmGuardTest
import com.jvmguard.integration.Controller
import com.jvmguard.integration.TestServerConnection
import com.jvmguard.integration.TestVmManager
import com.jvmguard.integration.config.VMConfig
import com.jvmguard.data.config.GroupConfig
import com.jvmguard.data.config.thresholds.Threshold
import com.jvmguard.data.config.triggers.ThresholdTrigger
import com.jvmguard.data.config.triggers.Trigger
import com.jvmguard.data.config.triggers.actions.HeapDumpAction
import com.jvmguard.data.config.triggers.actions.LogAction
import com.jvmguard.data.config.triggers.actions.ThreadDumpAction
import com.jvmguard.data.file.SnapshotFileType
import com.jvmguard.data.vmdata.PersistentTelemetryIdentifier
import com.jvmguard.data.vmdata.ThresholdIdentifier

class FindBestThresholdVmTest : JvmGuardTest() {
    override fun getJvmGuardOptions(runNo: Int, vmNo: Int, libraryNo: Int) = super.getJvmGuardOptions(runNo, vmNo, libraryNo) + " -Xmx64m"
    override fun getVmCount(vmConfig: VMConfig, runNo: Int) = 10

    override fun modifyInitialRootConfig(rootConfig: GroupConfig) {
        val identifier = PersistentTelemetryIdentifier("cu", "", AgentConstants.TELEMETRY_TYPE_DEVOPS, "continuous")

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
