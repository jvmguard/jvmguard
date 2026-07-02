package com.jvmguard.collector.vmdata.structures

import com.jvmguard.agent.tree.Tree.Visitor
import com.jvmguard.data.transactions.TransactionTree

class GroupMergeVisitor : Visitor<TransactionTree> {
    private lateinit var result: TransactionTree
    private lateinit var rootTree: TransactionTree

    fun init(rootTree: TransactionTree, result: TransactionTree): GroupMergeVisitor {
        this.rootTree = rootTree
        this.result = result
        return this
    }

    override fun preVisit(tree: TransactionTree): Boolean {
        if (tree !== rootTree) {
            result = result.getOrCreateChild(tree)
        }
        result.addTime(tree.time)
        result.addCount(tree.count)
        return true
    }

    override fun postVisit(tree: TransactionTree) {
        if (tree !== rootTree) {
            result = result.parent
        }
    }
}
