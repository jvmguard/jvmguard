package com.jvmguard.mcp.tool

import com.jvmguard.agent.tree.AbstractTransactionTree.PolicyType
import com.jvmguard.data.transactions.TransactionTree

object McpTransactionTree {

    private const val TIME_UNIT = "microseconds"

    /** @param rootKey the tool-specific name of the root array ("callTree", "hotSpots", "overdue"). */
    fun toResult(rootKey: String, root: TransactionTree): Map<String, Any?> {
        val topNodes = sortedChildren(root)
        val uniformType = uniformTypeOrNull(topNodes)
        return buildMap {
            put("timeUnit", TIME_UNIT)
            uniformType?.let { put("type", it) }
            put(rootKey, topNodes.map { node(it, omitType = uniformType != null) })
        }
    }

    private fun node(tree: TransactionTree, omitType: Boolean): Map<String, Any?> = buildMap {
        put("name", tree.name)
        put("count", tree.count)
        put("totalMicros", micros(tree.time))
        put("selfMicros", micros(tree.inherentTime))
        if (tree.count > 0) {
            put("avgMicros", Math.round(tree.time.toDouble() / tree.count / 1000.0))
        }
        if (!omitType) {
            put("type", tree.transactionType.name.lowercase())
        }
        val policyType = tree.policyType
        if (policyType != PolicyType.NORMAL) {
            put("policy", policyType.name.lowercase())
            if (policyType == PolicyType.ERROR) {
                put("error", tree.policyTypeString)
            }
        }
        val children = sortedChildren(tree)
        if (children.isNotEmpty()) {
            put("children", children.map { node(it, omitType) })
        }
    }

    private fun sortedChildren(tree: TransactionTree): List<TransactionTree> =
        tree.children().sortedByDescending { it.time }

    /** The single type shared by every node, or null when the tree mixes types. */
    private fun uniformTypeOrNull(topNodes: List<TransactionTree>): String? {
        val types = HashSet<String>()
        fun visit(tree: TransactionTree) {
            types.add(tree.transactionType.name.lowercase())
            tree.children().forEach(::visit)
        }
        topNodes.forEach(::visit)
        return types.singleOrNull()
    }

    private fun micros(nanos: Long): Long = Math.round(nanos / 1000.0)
}
