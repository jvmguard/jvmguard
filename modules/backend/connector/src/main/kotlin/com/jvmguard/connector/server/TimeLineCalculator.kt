package com.jvmguard.connector.server

import com.jvmguard.data.transactions.*
import com.jvmguard.connector.api.ServerConnection
import kotlin.math.max

abstract class TimeLineCalculator<T>(protected val selectedItem: T) {

    abstract fun calculateValue(serverConnection: ServerConnection, cursor: TransactionCursor, valueType: TransactionTreeValueType): Long?

    protected fun getCallTreeTimeLineValue(count: Long, time: Long, valueType: TransactionTreeValueType): Long =
        when (valueType) {
            TransactionTreeValueType.COUNT -> count
            TransactionTreeValueType.TOTAL, TransactionTreeValueType.AVERAGE -> time
        }

    open class TransactionInfoTimeLineCalculator(selectedItem: TransactionInfo) :
        TimeLineCalculator<TransactionInfo>(selectedItem) {

        override fun calculateValue(serverConnection: ServerConnection, cursor: TransactionCursor, valueType: TransactionTreeValueType): Long? {
            val transactionTreeData = getTransactionTreeData(serverConnection, cursor)
            if (transactionTreeData == null || transactionTreeData.maxIntervalPercentage == 0) {
                return null
            }
            val transactionTree = transactionTreeData.transactionTree
            val cumulatedValue = getTransactionTreeTimeLineValue(transactionTree, valueType)
            val cumulatedCount = getTransactionTreeTimeLineValue(transactionTree, TransactionTreeValueType.COUNT)
            return if (valueType.isAverage) cumulatedValue / max(1, cumulatedCount) else cumulatedValue
        }

        protected open fun getTransactionTreeData(serverConnection: ServerConnection, cursor: TransactionCursor): TransactionTreeData? =
            serverConnection.getCallTree(cursor, true)

        private fun getTransactionTreeTimeLineValue(transactionTree: TransactionTree?, valueType: TransactionTreeValueType): Long {
            if (transactionTree != null) {
                for (child in transactionTree.children()) {
                    if (selectedItem == getComparisonObject(child)) {
                        return getCallTreeTimeLineValue(child.count, child.time, valueType)
                    }
                }
            }
            return 0
        }

        protected open fun getComparisonObject(child: TransactionTree): TransactionInfo = child.getTransactionInfo()
    }

    class SplitTimeLineCalculator(selectedItem: TransactionTreeIdentifier) :
        TransactionInfoTimeLineCalculator(selectedItem) {

        override fun getTransactionTreeData(serverConnection: ServerConnection, cursor: TransactionCursor): TransactionTreeData =
            serverConnection.getCallTree(cursor, false)

        override fun getComparisonObject(child: TransactionTree): TransactionInfo = child.getIdentifier()
    }

    class HotspotsTimeLineCalculator(selectedItem: TransactionInfo) :
        TransactionInfoTimeLineCalculator(selectedItem) {

        override fun getTransactionTreeData(serverConnection: ServerConnection, cursor: TransactionCursor): TransactionTreeData =
            serverConnection.getHotspots(cursor, true)
    }
}
