package com.jvmguard.data.config.triggers

import com.jvmguard.agent.config.base.ConfigDoc
import com.jvmguard.data.base.PolymorphicJson
import com.jvmguard.data.base.StoredConfig
import com.jvmguard.data.config.triggers.actions.TriggerAction

@PolymorphicJson
sealed class Trigger protected constructor() : StoredConfig(), Cloneable {

    @field:ConfigDoc("Whether the trigger is active.")
    private var enabled: Boolean = true

    var isEnabled: Boolean
        get() = enabled
        set(value) { enabled = changed(enabled, value) }

    @field:ConfigDoc("Actions executed when the trigger fires.")
    var triggerActions: MutableList<TriggerAction> = ArrayList()

    @field:ConfigDoc("Number of qualifying events required to fire the trigger.")
    var count: Int = 10
        set(value) { field = changed(field, value) }

    @field:ConfigDoc("Time window for the inhibition/rate limit.")
    var inhibitionInterval: Interval = Interval.HOUR
        set(value) { field = changed(field, value) }

    @field:ConfigDoc("Duration (in inhibitionInterval units) during which re-firing is suppressed.")
    var inhibitionTime: Int = 12
        set(value) { field = changed(field, value) }

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
        @ConfigDoc("Per-minute window.")
        MINUTE("minute", "minutes", 60),
        @ConfigDoc("Per-hour window.")
        HOUR("hour", "hours", 60 * 60),
        @ConfigDoc("Per-day window.")
        DAY("day", "days", 60 * 60 * 24),
        @ConfigDoc("No time window (absolute count).")
        NONE("", "", 0);

        fun getSeconds(time: Int): Int = secondMultiplier * time

        fun getMillis(time: Int): Long = getSeconds(time).toLong() * 1000

        fun getNanos(time: Int): Long = getMillis(time) * 1000 * 1000

        override fun toString(): String = verbose

    }
}
