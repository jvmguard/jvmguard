package dev.jvmguard.data.config

import dev.jvmguard.agent.config.recording.RecordingOptions
import dev.jvmguard.agent.config.telemetry.TelemetrySettings
import dev.jvmguard.agent.config.transactions.TransactionSettings
import dev.jvmguard.data.config.guardrails.GuardrailSettings
import dev.jvmguard.data.config.thresholds.Threshold
import dev.jvmguard.data.config.thresholds.Threshold.Target
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

    val guardrailSettings: GuardrailSettings
        get() = groupConfigPath.map { it.guardrailSettings }.firstOrNull { it.isUsed }
            ?: rootConfig.guardrailSettings

    private val rootConfig: GroupConfig
        get() = groupConfigPath.last()
}
