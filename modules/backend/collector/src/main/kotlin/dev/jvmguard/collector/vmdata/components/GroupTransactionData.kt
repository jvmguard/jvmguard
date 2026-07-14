package dev.jvmguard.collector.vmdata.components

import dev.jvmguard.agent.config.VmType
import dev.jvmguard.annotation.MethodTransaction
import dev.jvmguard.annotation.Part
import dev.jvmguard.collector.telemetry.TelemetryManager
import dev.jvmguard.collector.transactions.TransactionManager
import dev.jvmguard.collector.transactions.util.ProcessedAgentTree
import dev.jvmguard.collector.vmdata.VmGroupData
import dev.jvmguard.collector.vmdata.structures.GroupMergeVisitor
import dev.jvmguard.collector.vmdata.structures.VmMergeVisitor
import dev.jvmguard.data.transactions.TransactionDataInterval
import dev.jvmguard.data.transactions.TransactionTree
import dev.jvmguard.data.vmdata.VM
import dev.jvmguard.data.vmdata.VmIdentifier
import it.unimi.dsi.fastutil.longs.LongOpenHashSet

// will be locked nested started from the vm to the root group and use structureLock via isComplete. ATTENTION: do not access otherwise
class GroupTransactionData(private val vmGroupData: VmGroupData) {

    private var transactionTree = TransactionTree()
    private var overdueTree = TransactionTree()
    private var currentSnapshotTimeStamp: Long = 0
    private var lastTelemetryIteration: Long = 0
    private var totalRealInterval: Long = 0
    private val arrivedVms = HashSet<VM>()
    private val arrivedGroups = HashSet<VmIdentifier>()
    private var totalArrivedVmCount = 0

    private val containedOverdueIds = LongOpenHashSet()
    private val containedTransactionIds = LongOpenHashSet()

    private val vmMergeVisitor = VmMergeVisitor()
    private val groupMergeVisitor = GroupMergeVisitor()

    private val pool = vmGroupData.vm.type == VmType.POOL

    @Volatile
    private var completionListener: CompletionListener? = null

    @Synchronized
    fun addTransactionData(
        iteration: Long,
        arrivedVm: VM,
        singleTree: ProcessedAgentTree,
        singleOverdueTree: ProcessedAgentTree,
        snapshotTimeStamp: Long,
        nanoTime: Long,
        realInterval: Int,
        transactionManager: TransactionManager
    ) {
        checkIncomplete(iteration, snapshotTimeStamp, nanoTime, transactionManager)
        singleTree.visit(vmMergeVisitor.init(singleTree, transactionTree))
        singleOverdueTree.visit(vmMergeVisitor.init(singleOverdueTree, overdueTree))

        if (pool) {
            containedTransactionIds.add(arrivedVm.id)
            if (singleOverdueTree.childCount > 0) {
                containedOverdueIds.add(arrivedVm.id)
            }
        }

        arrivedVms.add(arrivedVm)
        totalRealInterval += realInterval
        totalArrivedVmCount++
        currentSnapshotTimeStamp = snapshotTimeStamp

        if (vmGroupData.isComplete(arrivedVms, arrivedGroups)) {
            storeTransactions(iteration, transactionManager, snapshotTimeStamp, nanoTime)
        }
    }

    @Synchronized
    fun checkIncomplete(iteration: Long, snapshotTimeStamp: Long, nanoTime: Long, transactionManager: TransactionManager) {
        val previousIteration = iteration - 1
        val previousSnapshotTime = snapshotTimeStamp - TransactionDataInterval.getRecordingInterval().millis
        val previousNanoTime = nanoTime - TransactionDataInterval.getRecordingInterval().getNanos()

        if (currentSnapshotTimeStamp != 0L && currentSnapshotTimeStamp != snapshotTimeStamp) {
            storeTransactions(previousIteration, transactionManager, previousSnapshotTime, previousNanoTime)
        }
        if (lastTelemetryIteration < previousIteration) {
            commitTransactionTelemetryAndTrigger(previousIteration, previousSnapshotTime, previousNanoTime)
        }
    }

    private fun completed(iteration: Long) {
        val completionListener = this.completionListener
        if (completionListener != null && completionListener.completed(iteration)) {
            this.completionListener = null
        }
    }

    @Synchronized
    fun addGroupTransactionData(
        iteration: Long,
        arrivedGroupName: VmIdentifier,
        singleTree: TransactionTree,
        singleOverdueTree: TransactionTree,
        snapshotTimeStamp: Long,
        nanoTime: Long,
        realInterval: Long,
        vmCount: Int,
        transactionManager: TransactionManager
    ) {
        checkIncomplete(iteration, snapshotTimeStamp, nanoTime, transactionManager)

        singleTree.visit(groupMergeVisitor.init(singleTree, transactionTree))
        singleOverdueTree.visit(groupMergeVisitor.init(singleOverdueTree, overdueTree))
        arrivedGroups.add(arrivedGroupName)
        totalRealInterval += realInterval
        totalArrivedVmCount += vmCount
        currentSnapshotTimeStamp = snapshotTimeStamp

        if (vmGroupData.isComplete(arrivedVms, arrivedGroups)) {
            storeTransactions(iteration, transactionManager, snapshotTimeStamp, nanoTime)
        }
    }

    @MethodTransaction(naming = [Part(text = "store group data")])
    private fun storeTransactions(iteration: Long, transactionManager: TransactionManager, snapshotTimeStamp: Long, nanoTime: Long) {
        val realInterval = if (totalArrivedVmCount > 0) (totalRealInterval / totalArrivedVmCount).toInt() else 0
        transactionManager.saveGroupTree(
            transactionTree,
            overdueTree,
            vmGroupData.vm,
            currentSnapshotTimeStamp,
            realInterval,
            containedTransactionIds,
            containedOverdueIds
        )
        transactionManager.addTelemetryData(snapshotTimeStamp, nanoTime, vmGroupData, transactionTree, realInterval)

        vmGroupData.parent?.transactionData?.addGroupTransactionData(
            iteration,
            vmGroupData.vm.unqualifiedIdentifier,
            transactionTree,
            overdueTree,
            currentSnapshotTimeStamp,
            nanoTime,
            totalRealInterval,
            totalArrivedVmCount,
            transactionManager
        )
        commitTransactionTelemetryAndTrigger(iteration, snapshotTimeStamp, nanoTime)

        totalRealInterval = 0
        totalArrivedVmCount = 0
        currentSnapshotTimeStamp = 0
        containedOverdueIds.clear()
        containedTransactionIds.clear()
        arrivedVms.clear()
        arrivedGroups.clear()
        transactionTree = TransactionTree()
        overdueTree = TransactionTree()
    }

    private fun commitTransactionTelemetryAndTrigger(iteration: Long, snapshotTimeStamp: Long, nanoTime: Long) {
        lastTelemetryIteration = iteration
        if (vmGroupData.parent == null) {
            vmGroupData.commitTelemetryData(nanoTime, snapshotTimeStamp, TelemetryManager.TRANSACTION_RECORDING_INTERVAL)
            vmGroupData.updateTriggerFrequency(snapshotTimeStamp, nanoTime)
        }
        completed(iteration)
    }

    fun setCompletionListener(completionListener: CompletionListener) {
        this.completionListener = completionListener
    }

    fun interface CompletionListener {
        fun completed(iteration: Long): Boolean
    }
}
