package dev.jvmguard.collector.vmdata.components

import dev.jvmguard.collector.main.CollectorContext
import dev.jvmguard.collector.trigger.PolicyTriggerHandler
import dev.jvmguard.data.vmdata.VM

internal class TransactionTriggerEntry(triggerHandler: Collection<PolicyTriggerHandler>, transactionName: String) {

    private var triggerHandler: MutableList<PolicyTriggerHandler>? = null

    init {
        updateTrigger(triggerHandler, transactionName)
    }

    fun triggerCount(snapshotTimeStamp: Long, nanoTime: Long, count: Count, vm: VM, collectorContext: CollectorContext) {
        triggerHandler?.let { handlers ->
            for (handler in handlers) {
                val value = count.getValue(handler.getTrigger())
                if (value > 0) {
                    handler.addEvents(snapshotTimeStamp, nanoTime, value, vm, collectorContext)
                }
            }
        }
    }

    fun updateTrigger(triggers: Collection<PolicyTriggerHandler>, transactionName: String) {
        triggerHandler?.clear()
        for (triggerHandler in triggers) {
            if (triggerHandler.getTrigger().matches(transactionName)) {
                if (this.triggerHandler == null) {
                    this.triggerHandler = ArrayList(2)
                }
                this.triggerHandler!!.add(triggerHandler)
            }
        }
    }
}
