package dev.jvmguard.data.transactions

import dev.jvmguard.agent.config.transactions.TransactionType
import dev.jvmguard.agent.tree.AbstractTransactionTree
import java.util.*

class TransactionTreeIdentifier(
    name: String?,
    type: TransactionType?,
    private val policyTypeString: String?,
) : TransactionInfo(name, type) {

    val transactionInfo: TransactionInfo
        get() = TransactionInfo(name, type)

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        if (!super.equals(other)) {
            return false
        }
        val that = other as TransactionTreeIdentifier
        return Objects.equals(policyTypeString, that.policyTypeString)
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (policyTypeString?.hashCode() ?: 0)
        return result
    }

    override fun compareTo(other: TransactionInfo): Int {
        val value = super.compareTo(other)
        if (value == 0 && policyTypeString != null && other is TransactionTreeIdentifier) {
            if (other.policyTypeString != null) {
                return policyTypeString.compareTo(other.policyTypeString)
            }
        }
        return value
    }

    override fun toString(): String {
        val policy = AbstractTransactionTree.getVerbosePolicyType(policyTypeString, true, true)
        val name = super.toString()
        return if (policy == null) {
            name
        } else {
            "[$policy] $name"
        }
    }
}
