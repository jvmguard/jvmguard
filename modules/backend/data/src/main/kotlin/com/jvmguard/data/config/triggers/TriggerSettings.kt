package com.jvmguard.data.config.triggers

import com.jvmguard.data.base.StoredConfig

open class TriggerSettings : StoredConfig(), Comparable<TriggerSettings> {

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

    private var lastId: Long = 0

    val activeTriggerCount: Int
        get() = triggers.count { it.isEnabled }

    override fun compareTo(other: TriggerSettings): Int = activeTriggerCount - other.activeTriggerCount
}
