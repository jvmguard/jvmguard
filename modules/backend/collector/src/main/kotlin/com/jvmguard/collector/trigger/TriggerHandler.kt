package com.jvmguard.collector.trigger

import com.jvmguard.agent.config.base.LogCategory
import com.jvmguard.collector.connection.Command
import com.jvmguard.collector.main.CollectorContext
import com.jvmguard.data.config.triggers.DataTrigger
import com.jvmguard.data.config.triggers.Trigger.Interval
import com.jvmguard.data.config.triggers.actions.*
import com.jvmguard.data.vmdata.VM

abstract class TriggerHandler(protected var triggerData: DataTrigger, protected val groupVm: VM) {

    private val inhibitionNanos: Long = triggerData.inhibitionInterval.getNanos(triggerData.inhibitionTime)

    private var lastUpdateNano = Long.MIN_VALUE
    private var lastTriggerNano = Long.MIN_VALUE
    private var frequencyUpdateCount: Long = 0

    private var slices: LongArray = LongArray(0)
    private var sliceMinutes = 0
    private var currentSlice = 0

    private var currentEventCount: Long = 0

    protected var lastVm: VM? = null

    init {
        when (triggerData.interval) {
            Interval.MINUTE -> {
                slices = LongArray(1)
                sliceMinutes = 1
            }

            Interval.HOUR -> {
                slices = LongArray(60)
                sliceMinutes = 1
            }

            Interval.DAY -> {
                slices = LongArray(48)
                sliceMinutes = 30
            }

            else -> {}
        }
    }

    open fun getTrigger(): DataTrigger {
        return triggerData
    }

    fun setTrigger(trigger: DataTrigger) {
        this.triggerData = trigger
    }

    @Synchronized
    fun updateFrequency(snapshotTimeStamp: Long, nanoTime: Long, collectorContext: CollectorContext) {
        if (triggerData.interval != Interval.NONE) {
            if (lastTriggerNano != Long.MIN_VALUE && nanoTime - lastTriggerNano < inhibitionNanos) {
                // do nothing
            } else if (lastUpdateNano != Long.MIN_VALUE && nanoTime - lastUpdateNano > Interval.MINUTE.getNanos(10)) {
                resetCounter()
            } else {
                slices[currentSlice] += currentEventCount
                currentEventCount = 0

                var recordedCount: Long = 0
                for (slice in slices) {
                    recordedCount += slice
                }

                if (recordedCount >= triggerData.count) {
                    lastTriggerNano = nanoTime
                    resetCounter()
                    executeActions(snapshotTimeStamp, collectorContext)
                } else {
                    if ((++frequencyUpdateCount % sliceMinutes) == 0L) {
                        currentSlice++
                        if (currentSlice >= slices.size) {
                            currentSlice = 0
                        }
                        slices[currentSlice] = 0
                    }
                }
            }
            lastUpdateNano = nanoTime
        }
    }

    protected open fun executeActions(snapshotTimeStamp: Long, collectorContext: CollectorContext) {
        executeActions(collectorContext, triggerData.triggerActions, lastVm, groupVm, triggerData.description)
    }

    private fun resetCounter() {
        slices = LongArray(slices.size)
        currentSlice = 0
        frequencyUpdateCount = 0
    }

    @Synchronized
    open fun addEvents(snapshotTimeStamp: Long, nanoTime: Long, count: Long, vm: VM, collectorContext: CollectorContext) {
        if (lastTriggerNano != Long.MIN_VALUE && nanoTime - lastTriggerNano < inhibitionNanos) {
            // do nothing
        } else {
            currentEventCount += count
            lastVm = vm
            if (triggerData.interval == Interval.NONE && currentEventCount >= triggerData.count) {
                lastTriggerNano = nanoTime
                currentEventCount = 0
                executeActions(snapshotTimeStamp, collectorContext)
            }
        }
    }

    companion object {
        fun executeActions(
            collectorContext: CollectorContext,
            triggerActions: Collection<TriggerAction>,
            lastVm: VM?,
            groupVm: VM?,
            triggerDescription: String,
        ) {
            // every trigger firing leaves exactly one event log entry
            if (triggerActions.none { it is LogAction }) {
                collectorContext.logEvent(groupVm, lastVm, LogCategory.INFO, "Trigger fired: $triggerDescription")
            }
            var commands: MutableList<Command>? = null
            for (action in triggerActions) {
                when {
                    action is LogAction -> collectorContext.logEvent(groupVm, lastVm, action.category, action.text)
                    action is InboxAction -> collectorContext.addInboxItems(
                        groupVm!!.qualifiedIdentifier,
                        getSubject("Trigger", lastVm, groupVm),
                        action.text,
                        null,
                        groupVm
                    )

                    action is EmailAction -> collectorContext.sendMessage(action.email, getSubject(action.category, lastVm, groupVm), action.text)
                    action is WebhookAction -> collectorContext.invokeWebhook(action, getSubject("Triggered by event", lastVm, groupVm))
                    lastVm != null && !lastVm.isGroupNode -> {
                        if (commands == null) {
                            commands = ArrayList()
                        }
                        when (action) {
                            is HeapDumpAction -> commands.add(collectorContext.getHeapDumpCommand(lastVm, null, action.isCreateInboxItem, action.artifactName))
                            is ThreadDumpAction -> commands.add(
                                collectorContext.getThreadDumpCommand(
                                    lastVm,
                                    null,
                                    action.isCreateInboxItem,
                                    action.artifactName
                                )
                            )

                            is RecordJpsAction -> collectorContext.recordJProfilerSnapshot(lastVm, null, action)
                            is RecordJfrAction -> commands.add(collectorContext.getRecordJfrCommand(lastVm, null, action))
                        }
                    }
                }
            }
            if (commands != null) {
                collectorContext.executeLater(lastVm, commands)
            }
        }

        private fun getSubject(prefix: String, lastVm: VM?, groupVm: VM?): String {
            val builder = StringBuilder(prefix)
            if (groupVm != null) {
                builder.append(" on ").append(groupVm.verbose)
                if (lastVm != null) {
                    builder.append(" (last event on ").append(lastVm.verbose).append(")")
                }
            }
            return builder.toString()
        }
    }
}
