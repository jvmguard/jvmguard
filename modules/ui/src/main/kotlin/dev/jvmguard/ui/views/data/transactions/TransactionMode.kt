package dev.jvmguard.ui.views.data.transactions

import dev.jvmguard.data.transactions.TransactionCursor
import dev.jvmguard.data.transactions.TransactionDataType
import dev.jvmguard.data.transactions.TransactionTreeData
import dev.jvmguard.connector.api.ServerConnection
import dev.jvmguard.ui.server.t

enum class TransactionMode(
    // English-only label is used by the JSON export
    val label: String,
    val dataType: TransactionDataType,
    val hasTimeLines: Boolean,
    val cumulateBacktraces: Boolean,
) {
    CALL_TREE("Call tree", TransactionDataType.TRANSACTION, hasTimeLines = true, cumulateBacktraces = false) {
        override fun fetch(connection: ServerConnection, cursor: TransactionCursor): TransactionTreeData =
            connection.getCallTree(cursor, false)
    },
    HOT_SPOTS("Hot spots", TransactionDataType.TRANSACTION, hasTimeLines = true, cumulateBacktraces = true) {
        override fun fetch(connection: ServerConnection, cursor: TransactionCursor): TransactionTreeData =
            connection.getHotspots(cursor, false)
    },
    OVERDUE("Overdue", TransactionDataType.OVERDUE, hasTimeLines = false, cumulateBacktraces = true) {
        override fun fetch(connection: ServerConnection, cursor: TransactionCursor): TransactionTreeData =
            connection.getHotspots(cursor, false)
    };

    val nameColumnHeader: String
        get() = when (this) {
            CALL_TREE -> t("transactions.column.transaction")
            HOT_SPOTS, OVERDUE -> t("transactions.column.hotSpot")
        }

    abstract fun fetch(connection: ServerConnection, cursor: TransactionCursor): TransactionTreeData
}
