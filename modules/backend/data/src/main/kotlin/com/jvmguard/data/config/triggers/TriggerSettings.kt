package com.jvmguard.data.config.triggers

import com.jvmguard.agent.config.base.ConfigDoc
import com.jvmguard.data.base.StoredConfig

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

    @field:ConfigDoc("Internal counter for assigning ids to newly added triggers.")
    private var lastId: Long = 0

    val activeTriggerCount: Int
        get() = triggers.count { it.isEnabled }

    override fun compareTo(other: TriggerSettings): Int = activeTriggerCount - other.activeTriggerCount
}
