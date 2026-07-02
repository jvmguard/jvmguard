package com.jvmguard.collector.vmdata.structures

import com.jvmguard.agent.config.transactions.TransactionType
import com.jvmguard.agent.tree.Tree.Visitor
import com.jvmguard.collector.transactions.util.ProcessedAgentTree
import com.jvmguard.data.transactions.TransactionTree

class VmMergeVisitor : Visitor<ProcessedAgentTree> {

    private lateinit var result: TransactionTree
    private lateinit var rootTree: ProcessedAgentTree

    private val lookupTree = TransactionTree()

    fun init(rootTree: ProcessedAgentTree, result: TransactionTree): VmMergeVisitor {
        this.rootTree = rootTree
        this.result = result
        return this
    }

    override fun preVisit(tree: ProcessedAgentTree): Boolean {
        if (tree !== rootTree) {
            result = result.getOrCreateChild(initLookup(tree))
        }
        result.addTime(tree.time)
        result.addCount(tree.count)
        result.nameId = tree.nameId
        return true
    }

    override fun postVisit(tree: ProcessedAgentTree) {
        if (tree !== rootTree) {
            result = result.parent
        }
    }

    private fun initLookup(tree: ProcessedAgentTree): TransactionTree {
        return lookupTree.init(tree.nameId, tree.name, tree.transactionType ?: TransactionType.POJO, tree.policyTypeString)
    }
}
