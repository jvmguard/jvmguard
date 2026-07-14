package dev.jvmguard.collector.trigger

import dev.jvmguard.collector.main.CollectorContext
import dev.jvmguard.collector.threshold.ViolationEvent
import dev.jvmguard.collector.vmdata.AbstractVmData.TelemetryValueVisitor
import dev.jvmguard.collector.vmdata.VmGroupData
import dev.jvmguard.data.config.triggers.ThresholdTrigger
import dev.jvmguard.data.vmdata.VM

class ThresholdTriggerHandler(trigger: ThresholdTrigger, private val groupData: VmGroupData) :
    TriggerHandler(trigger, groupData.vm) {

    private var lastEvent: ViolationEvent? = null

    private val bestTelemetryValueVisitor = BestTelemetryValueVisitor()

    @Synchronized
    fun addEvents(snapshotTimeStamp: Long, nanoTime: Long, violationEvent: ViolationEvent, vm: VM, collectorContext: CollectorContext) {
        lastEvent = violationEvent
        super.addEvents(snapshotTimeStamp, nanoTime, 1, vm, collectorContext)
    }

    override fun executeActions(snapshotTimeStamp: Long, collectorContext: CollectorContext) {
        val currentLastVm = lastVm
        val event = lastEvent
        var usedLastVm = currentLastVm
        val telemetryIdentifier = event?.thresholdIdentifier?.telemetryIdentifier
        if (currentLastVm != null && currentLastVm.isGroupNode && telemetryIdentifier != null) {
            groupData.visitConnectedVmTelemetryValue(telemetryIdentifier, bestTelemetryValueVisitor.init())
            usedLastVm = bestTelemetryValueVisitor.bestVm
        }
        executeActions(collectorContext, triggerData.triggerActions, usedLastVm, groupVm, triggerData.description)
    }

    private inner class BestTelemetryValueVisitor : TelemetryValueVisitor {
        var bestVm: VM? = null
        private var bestValue: Long = 0

        fun init(): BestTelemetryValueVisitor {
            bestVm = null
            bestValue = if (lastEvent!!.isTooHigh) Long.MIN_VALUE else Long.MAX_VALUE
            return this
        }

        override fun visit(vm: VM, value: Long) {
            if (bestVm == null) {
                bestVm = vm
            }
            if (value != Long.MIN_VALUE) {
                if (lastEvent!!.isTooHigh) {
                    if (value > bestValue) {
                        bestValue = value
                        bestVm = vm
                    }
                } else {
                    if (value < bestValue) {
                        bestValue = value
                        bestVm = vm
                    }
                }
            }
        }
    }
}
