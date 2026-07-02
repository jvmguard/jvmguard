package com.jvmguard.collector.vmdata.components

import com.jvmguard.agent.config.transactions.PolicyEventType
import com.jvmguard.agent.tree.AbstractTransactionTree
import com.jvmguard.agent.tree.Tree.Visitor
import com.jvmguard.collector.main.CollectorContext
import com.jvmguard.collector.threshold.ViolationEvent
import com.jvmguard.collector.trigger.ConnectionTriggerHandler
import com.jvmguard.collector.trigger.PolicyTriggerHandler
import com.jvmguard.collector.trigger.ThresholdTriggerHandler
import com.jvmguard.collector.vmdata.VmGroupData
import com.jvmguard.data.config.triggers.ConnectionTrigger
import com.jvmguard.data.config.triggers.PolicyTrigger
import com.jvmguard.data.config.triggers.ThresholdTrigger
import com.jvmguard.data.config.triggers.Trigger
import com.jvmguard.data.vmdata.ThresholdIdentifier
import com.jvmguard.data.vmdata.VM

class GroupTriggerHandler(private val vmGroupData: VmGroupData) {

    private val transactionEntries = HashMap<String, TransactionTriggerEntry>()
    private val policyTriggers = HashMap<Long, PolicyTriggerHandler>()
    private val connectionTriggers = ArrayList<ConnectionTriggerHandler>()
    private val thresholdTriggers = HashMap<ThresholdIdentifier, MutableList<ThresholdTriggerHandler>>()

    fun <T : AbstractTransactionTree<*, T>> addTransactions(
        transactionTree: T,
        overdueTree: T?,
        collectorContext: CollectorContext,
        snapshotTimeStamp: Long,
        nanoTime: Long,
        vm: VM
    ) {
        synchronized(this) {
            if (policyTriggers.isNotEmpty()) {
                val countVisitor = CountVisitor()
                @Suppress("UNCHECKED_CAST")
                transactionTree.visit(countVisitor as Visitor<T>)
                if (overdueTree != null) {
                    @Suppress("UNCHECKED_CAST")
                    overdueTree.visit(countVisitor.overdue(true) as Visitor<T>)
                }
                for ((name, count) in countVisitor.counts) {
                    getTransactionEntry(name).triggerCount(snapshotTimeStamp, nanoTime, count, vm, collectorContext)
                }
            }
        }
        vmGroupData.parent?.triggerHandler?.addTransactions(transactionTree, overdueTree, collectorContext, snapshotTimeStamp, nanoTime, vm)
    }

    private fun getTransactionEntry(name: String): TransactionTriggerEntry {
        return transactionEntries.computeIfAbsent(name) { n -> TransactionTriggerEntry(policyTriggers.values, n) }
    }

    fun addViolation(snapshotTimeStamp: Long, nanoTime: Long, violationEvent: ViolationEvent, vm: VM, collectorContext: CollectorContext) {
        var usedHandlers: List<ThresholdTriggerHandler>? = null
        synchronized(this) {
            if (thresholdTriggers.containsKey(violationEvent.thresholdIdentifier)) {
                usedHandlers = thresholdTriggers[violationEvent.thresholdIdentifier]
            }
        }
        usedHandlers?.let { handlers ->
            for (usedHandler in handlers) {
                usedHandler.addEvents(snapshotTimeStamp, nanoTime, violationEvent, vm, collectorContext)
            }
        }
        vmGroupData.parent?.triggerHandler?.addViolation(snapshotTimeStamp, nanoTime, violationEvent, vm, collectorContext)
    }

    @Synchronized
    fun checkConnectionTrigger(nanoTime: Long, count: Int, collectorContext: CollectorContext) {
        for (triggerHandler in connectionTriggers) {
            triggerHandler.check(nanoTime, count, collectorContext)
        }
    }

