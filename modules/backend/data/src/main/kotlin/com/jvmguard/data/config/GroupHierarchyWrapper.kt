package com.jvmguard.data.config

import com.jvmguard.agent.config.recording.RecordingOptions
import com.jvmguard.agent.config.telemetry.TelemetrySettings
import com.jvmguard.agent.config.transactions.TransactionSettings
import com.jvmguard.data.config.thresholds.Threshold
import com.jvmguard.data.config.thresholds.Threshold.Target
import java.util.*

class GroupHierarchyWrapper(private val groupConfigPath: LinkedList<GroupConfig>) {

    val transactionSettings: TransactionSettings
        get() = groupConfigPath.map { it.transactionSettings }.firstOrNull { it.isUsed }
            ?: rootConfig.transactionSettings

    val telemetrySettings: TelemetrySettings
        get() = groupConfigPath.map { it.telemetrySettings }.firstOrNull { it.isUsed }
            ?: rootConfig.telemetrySettings

    val vmThresholds: List<Threshold>
        get() = groupConfigPath
            .flatMap { it.thresholdSettings.thresholds }
            .filter { it.target == Target.SINGLE_VMS }

    val recordingOptions: RecordingOptions
        get() = groupConfigPath.map { it.recordingOptions }.firstOrNull { it.isUsed }
            ?: rootConfig.recordingOptions

    private val rootConfig: GroupConfig
        get() = groupConfigPath.last()
}
