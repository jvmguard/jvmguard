package com.jvmguard.data.transactions

import com.jvmguard.common.transactions.TransactionCursorImpl

class TransactionTreeData(
    transactionCursor: TransactionCursorImpl,
    containedVmIds: LongArray,
    minIntervalPercentage: Int,
    maxIntervalPercentage: Int,
    val transactionTree: TransactionTree,
    val hotspots: Boolean,
    mergePolicies: Boolean,
) : AbstractTransactionData(transactionCursor, containedVmIds, minIntervalPercentage, maxIntervalPercentage, mergePolicies)
