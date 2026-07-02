package com.jvmguard.ui.views.data.transactions

import com.jvmguard.agent.config.transactions.TransactionType
import com.jvmguard.agent.tree.AbstractTransactionTree.PolicyType
import com.jvmguard.data.transactions.TransactionTree
import com.jvmguard.data.transactions.TransactionTreeData
import com.jvmguard.data.transactions.TransactionTreeIdentifier

class TransactionNode private constructor(
    private val tree: TransactionTree?,
    val topLevel: Boolean,
    val children: List<TransactionNode>,
) {
    /** synthetic grouping node  */
    val isContainer: Boolean get() = tree == null

    private val aggregateTime: Long by lazy { children.sumOf { it.time } }
    private val aggregateCount: Long by lazy { children.sumOf { it.count } }

    val name: String get() = tree?.name ?: BACKTRACE_CONTAINER
    val transactionType: TransactionType? get() = tree?.transactionType
    val policyType: PolicyType get() = tree?.policyType ?: PolicyType.NORMAL
    val time: Long get() = tree?.time ?: aggregateTime
    val count: Long get() = tree?.count ?: aggregateCount
    val averageTime: Long get() = if (count > 0) time / count else 0

    // Only meaningful for top-level transactions
    val identifier: TransactionTreeIdentifier get() = tree!!.getIdentifier()

    fun filtered(matches: (TransactionNode) -> Boolean): TransactionNode? {
        val keptChildren = children.mapNotNull { it.filtered(matches) }
        // A container is kept only when it still holds matching backtraces
        val selfMatches = tree != null && matches(this)
        return if (keptChildren.isNotEmpty() || selfMatches) {
            TransactionNode(tree, topLevel, keptChildren)
        } else {
            null
        }
    }

    /** The policy prefix V1 renders in bold before the name for non-normal transactions. */
    val policyPrefix: String?
        get() {
            val tree = this.tree ?: return null
            return when (val policy = policyType) {
                PolicyType.NORMAL -> null
                PolicyType.ERROR -> "[Error: ${tree.policyTypeString}]"
                else -> policy.prefix.trim()
            }
        }

    companion object {

        const val BACKTRACE_CONTAINER = "Cumulated backtraces"

        fun roots(data: TransactionTreeData, cumulateBacktraces: Boolean): List<TransactionNode> {
            val root = data.transactionTree
            return root.children().map { topNode(it, cumulateBacktraces) }.sortedByDescending { it.time }
        }

        private fun topNode(tree: TransactionTree, cumulateBacktraces: Boolean): TransactionNode {
            val childNodes = tree.children().map { build(it) }.sortedByDescending { it.time }
            if (cumulateBacktraces && childNodes.isNotEmpty()) {
                val container = TransactionNode(null, topLevel = false, childNodes)
                return TransactionNode(tree, topLevel = true, listOf(container))
            }
            return TransactionNode(tree, topLevel = true, childNodes)
        }

        private fun build(tree: TransactionTree): TransactionNode {
            val children = tree.children().map { build(it) }.sortedByDescending { it.time }
            return TransactionNode(tree, topLevel = false, children)
        }
    }
}
