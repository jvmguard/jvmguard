package dev.jvmguard.data.transactions

enum class TransactionDataType(currentCheckType: TransactionDataType?) {
    TRANSACTION(null),
    OVERDUE(TRANSACTION);

    val currentCheckType: TransactionDataType = currentCheckType ?: this

    val isRemoveEmpty: Boolean
        get() = this != OVERDUE
}
