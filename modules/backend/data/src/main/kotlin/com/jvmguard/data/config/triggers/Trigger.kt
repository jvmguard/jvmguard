package com.jvmguard.data.config.triggers

import com.jvmguard.data.base.StoredConfig
import com.jvmguard.data.config.triggers.actions.TriggerAction

abstract class Trigger protected constructor() : StoredConfig(), Cloneable {

    private var enabled: Boolean = true

    var isEnabled: Boolean
        get() = enabled
        set(value) {
            val old = enabled
            enabled = value
            fireChanged(old, value)
        }

    var triggerActions: MutableList<TriggerAction> = ArrayList()

    var count: Int = 10
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var inhibitionInterval: Interval = Interval.HOUR
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var inhibitionTime: Int = 12
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    abstract val triggerType: TriggerType

    abstract val description: String

    public override fun clone(): Trigger {
        val copy = super.clone() as Trigger
        copy.triggerActions = triggerActions.mapTo(ArrayList(triggerActions.size)) { it.clone() }
        return copy
    }

    open fun isIdenticalCounterType(trigger: Trigger): Boolean {
        if (this === trigger) {
            return true
        }
        if (inhibitionTime != trigger.inhibitionTime) {
            return false
        }
        if (inhibitionInterval != trigger.inhibitionInterval) {
            return false
        }
        return true
    }

    enum class Interval(
        private val verbose: String,
        val multipleVerbose: String,
        private val secondMultiplier: Int,
    ) {
        MINUTE("minute", "minutes", 60),
        HOUR("hour", "hours", 60 * 60),
        DAY("day", "days", 60 * 60 * 24),
        NONE("", "", 0);

        fun getSeconds(time: Int): Int = secondMultiplier * time

        fun getMillis(time: Int): Long = getSeconds(time).toLong() * 1000

        fun getNanos(time: Int): Long = getMillis(time) * 1000 * 1000

        override fun toString(): String = verbose

    }
}
