package com.jvmguard.common.export

import com.jvmguard.data.transactions.TransactionTree

class TransactionTreeExport(dataType: DataType, private val transactionTree: TransactionTree) :
    AbstractTreeExport<TransactionTreeExport>(dataType.toString()) {

    override fun doExport() {
        exportTransactionTree(transactionTree)
    }

    enum class DataType(private val verbose: String) {
        CALL_TREE("callTree"),
        HOT_SPOTS("hotSpots"),
        OVERDUE("overdue");

        override fun toString(): String = verbose
    }
}
