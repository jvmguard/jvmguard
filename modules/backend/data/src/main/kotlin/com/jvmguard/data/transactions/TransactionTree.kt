package com.jvmguard.data.transactions

import com.jvmguard.agent.comm.CommunicationContext
import com.jvmguard.agent.config.transactions.TransactionType
import com.jvmguard.agent.tree.AbstractTransactionTree
import java.io.DataInputStream
import java.io.DataOutputStream

class TransactionTree : AbstractTransactionTree<String, TransactionTree> {

    private var transactionTypeValue: TransactionType = TransactionType.MATCHED
    var nameId: Long = 0

    constructor()

    constructor(parent: TransactionTree?, name: String?, transactionType: TransactionType, policyType: String?) :
            super(parent, name, policyType) {
        this.transactionTypeValue = transactionType
    }

    constructor(name: String?, transactionType: TransactionType, policyType: String?) : super(name, policyType) {
        this.transactionTypeValue = transactionType
    }

    override fun getName(): String? = getInfo()

    override fun getTransactionType(): TransactionType = transactionTypeValue

    fun init(nameId: Long, name: String?, transactionType: TransactionType, policyTypeString: String?): TransactionTree {
        this.nameId = nameId
        info = name
        this.transactionTypeValue = transactionType
        this.policyTypeString = policyTypeString
        return this
    }

    protected override fun writeEntry(context: CommunicationContext, out: DataOutputStream, info: String) {
        throw UnsupportedOperationException()
    }

    protected override fun readEntry(context: CommunicationContext, `in`: DataInputStream): String {
        throw UnsupportedOperationException()
    }

    protected override fun createChildInt(lookupTree: TransactionTree?): TransactionTree {
        return if (lookupTree == null) {
            TransactionTree(this, null, TransactionType.MATCHED, null)
        } else {
            TransactionTree(this, lookupTree.getName(), lookupTree.getTransactionType(), lookupTree.getPolicyTypeString())
                .apply { nameId = lookupTree.nameId }
        }
    }

    fun removeEmpty(checkTime: Boolean) {
        val currentChildren = children ?: return
        val iterator = currentChildren.keys.iterator()
        while (iterator.hasNext()) {
            val tree = iterator.next()
            if (tree.isVm) {
                if (tree.childCount == 0) {
                    iterator.remove()
                } else {
                    tree.removeEmpty(checkTime)
                }
            } else {
                if (tree.count == 0L && (!checkTime || tree.time == 0L)) {
                    iterator.remove()
                } else {
                    tree.removeEmpty(checkTime)
                }
            }
        }
    }

    val isVm: Boolean
        get() = transactionTypeValue == TransactionType.VM

    override fun toString(): String {
        val ret = StringBuilder()
        visit(PrintVisitor(ret))
        return ret.toString()
    }

    fun getIdentifier(): TransactionTreeIdentifier =
        TransactionTreeIdentifier(getName(), getTransactionType(), getPolicyTypeString())

    fun getTransactionInfo(): TransactionInfo =
        TransactionInfo(getName(), getTransactionType())

    override fun compareTo(other: TransactionTree): Int {
        var value = compareValues(transactionTypeValue, other.transactionTypeValue)
        if (value == 0) {
            value = compareValues(getName(), other.getName())
            if (value == 0) {
                value = compareValues(getPolicyTypeString(), other.getPolicyTypeString())
            }
        }
        return value
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + transactionTypeValue.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (!super.equals(other)) {
            return false
        }
        return (other as AbstractTransactionTree<*, *>).getTransactionType() == transactionTypeValue
    }

    class PrintVisitor(appendable: Appendable) :
        AbstractTransactionTreePrintVisitor<TransactionTree>(appendable) {

        override fun getAdditionalLines(tree: TransactionTree): List<String> = super.getAdditionalLines(tree)
    }
}
