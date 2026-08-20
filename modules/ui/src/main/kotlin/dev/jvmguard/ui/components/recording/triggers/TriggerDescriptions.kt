package dev.jvmguard.ui.components.recording.triggers

import dev.jvmguard.data.config.triggers.ConnectionTrigger
import dev.jvmguard.data.config.triggers.PolicyTrigger
import dev.jvmguard.data.config.triggers.ThresholdTrigger
import dev.jvmguard.data.config.triggers.TimeUnit
import dev.jvmguard.data.config.triggers.Trigger
import dev.jvmguard.data.config.triggers.actions.EmailAction
import dev.jvmguard.data.config.triggers.actions.HeapDumpAction
import dev.jvmguard.data.config.triggers.actions.InboxAction
import dev.jvmguard.data.config.triggers.actions.LogAction
import dev.jvmguard.data.config.triggers.actions.RecordArtifactAction
import dev.jvmguard.data.config.triggers.actions.ThreadDumpAction
import dev.jvmguard.data.config.triggers.actions.TriggerAction
import dev.jvmguard.data.config.triggers.actions.WebhookAction
import dev.jvmguard.data.vmdata.TelemetryType
import dev.jvmguard.ui.components.recording.thresholds.telemetryTypeOf
import dev.jvmguard.ui.server.displayName
import dev.jvmguard.ui.server.enumLabel
import dev.jvmguard.ui.server.t

@Suppress("REDUNDANT_ELSE_IN_WHEN")
fun describe(trigger: Trigger, telemetryTypes: () -> Collection<TelemetryType>): String = when (trigger) {
    is ConnectionTrigger -> t("trigger.desc.connection", enumLabel(trigger.triggerType), trigger.count)
    is PolicyTrigger -> describePolicy(trigger)
    is ThresholdTrigger -> describeThreshold(trigger, telemetryTypes)
    else -> trigger.description
}

@Suppress("REDUNDANT_ELSE_IN_WHEN")
fun describe(action: TriggerAction): String = when (action) {
    is RecordArtifactAction ->
        t("trigger.action.desc.record", enumLabel(action.actionType), duration(action.time, action.timeUnit))
    is EmailAction -> t("trigger.action.desc.email", enumLabel(action.actionType), action.email)
    is WebhookAction -> t("trigger.action.desc.webhook", enumLabel(action.actionType), action.url)
    is HeapDumpAction, is ThreadDumpAction, is InboxAction, is LogAction -> enumLabel(action.actionType)
    else -> action.description
}

private fun duration(time: Int, unit: TimeUnit): String =
    t("trigger.action.duration." + unit.name.lowercase(), time)

private fun describePolicy(trigger: PolicyTrigger): String {
    val states = when {
        trigger.isNormal && trigger.isSlow && trigger.isVerySlow && trigger.isError && trigger.isOverdue ->
            t("trigger.desc.state.all")
        !trigger.isNormal && !trigger.isSlow && !trigger.isVerySlow && !trigger.isError && !trigger.isOverdue ->
            t("trigger.desc.state.no")
        else -> buildList {
            if (trigger.isNormal) add(t("trigger.desc.state.normal"))
            if (trigger.isSlow) add(t("trigger.desc.state.slow"))
            if (trigger.isVerySlow) add(t("trigger.desc.state.verySlow"))
            if (trigger.isError) add(t("trigger.desc.state.error"))
            if (trigger.isOverdue) add(t("trigger.desc.state.overdue"))
        }.joinToString(",") // no space: byte-identical to the backend join
    }
    return if (trigger.interval == Trigger.Interval.NONE) {
        t("trigger.desc.policy", enumLabel(trigger.triggerType), trigger.filter, trigger.count, states)
    } else {
        t("trigger.desc.policy.interval", enumLabel(trigger.triggerType), trigger.filter, trigger.count, states,
            enumLabel(trigger.interval))
    }
}

private fun describeThreshold(trigger: ThresholdTrigger, telemetryTypes: () -> Collection<TelemetryType>): String {
    val name = thresholdName(trigger, telemetryTypes)
    return if (trigger.interval == Trigger.Interval.NONE) {
        t("trigger.desc.threshold", enumLabel(trigger.triggerType), name, trigger.count)
    } else {
        t("trigger.desc.threshold.interval", enumLabel(trigger.triggerType), name, trigger.count,
            enumLabel(trigger.interval))
    }
}

private fun thresholdName(trigger: ThresholdTrigger, telemetryTypes: () -> Collection<TelemetryType>): String {
    val identifier = trigger.thresholdIdentifier
    val customName = identifier?.customName
    if (!customName.isNullOrEmpty()) {
        return customName
    }
    val telemetryId = identifier?.telemetryIdentifier
    return telemetryId?.let { telemetryTypeOf(it, telemetryTypes())?.displayName() }
        ?: "\${${ThresholdTrigger.VARIABLE_TYPE_TELEMETRY_ID}:$telemetryId}"
}
