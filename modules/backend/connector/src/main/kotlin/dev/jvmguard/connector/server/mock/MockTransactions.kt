package dev.jvmguard.connector.server.mock

import dev.jvmguard.agent.config.transactions.TransactionType
import dev.jvmguard.agent.tree.AbstractTransactionTree.PolicyType
import dev.jvmguard.common.helper.Direction
import dev.jvmguard.common.transactions.DataAvailability
import dev.jvmguard.common.transactions.TransactionCursorImpl
import dev.jvmguard.data.transactions.*
import dev.jvmguard.data.vmdata.VM
import kotlin.math.abs

class MockTransactions(private val serverConnection: MockServerConnectionImpl) {

    private val transactionTreeDatas = HashMap<TransactionTreeInterval, MutableMap<TransactionCursor, TransactionTreeData>>()

    init {
        fillTransactionTreeData()
    }

    private fun findTransactionCursor(
        interval: TransactionTreeInterval,
        time: Long,
        timeRequirement: TimeRequirement = TimeRequirement.INCLUDED
    ): TransactionCursor {
        val cursorToData = transactionTreeDatas[interval]!!
        val floorStart = interval.getFloorStartTime(time)

        if (timeRequirement == TimeRequirement.START_TIME) {
            val available = containsTime(cursorToData.keys, interval, floorStart)
            return MockTransactionCursor(false, available, floorStart, interval)
        }

        if (timeRequirement == TimeRequirement.NEAREST_START_TIME) {
            // The recorded bucket whose start is nearest to the floored start time.
            var nearest: TransactionCursor? = null
            var bestDistance = Long.MAX_VALUE
            for (cursor in cursorToData.keys) {
                val distance = abs(cursor.startTime - floorStart)
                if (distance < bestDistance) {
                    bestDistance = distance
                    nearest = cursor
                }
            }
            return nearest ?: MockTransactionCursor(isLatest = false, dataAvailable = false, startTime = floorStart, interval = interval)
        }

        // INCLUDED: the recorded bucket whose interval contains the time, else an unavailable cursor.
        for (cursor in cursorToData.keys) {
            val start = cursor.startTime
            if (time >= start && time < start + interval.timeExtent) {
                return cursor
            }
        }
        return MockTransactionCursor(false, false, floorStart, interval)
    }

    private fun fillTransactionTreeData() {
        val now = serverConnection.currentTime
        for (interval in TransactionTreeInterval.entries) {
            val cursorToData = LinkedHashMap<TransactionCursor, TransactionTreeData>()
            transactionTreeDatas[interval] = cursorToData

            for (i in 0 until 5) {
                val transactionCursor = MockTransactionCursor(i == 0, true, now - (i + 1) * interval.timeExtent, interval)
                val transactionTreeData = TransactionTreeData(
                    TransactionCursorImpl(),
                    LongArray(0),
                    getMinIntervalPercentage(i),
                    getMaxIntervalPercentage(i),
                    createTransactionTree(i, interval),
                    false,
                    false,
                )
                cursorToData[transactionCursor] = transactionTreeData
            }
        }
    }

    private fun getMinIntervalPercentage(i: Int): Int =
        when (i) {
            0 -> 50
            1 -> 70
            else -> 100
        }

    private fun getMaxIntervalPercentage(i: Int): Int =
        when (i) {
            0 -> 50
            1 -> 90
            else -> 100
        }

    private fun createTransactionTree(i: Int, interval: TransactionTreeInterval): TransactionTree {
        val transactionTree = TransactionTree()
        addChain(transactionTree, i, interval, 1)
        addChain(transactionTree, i, interval, 2)
        addChain(transactionTree, i, interval, 3)
        addChild(
            transactionTree,
            "com.example.acme.billing.invoice.InvoiceReconciliationService.recalculateAllOutstandingInvoicesForCustomerAccountAndNotifyDownstreamSubscribers(java.lang.Long, java.time.LocalDate, boolean)",
            i, interval, 4, PolicyType.NORMAL,
        )
        return transactionTree
    }

    private fun addChain(transactionTree: TransactionTree, i: Int, interval: TransactionTreeInterval, number: Int) {
        val level1 = addChild(transactionTree, "Request $number", i, interval, number, PolicyType.NORMAL)
        val level21 = addChild(level1, "Child 1", i, interval, number, PolicyType.VERY_SLOW)
        addChild(level1, "Child 2", i, interval, number, PolicyType.NORMAL)
        addChild(level21, "3rd level", i, interval, number, PolicyType.NORMAL)
    }

    private fun addChild(
        transactionTree: TransactionTree,
        methodId: String,
        i: Int,
        interval: TransactionTreeInterval,
        number: Int,
        policyType: PolicyType
    ): TransactionTree {
        val child = TransactionTree(transactionTree, methodId, TransactionType.MATCHED, policyType.typeString)
        transactionTree.add(child)
        child.addTimeRecursive(interval.timeExtent * 100 * number / (i + 1))
        child.addCountRecursive(number.toLong())
        return child
    }

    private fun translateTransactionCursor(transactionCursor: TransactionCursor): TransactionCursor =
        findTransactionCursor(transactionCursor.interval, transactionCursor.startTime)

    fun getTransactionTreeCursor(interval: TransactionTreeInterval, time: Long): TransactionCursor =
        findTransactionCursor(interval, time)

    fun getTransactionTreeCursor(interval: TransactionTreeInterval, time: Long, timeRequirement: TimeRequirement): TransactionCursor =
        findTransactionCursor(interval, time, timeRequirement)

    fun getCurrentTransactionTreeCursor(interval: TransactionTreeInterval): TransactionCursor =
        transactionTreeDatas[interval]!!.keys.iterator().next()

    fun changeTransactionCursor(transactionCursor: TransactionCursor, interval: TransactionTreeInterval): TransactionCursor =
        if (transactionCursor.isLatest) {
            getCurrentTransactionTreeCursor(interval)
        } else {
            getTransactionTreeCursor(interval, transactionCursor.startTime)
        }

    fun moveTransactionCursor(transactionCursor: TransactionCursor, direction: Direction): TransactionCursor {
        val newStartTime = transactionCursor.startTime + direction.factor * transactionCursor.interval.timeExtent
        return findTransactionCursor(transactionCursor.interval, newStartTime)
    }

    fun getTransactionTreeData(transactionCursor: TransactionCursor): TransactionTreeData? =
        transactionTreeDatas[transactionCursor.interval]!![translateTransactionCursor(transactionCursor)]

    private class MockTransactionCursor(
        override val isLatest: Boolean,
        private val dataAvailable: Boolean,
        override val startTime: Long,
        override val interval: TransactionTreeInterval,
    ) : TransactionCursor {

        override val availability: DataAvailability
            get() = if (dataAvailable) DataAvailability.TRUE else DataAvailability.FALSE

        override val vm: VM? = null

        override val gap: Long = 0
    }

    companion object {
        private fun containsTime(cursors: Collection<TransactionCursor>, interval: TransactionTreeInterval, time: Long): Boolean {
            for (cursor in cursors) {
                val start = cursor.startTime
                if (time >= start && time < start + interval.timeExtent) {
                    return true
                }
            }
            return false
        }
    }
}
