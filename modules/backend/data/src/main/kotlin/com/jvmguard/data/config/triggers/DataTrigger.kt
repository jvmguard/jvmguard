package com.jvmguard.data.config.triggers

abstract class DataTrigger : Trigger() {

    var interval: Interval = Interval.HOUR
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

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
