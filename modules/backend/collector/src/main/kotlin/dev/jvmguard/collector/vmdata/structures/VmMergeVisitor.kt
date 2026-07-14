package dev.jvmguard.collector.vmdata.structures

import dev.jvmguard.agent.config.transactions.TransactionType
import dev.jvmguard.agent.tree.Tree.Visitor
import dev.jvmguard.collector.transactions.util.ProcessedAgentTree
import dev.jvmguard.data.transactions.TransactionTree

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
        return lookupTree.init(tree.nameId, tree.name, tree.transactionType ?: TransactionType.MATCHED, tree.policyTypeString)
    }
}
