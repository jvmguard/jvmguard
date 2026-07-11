package com.jvmguard.data.config.triggers

import com.jvmguard.agent.config.base.ConfigDoc

@ConfigDoc("Fires on the number of connected VMs in the group.")
open class ConnectionTrigger : Trigger() {

    @field:ConfigDoc("Whether the trigger may fire immediately or only after the connection count is first reached.")
    var startMode: StartMode = StartMode.IMMEDIATELY
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    @field:ConfigDoc("Time unit for minimumTime.")
    var minimumTimeUnit: TimeUnit = TimeUnit.MINUTES
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    @field:ConfigDoc("Minimum connection duration before a VM connection counts toward the trigger.")
    var minimumTime: Int = 1
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    override val triggerType: TriggerType
        get() = TriggerType.CONNECTION

    override val description: String
        get() = "$triggerType: $count"

    override fun isIdenticalCounterType(trigger: Trigger): Boolean {
        if (trigger !is ConnectionTrigger || !super.isIdenticalCounterType(trigger)) {
            return false
        }
        if (startMode != trigger.startMode) {
            return false
        }
        if (minimumTimeUnit != trigger.minimumTimeUnit) {
            return false
        }
        if (minimumTime != trigger.minimumTime) {
            return false
        }
        return true
    }

    enum class StartMode(private val verbose: String) {
        @ConfigDoc("Fire only after the configured threshold has been reached.")
        REACHED_ONLY("After the configured threshold has been reached"),
        @ConfigDoc("May fire immediately.")
        IMMEDIATELY("Immediately");

        override fun toString(): String = verbose
    }
}
