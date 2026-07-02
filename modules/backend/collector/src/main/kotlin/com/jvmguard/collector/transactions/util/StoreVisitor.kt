package com.jvmguard.collector.transactions.util

import com.jvmguard.agent.config.transactions.TransactionType
import com.jvmguard.agent.tree.AbstractTransactionTree
import com.jvmguard.agent.tree.AbstractTransactionTree.PolicyType
import com.jvmguard.agent.tree.Tree.Visitor
import com.jvmguard.collector.transactions.TransactionManager
import com.jvmguard.collector.transactions.util.AbstractNameManager.NameWriteStatements
import com.jvmguard.collector.util.AccessibleByteArrayOutputStream
import com.jvmguard.data.transactions.TransactionTree
import java.io.DataOutputStream
import java.io.IOException
import java.sql.PreparedStatement

abstract class StoreVisitor<T : AbstractTransactionTree<*, T>>(
    private val nameManager: TypedNameManager,
    private val nameWriteStatements: NameWriteStatements,
    val writeStatement: PreparedStatement,
) : Visitor<T> {

    private val bos = AccessibleByteArrayOutputStream()

    var out: DataOutputStream? = DataOutputStream(bos)
        private set

    abstract fun getNameId(tree: T): Long
    protected abstract fun getTransactionType(tree: T): TransactionType?

    override fun preVisit(tree: T): Boolean {
        val transactionType = getTransactionType(tree)
        val out = out!!
        out.writeShort(transactionType?.id?.toInt() ?: 0)
        var nameId = getNameId(tree)
        if (nameId == 0L) {
            nameId = nameManager.getNameId(tree.name, NameType.TRANSACTION, nameWriteStatements)
        }
        TransactionManager.writeId(out, nameId)
        val policyType = PolicyType.getSpecialByString(tree.policyTypeString)
        val policyId = policyType?.id?.toLong() ?: nameManager.getNameId(tree.policyTypeString, NameType.POLICY, nameWriteStatements)
        TransactionManager.writeId(out, policyId)
        out.writeLong(tree.time)
        out.writeLong(tree.count)

        out.writeInt(tree.childCount)
        out.flush()
        return true
    }

    override fun postVisit(tree: T) {
    }

    fun getBuffer(): ByteArray {
        close()
        return bos.buffer
    }

    private fun close() {
        try {
            out?.let {
                it.close()
                out = null
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    fun size(): Int {
        close()
        return bos.size()
    }

    class StoreAgentTreeVisitor(nameManager: TypedNameManager, nameWriteStatements: NameWriteStatements, writeStatement: PreparedStatement) :
        StoreVisitor<ProcessedAgentTree>(nameManager, nameWriteStatements, writeStatement) {

        override fun getNameId(tree: ProcessedAgentTree): Long = tree.nameId

        override fun getTransactionType(tree: ProcessedAgentTree): TransactionType? = tree.transactionType
    }

    class StoreTransactionTreeVisitor(nameManager: TypedNameManager, nameWriteStatements: NameWriteStatements, writeStatement: PreparedStatement) :
        StoreVisitor<TransactionTree>(nameManager, nameWriteStatements, writeStatement) {

        override fun getNameId(tree: TransactionTree): Long = tree.nameId

        override fun getTransactionType(tree: TransactionTree): TransactionType = tree.transactionType
    }
}
