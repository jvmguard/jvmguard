package dev.jvmguard.collector.transactions.provider

import dev.jvmguard.agent.config.VmType
import dev.jvmguard.agent.config.transactions.TransactionType
import dev.jvmguard.agent.tree.AbstractTransactionTree.PolicyType
import dev.jvmguard.collector.api.TransactionProvider
import dev.jvmguard.collector.main.VmRegistry
import dev.jvmguard.collector.main.VmStorage
import dev.jvmguard.collector.transactions.TransactionManager
import dev.jvmguard.collector.transactions.TransactionManager.TreeVisitor
import dev.jvmguard.common.helper.Direction
import dev.jvmguard.common.transactions.DataAvailability
import dev.jvmguard.common.transactions.TransactionCursorImpl
import dev.jvmguard.common.transactions.TransactionCursorImpl.VmNodes
import dev.jvmguard.data.transactions.*
import dev.jvmguard.data.vmdata.VM
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.longs.LongSet
import org.springframework.stereotype.Component
import java.util.*

@Component
class TransactionProviderImpl(
    private val vmRegistry: VmRegistry,
    private val vmStorage: VmStorage,
    private val transactionManager: TransactionManager,
) : TransactionProvider {

    override fun getTransactionTreeCursor(
        vm: VM?,
        interval: TransactionTreeInterval,
        transactionDataType: TransactionDataType,
        time: Long,
        timeRequirement: TimeRequirement
    ): TransactionCursor {
        val usedVm = vm ?: vmRegistry.getRootGroupVM()
        val currentTransactionTreeCursor = getCurrentTransactionTreeCursor(usedVm, interval, transactionDataType) as TransactionCursorImpl

        val dataInterval = TransactionDataInterval.getUsedIntervals(interval, usedVm.isGroupNode, false).largeInterval

        val searchedStartTime = dataInterval.getFloorStartTime(time)
        return when (timeRequirement) {
            TimeRequirement.START_TIME -> {
                val transactionCursor = createCursor(usedVm, interval, transactionDataType, searchedStartTime)
                updateAvailability(transactionCursor, dataInterval)
                transactionCursor
            }

            TimeRequirement.INCLUDED -> {
                val lowerTime = transactionManager.getTimeExtremum("MAX", 0, time, dataInterval, transactionDataType, usedVm)
                if (lowerTime != null && lowerTime + interval.timeExtent >= time) {
                    createCursor(usedVm, interval, transactionDataType, lowerTime)
                } else {
                    val transactionCursor = createCursor(usedVm, interval, transactionDataType, 0)
                    transactionCursor.availability = DataAvailability.FALSE
                    transactionCursor
                }
            }

            else -> {
                val nearestTime = getNearestTime(usedVm, dataInterval, transactionDataType, searchedStartTime)
                if (nearestTime == null) {
                    currentTransactionTreeCursor
                } else if (getDistance(currentTransactionTreeCursor.startTime, searchedStartTime) < getDistance(nearestTime, searchedStartTime)) {
                    currentTransactionTreeCursor
                } else {
                    createCursor(usedVm, interval, transactionDataType, nearestTime)
                }
            }
        }
    }

    private fun updateAvailability(transactionCursor: TransactionCursorImpl, dataInterval: TransactionDataInterval) {
        val vm = transactionCursor.vm ?: return
        val endTime = transactionCursor.startTime + transactionCursor.interval.timeExtent - 1 // exclusive
        val minTime = transactionManager.getTimeExtremum("MIN", transactionCursor.startTime, endTime, dataInterval, transactionCursor.transactionDataType, vm)
        if (minTime == null) {
            transactionCursor.availability = DataAvailability.FALSE
        }
    }

    private fun getNearestTime(vm: VM, dataInterval: TransactionDataInterval, transactionDataType: TransactionDataType, startTime: Long): Long? {
        val lowerTime = transactionManager.getTimeExtremum("MAX", 0, startTime, dataInterval, transactionDataType, vm)
        val upperTime = transactionManager.getTimeExtremum("MIN", startTime, System.currentTimeMillis(), dataInterval, transactionDataType, vm)
        return when {
            lowerTime == null && upperTime == null -> null
            lowerTime == null -> upperTime
            upperTime == null -> lowerTime
            startTime - lowerTime < upperTime - startTime -> lowerTime
            else -> upperTime
        }
    }

    override fun getCurrentTransactionTreeCursor(vm: VM?, interval: TransactionTreeInterval, transactionDataType: TransactionDataType): TransactionCursor {
        val usedVm = vm ?: vmRegistry.getRootGroupVM()
        val cursor = TransactionCursorImpl()
        cursor.vm = usedVm
        cursor.isGroupData = usedVm.isGroupNode
        cursor.interval = interval
        cursor.transactionDataType = transactionDataType
        updateCurrentTransactionTreeCursor(cursor)
        return cursor
    }

    private fun updateCurrentTransactionTreeCursor(cursor: TransactionCursorImpl) {
        cursor.isLatest = true
        val currentTime = System.currentTimeMillis()
        val vm = cursor.vm ?: return

        val transactionTreeInterval = cursor.interval
        val dataInterval = TransactionDataInterval.getUsedIntervals(transactionTreeInterval, vm.isGroupNode, true).smallestInterval

        val newestDbTime =
            transactionManager.getTimeExtremum("MAX", 0, System.currentTimeMillis(), dataInterval, cursor.transactionDataType.currentCheckType, vm)
        if (newestDbTime == null) {
            cursor.availability = DataAvailability.FALSE
        } else {
            cursor.startTime = newestDbTime - transactionTreeInterval.timeExtent + dataInterval.millis
        }
        val timeExtent = transactionTreeInterval.timeExtent
        if (cursor.startTime + (if (transactionTreeInterval == TransactionTreeInterval.MINUTE) 3 else 2) * timeExtent < currentTime) {
            cursor.startTime = currentTime - timeExtent
            cursor.availability = DataAvailability.FALSE
        }
    }

    override fun changeTransactionCursor(transactionCursor: TransactionCursor, vm: VM?, interval: TransactionTreeInterval): TransactionCursor {
        val transactionCursorImpl = transactionCursor as TransactionCursorImpl
        val usedVm = vm ?: vmRegistry.getRootGroupVM()

        val latest = getCurrentTransactionTreeCursor(usedVm, interval, transactionCursorImpl.transactionDataType) as TransactionCursorImpl
        if (transactionCursorImpl.isLatest) {
            return latest
        }
        val newCursor = transactionCursorImpl.clone()

        var startTime = transactionCursorImpl.startTime

        if (interval != transactionCursorImpl.interval) {
            if (interval.timeExtent < transactionCursorImpl.interval.timeExtent) {
                startTime += transactionCursorImpl.interval.timeExtent - interval.timeExtent
            } else {
                startTime = interval.getFloorStartTime(startTime)
            }

            val dataInterval = TransactionDataInterval.getUsedIntervals(interval, usedVm.isGroupNode, false).largeInterval
            if (dataInterval.isSameLength(interval)) {
                newCursor.availability = DataAvailability.FALSE
                val nearestTime = getNearestTime(usedVm, dataInterval, newCursor.transactionDataType, startTime)

                if (nearestTime != null) {
                    val distance = if (nearestTime > startTime) nearestTime - startTime else startTime - nearestTime
                    if (distance < interval.timeExtent / 2) {
                        startTime = nearestTime
                        newCursor.availability = DataAvailability.TRUE
                    }
                }
            } else {
                newCursor.availability = if (transactionManager.getTimeExtremum(
                        "MIN",
                        startTime,
                        startTime + interval.timeExtent + 1,
                        dataInterval,
                        newCursor.transactionDataType,
                        usedVm
                    ) != null
                ) DataAvailability.TRUE else DataAvailability.FALSE
            }

            if (startTime >= latest.startTime || (!newCursor.availability.isAvailable && latest.startTime <= transactionCursorImpl.startTime)) {
                return latest
            }
        }
        newCursor.startTime = startTime
        newCursor.vm = usedVm
        newCursor.isGroupData = usedVm.isGroupNode
        newCursor.interval = interval
        return newCursor
    }

    private fun getUpperDataTime(startTime: Long, vm: VM, dataInterval: TransactionDataInterval, transactionDataType: TransactionDataType): Long {
        val time = transactionManager.getTimeExtremum("MIN", startTime, System.currentTimeMillis(), dataInterval, transactionDataType, vm)
        return time ?: Long.MAX_VALUE
    }

    override fun getNextTransactionCursor(cursor: TransactionCursor): TransactionCursor {
        val transactionCursor = cursor as TransactionCursorImpl
        val vm = transactionCursor.vm ?: throw IllegalArgumentException()

        val latestTransactionTreeCursor =
            getCurrentTransactionTreeCursor(vm, transactionCursor.interval, transactionCursor.transactionDataType) as TransactionCursorImpl
        if (transactionCursor.isLatest) {
            return latestTransactionTreeCursor
        }
        val dataInterval = TransactionDataInterval.getUsedIntervals(cursor.interval, vm.isGroupNode, false).largeInterval

        var nextStartTime: Long
        if (dataInterval.isSameLength(transactionCursor.interval)) {
            nextStartTime = getUpperDataTime(transactionCursor.startTime + 1, vm, dataInterval, transactionCursor.transactionDataType)
        } else {
            nextStartTime = transactionCursor.interval.getNextStartTime(transactionCursor.startTime)
            val nextDataTime = getUpperDataTime(nextStartTime, vm, dataInterval, transactionCursor.transactionDataType)
            nextStartTime = transactionCursor.interval.getFloorStartTime(nextDataTime)
        }

        return if (nextStartTime == 0L || nextStartTime == Long.MAX_VALUE || nextStartTime >= latestTransactionTreeCursor.startTime) {
            latestTransactionTreeCursor.updateGap(Direction.NEXT, transactionCursor)
        } else {
            val newCursor = transactionCursor.clone()
            newCursor.isLatest = false
            newCursor.availability = DataAvailability.TRUE
            newCursor.startTime = nextStartTime
            newCursor.updateGap(Direction.NEXT, transactionCursor)
        }
    }

    private fun getLowerDataTime(endTime: Long, vm: VM, dataInterval: TransactionDataInterval, transactionDataType: TransactionDataType): Long {
        val time = transactionManager.getTimeExtremum("MAX", 0, endTime, dataInterval, transactionDataType, vm)
        return time ?: 0
    }

    override fun getPreviousTransactionCursor(cursor: TransactionCursor): TransactionCursor {
        val transactionCursor = cursor as TransactionCursorImpl
        val vm = transactionCursor.vm ?: throw IllegalArgumentException()

        val dataInterval = TransactionDataInterval.getUsedIntervals(cursor.interval, vm.isGroupNode, false).largeInterval
        var previousStartTime: Long
        if (dataInterval.isSameLength(transactionCursor.interval)) {
            previousStartTime = getLowerDataTime(transactionCursor.startTime - 1, vm, dataInterval, transactionCursor.transactionDataType)
        } else {
            previousStartTime = if (transactionCursor.isLatest) {
                val floored = transactionCursor.interval.getFloorStartTime(transactionCursor.startTime)
                if (floored == transactionCursor.startTime) {
                    transactionCursor.interval.getPreviousStartTime(transactionCursor.startTime)
                } else {
                    floored
                }
            } else {
                transactionCursor.interval.getPreviousStartTime(transactionCursor.startTime)
            }
            val previousDataTime =
                getLowerDataTime(previousStartTime + transactionCursor.interval.timeExtent - 1, vm, dataInterval, transactionCursor.transactionDataType)
            previousStartTime = transactionCursor.interval.getFloorStartTime(previousDataTime)
        }

        val newCursor = transactionCursor.clone()
        newCursor.startTime = previousStartTime
        newCursor.availability = DataAvailability.FALSE
        if (previousStartTime != 0L) {
            newCursor.availability = DataAvailability.TRUE
        }
        newCursor.isLatest = false
        return newCursor.updateGap(Direction.PREVIOUS, transactionCursor)
    }

    override fun getCallTree(transactionCursor: TransactionCursor, mergePolicies: Boolean): TransactionTreeData =
        getRawData(transactionCursor).calculateTransactionTree(mergePolicies)

    override fun getHotspots(transactionCursor: TransactionCursor, mergePolicies: Boolean): TransactionTreeData =
        getRawData(transactionCursor).calculateHotspotTree(mergePolicies)

    override fun getTransactionInfo(transactionCursor: TransactionCursor): Set<TransactionInfo> =
        getRawData(transactionCursor).getTransactionInfo()

    override fun resetCapCount(ifCappedOnly: Boolean) {
        transactionManager.resetCapCount(ifCappedOnly)
    }

    override val caps: EnumSet<CapType>
        get() = transactionManager.caps

    private fun getRawData(transactionCursorIf: TransactionCursor?): RawTransactionData {
        val transactionCursor = transactionCursorIf as TransactionCursorImpl?
        val destinationTree = TransactionTree()

        if (transactionCursor == null || !transactionCursor.availability.isAvailable) {
            return RawTransactionData(TransactionCursorImpl(), destinationTree, LongArray(0), null)
        }

        val containedVms = LongOpenHashSet()
        val treeVisitor = object : TreeVisitor() {
            override fun preVisit(vmId: Long, groupNode: Byte): TransactionTree {
                if (transactionCursor.vmNodes.isComplete) {
                    val vm = vmStorage.getVmById(vmId)
                    var name = "<unknown>"
                    if (vm != null) {
                        name =
                            if (transactionCursor.vmNodes == VmNodes.GROUP || vm.type == VmType.POOLED) TransactionManager.getVerboseGroup(vm) else vm.displayHierarchyPath
                    }
                    return destinationTree.getOrCreateChild(TransactionTree(name, TransactionType.VM, PolicyType.NORMAL.typeString))
                } else {
                    return destinationTree
                }
            }

            override fun preVisitContained(vmId: Long, groupNode: Byte): LongSet = containedVms
        }

        if (transactionCursor.vm != null) {
            transactionManager.visitTransactionTreeData(Collections.singleton(transactionCursor.vm), transactionCursor, false, treeVisitor)
            if (transactionCursor.transactionDataType.isRemoveEmpty) {
                destinationTree.removeEmpty(true)
            }
        }

        return RawTransactionData(transactionCursor, destinationTree, containedVms.toLongArray(), treeVisitor.percentage)
    }

    companion object {
        private fun createCursor(vm: VM, interval: TransactionTreeInterval, transactionDataType: TransactionDataType, startTime: Long): TransactionCursorImpl {
            val cursor = TransactionCursorImpl()
            cursor.transactionDataType = transactionDataType
            cursor.vm = vm
            cursor.isGroupData = vm.isGroupNode
            cursor.interval = interval
            cursor.startTime = startTime
            return cursor
        }

        private fun getDistance(v1: Long, v2: Long): Long = if (v1 > v2) v1 - v2 else v2 - v1
    }
}
