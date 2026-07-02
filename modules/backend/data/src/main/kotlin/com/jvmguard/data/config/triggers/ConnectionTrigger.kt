package com.jvmguard.data.config.triggers

open class ConnectionTrigger : Trigger() {

    var startMode: StartMode = StartMode.IMMEDIATELY
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var minimumTimeUnit: TimeUnit = TimeUnit.MINUTES
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

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
        REACHED_ONLY("After the configured threshold has been reached"),
        IMMEDIATELY("Immediately");

        override fun toString(): String = verbose
    }
}
