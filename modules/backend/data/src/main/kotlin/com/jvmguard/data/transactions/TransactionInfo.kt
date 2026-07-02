package com.jvmguard.data.transactions

import com.jvmguard.agent.config.transactions.TransactionType
import java.util.Objects

open class TransactionInfo(
    val name: String?,
    val type: TransactionType?,
) : Comparable<TransactionInfo> {

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is TransactionInfo) {
            return false
        }
        if (!Objects.equals(name, other.name)) {
            return false
        }
        if (type != other.type) {
            return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = name?.hashCode() ?: 0
        result = 31 * result + (type?.hashCode() ?: 0)
        return result
    }

    override fun compareTo(other: TransactionInfo): Int {
        if (type != null && other.type != null) {
            val value = type.compareTo(other.type)
            if (value != 0) {
                return value
            }
        }
        if (name != null && other.name != null) {
            return name.compareTo(other.name)
        }
        return 0
    }

    override fun toString(): String = "${type?.name}: $name"
}
