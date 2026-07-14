package dev.jvmguard.collector.api

import dev.jvmguard.data.transactions.*
import dev.jvmguard.data.vmdata.VM
import java.util.*

interface TransactionProvider {
    fun getTransactionTreeCursor(
        vm: VM?,
        interval: TransactionTreeInterval,
        transactionDataType: TransactionDataType,
        time: Long,
        timeRequirement: TimeRequirement
    ): TransactionCursor

    fun getCurrentTransactionTreeCursor(vm: VM?, interval: TransactionTreeInterval, transactionDataType: TransactionDataType): TransactionCursor
    fun changeTransactionCursor(transactionCursor: TransactionCursor, vm: VM?, interval: TransactionTreeInterval): TransactionCursor

    fun getNextTransactionCursor(cursor: TransactionCursor): TransactionCursor
    fun getPreviousTransactionCursor(cursor: TransactionCursor): TransactionCursor

    fun getCallTree(transactionCursor: TransactionCursor, mergePolicies: Boolean): TransactionTreeData
    fun getHotspots(transactionCursor: TransactionCursor, mergePolicies: Boolean): TransactionTreeData

    fun getTransactionInfo(transactionCursor: TransactionCursor): Set<TransactionInfo>

    fun resetCapCount(ifCappedOnly: Boolean)

    val caps: EnumSet<CapType>
}
