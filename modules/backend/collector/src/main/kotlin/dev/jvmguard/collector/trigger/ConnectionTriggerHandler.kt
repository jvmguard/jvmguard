package dev.jvmguard.collector.trigger

import dev.jvmguard.collector.main.CollectorContext
import dev.jvmguard.data.config.triggers.ConnectionTrigger
import dev.jvmguard.data.config.triggers.ConnectionTrigger.StartMode
import dev.jvmguard.data.vmdata.VM

class ConnectionTriggerHandler(var trigger: ConnectionTrigger, private val groupVm: VM) {

    private var started = false

    private var lastTimeTriggeredNanos = Long.MIN_VALUE
    private var firstTimeReachedNanos = Long.MIN_VALUE

    fun check(nanoTime: Long, count: Int, collectorContext: CollectorContext) {
        if (!started) {
            if (trigger.startMode == StartMode.IMMEDIATELY) {
                started = true
            } else if (count >= trigger.count) {
                started = true
            }
        }

        if (started && count < trigger.count) {
            if (firstTimeReachedNanos == Long.MIN_VALUE) {
                firstTimeReachedNanos = nanoTime
            }

            if (nanoTime - firstTimeReachedNanos >= trigger.minimumTimeUnit.getNanos(trigger.minimumTime) &&
                (lastTimeTriggeredNanos == Long.MIN_VALUE || nanoTime - lastTimeTriggeredNanos >= trigger.inhibitionInterval.getNanos(trigger.inhibitionTime))
            ) {
                lastTimeTriggeredNanos = nanoTime
                TriggerHandler.executeActions(collectorContext, trigger.triggerActions, null, groupVm, trigger.description)
            }
        } else {
            firstTimeReachedNanos = Long.MIN_VALUE
        }
    }
}
