package dev.jvmguard.data.config.triggers

import dev.jvmguard.agent.config.base.ConfigDoc
import dev.jvmguard.data.base.StoredConfig

open class TriggerSettings : StoredConfig(), Comparable<TriggerSettings> {

    @field:ConfigDoc("The configured triggers (polymorphic: connection-count, policy, threshold).")
    var triggers: MutableList<Trigger> = ArrayList()
        set(value) {
            field = value
            for (trigger in value) {
                if (trigger.id == null) {
                    trigger.id = ++lastId
                }
            }
            fireChanged(false, true) // always fire, so only call setter if changed
        }

    @field:ConfigDoc(
        "Whether completed captures (heap dump, thread dump, JFR snapshot, JProfiler recording) are emitted " +
            "as OpenTelemetry log records into the monitored application's OTel pipeline. No-op when the application " +
            "has no OpenTelemetry SDK. Only the setting on the root group applies."
    )
    var emitOtelCaptureEvents: Boolean = true
        set(value) {
            field = value
            fireChanged()
        }

    @field:ConfigDoc("Internal counter for assigning ids to newly added triggers.")
    private var lastId: Long = 0

    val activeTriggerCount: Int
        get() = triggers.count { it.isEnabled }

    override fun compareTo(other: TriggerSettings): Int = activeTriggerCount - other.activeTriggerCount
}
