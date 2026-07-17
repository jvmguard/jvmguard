package dev.jvmguard.ui.views.data.transactions

import dev.jvmguard.data.transactions.TransactionCursor
import dev.jvmguard.data.transactions.TransactionDataType
import dev.jvmguard.data.transactions.TransactionTreeData
import dev.jvmguard.connector.api.ServerConnection

enum class TransactionMode(
    val label: String,
    val dataType: TransactionDataType,
    val nameColumnHeader: String,
    val hasTimeLines: Boolean,
    val cumulateBacktraces: Boolean,
) {
    CALL_TREE("Call tree", TransactionDataType.TRANSACTION, "Transaction", hasTimeLines = true, cumulateBacktraces = false) {
        override fun fetch(connection: ServerConnection, cursor: TransactionCursor): TransactionTreeData =
            connection.getCallTree(cursor, false)
    },
    HOT_SPOTS("Hot spots", TransactionDataType.TRANSACTION, "Hot spot", hasTimeLines = true, cumulateBacktraces = true) {
        override fun fetch(connection: ServerConnection, cursor: TransactionCursor): TransactionTreeData =
            connection.getHotspots(cursor, false)
    },
    OVERDUE("Overdue", TransactionDataType.OVERDUE, "Hot spot", hasTimeLines = false, cumulateBacktraces = true) {
        override fun fetch(connection: ServerConnection, cursor: TransactionCursor): TransactionTreeData =
            connection.getHotspots(cursor, false)
    };

    abstract fun fetch(connection: ServerConnection, cursor: TransactionCursor): TransactionTreeData
}