    @Synchronized
    fun setTriggers(triggerList: List<Trigger>) {
        val newThresholdTriggers = HashMap<ThresholdIdentifier, MutableList<ThresholdTriggerHandler>>()
        val newTriggerHandler = HashMap<Long, PolicyTriggerHandler>()
        val newConnectionTriggers = ArrayList<ConnectionTriggerHandler>()

        for (originalTrigger in triggerList) {
            if (originalTrigger.isEnabled) {
                when (val trigger = originalTrigger.clone()) {
                    is ThresholdTrigger -> {
                        val thresholdIdentifier = trigger.thresholdIdentifier ?: continue
                        var usedHandler: ThresholdTriggerHandler? = null
                        for (triggerHandler in thresholdTriggers[thresholdIdentifier].orEmpty()) {
                            val otherTrigger = triggerHandler.getTrigger()
                            if (otherTrigger.id != null && otherTrigger.id == trigger.id && otherTrigger.isIdenticalCounterType(trigger)) {
                                usedHandler = triggerHandler
                                usedHandler.setTrigger(trigger)
                                break
                            }
                        }
                        if (usedHandler == null) {
                            usedHandler = ThresholdTriggerHandler(trigger, vmGroupData)
                        }
                        newThresholdTriggers.computeIfAbsent(thresholdIdentifier) { _ -> ArrayList() }.add(usedHandler)
                    }

                    is PolicyTrigger -> {
                        val id = trigger.id!!
                        var usedHandler = policyTriggers[id]
                        if (usedHandler != null && usedHandler.getTrigger().isReplaceable(trigger)) {
                            usedHandler.setTrigger(trigger)
                        } else {
                            usedHandler = PolicyTriggerHandler(trigger, vmGroupData.vm)
                        }
                        newTriggerHandler[id] = usedHandler
                    }

                    is ConnectionTrigger -> {
                        var handler: ConnectionTriggerHandler? = null
                        for (previousHandler in connectionTriggers) {
                            val otherTrigger = previousHandler.trigger
                            if (otherTrigger.id != null && otherTrigger.id == trigger.id && otherTrigger.isIdenticalCounterType(trigger)) {
                                handler = previousHandler
                                handler.trigger = trigger
                                break
                            }
                        }
                        if (handler == null) {
                            handler = ConnectionTriggerHandler(trigger, vmGroupData.vm)
                        }
                        newConnectionTriggers.add(handler)
                    }
                }
            }
        }

        thresholdTriggers.clear()
        thresholdTriggers.putAll(newThresholdTriggers)
        policyTriggers.clear()
        policyTriggers.putAll(newTriggerHandler)
        for ((name, entry) in transactionEntries) {
            entry.updateTrigger(policyTriggers.values, name)
        }
        connectionTriggers.clear()
        connectionTriggers.addAll(newConnectionTriggers)
    }

    @Synchronized
    fun updateTriggerFrequency(snapshotTimeStamp: Long, nanoTime: Long, collectorContext: CollectorContext) {
        for (handlers in thresholdTriggers.values) {
            for (triggerHandler in handlers) {
                triggerHandler.updateFrequency(snapshotTimeStamp, nanoTime, collectorContext)
            }
        }
        for (triggerHandler in policyTriggers.values) {
            triggerHandler.updateFrequency(snapshotTimeStamp, nanoTime, collectorContext)
        }
    }

    private class CountVisitor : Visitor<AbstractTransactionTree<*, *>> {
        private var overdue = false
        val counts = HashMap<String, Count>()

        override fun preVisit(tree: AbstractTransactionTree<*, *>): Boolean {
            if (tree.name != null) {
                val count = getCount(tree.name)
                count.count(if (overdue) PolicyEventType.OVERDUE else PolicyEventType.getByTransactionTreeType(tree.policyType), tree.count)
            }
            return true
        }

        override fun postVisit(tree: AbstractTransactionTree<*, *>) {
        }

        private fun getCount(name: String): Count {
            return counts.computeIfAbsent(name) { _ -> Count() }
        }

        fun overdue(overdue: Boolean): CountVisitor {
            this.overdue = overdue
            return this
        }
    }
}
