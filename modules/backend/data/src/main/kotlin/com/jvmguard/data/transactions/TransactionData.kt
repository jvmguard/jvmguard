package com.jvmguard.data.transactions

import com.jvmguard.common.transactions.TransactionCursorImpl

interface TransactionPercentage {
    val minIntervalPercentage: Int
    val maxIntervalPercentage: Int
}

interface TransactionData : TransactionPercentage {
    val containedVmIds: LongArray
}

abstract class AbstractTransactionData protected constructor(
    protected var transactionCursor: TransactionCursorImpl,
    override val containedVmIds: LongArray,
    override val minIntervalPercentage: Int,
    override val maxIntervalPercentage: Int,
    protected val mergePolicies: Boolean,
) : TransactionData
