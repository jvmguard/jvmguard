package com.jvmguard.collector.transactions.util

import com.jvmguard.agent.comm.CommunicationContext
import com.jvmguard.agent.config.transactions.TransactionType
import com.jvmguard.agent.tree.AbstractTransactionTree
import com.jvmguard.agent.util.JvmGuardUtil
import java.io.DataInputStream
import java.io.DataOutputStream

class ProcessedAgentTree : AbstractTransactionTree<String, ProcessedAgentTree> {

    private var transactionTypeValue: TransactionType? = null

    var nameId: Long = 0
        private set

    private constructor(parent: ProcessedAgentTree?, name: String?, policyTypeString: String?, transactionType: TransactionType?, nameId: Long) : super(
        parent,
        name,
        policyTypeString
    ) {
        this.transactionTypeValue = transactionType
        this.nameId = nameId
    }

    constructor()

    fun init(name: String?, policyTypeString: String?, transactionType: TransactionType?, nameId: Long): ProcessedAgentTree {
        info = name
        this.policyTypeString = policyTypeString
        this.transactionTypeValue = transactionType
        this.nameId = nameId
        return this
    }

    override fun getName(): String? = info

    override fun getTransactionType(): TransactionType? = transactionTypeValue

    override fun writeEntry(context: CommunicationContext, out: DataOutputStream, info: String?) {
        throw UnsupportedOperationException()
    }

    override fun readEntry(context: CommunicationContext, `in`: DataInputStream): String {
        throw UnsupportedOperationException()
    }

    override fun createChildInt(lookupTree: ProcessedAgentTree?): ProcessedAgentTree =
        if (lookupTree == null) {
            ProcessedAgentTree()
        } else {
            ProcessedAgentTree(this, lookupTree.info, lookupTree.policyTypeString, lookupTree.transactionTypeValue, lookupTree.nameId)
        }

    override fun compareTo(other: ProcessedAgentTree): Int {
        var value = JvmGuardUtil.compareNullable(transactionTypeValue, other.transactionTypeValue)
        if (value == 0) {
            value = nameId.compareTo(other.nameId)
            if (value == 0) {
                value = JvmGuardUtil.compareNullable(policyTypeString, other.policyTypeString)
            }
        }
        return value
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (transactionTypeValue?.hashCode() ?: 0)
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (!super.equals(other)) {
            return false
        }
        return (other as AbstractTransactionTree<*, *>).transactionType === transactionTypeValue
    }
}
