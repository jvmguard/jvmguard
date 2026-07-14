package dev.jvmguard.data.config.triggers

import dev.jvmguard.agent.config.base.ConfigDoc

sealed class DataTrigger : Trigger() {

    @field:ConfigDoc("Counting window over which 'count' events are accumulated.")
    var interval: Interval = Interval.HOUR
        set(value) { field = changed(field, value) }

    override fun isIdenticalCounterType(trigger: Trigger): Boolean {
        if (trigger !is DataTrigger || !super.isIdenticalCounterType(trigger)) {
            return false
        }
        if (interval != trigger.interval) {
            return false
        }
        return true
    }
}
