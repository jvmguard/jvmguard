package dev.jvmguard.collector.transactions.util

import dev.jvmguard.agent.tree.AgentTransactionTree
import dev.jvmguard.agent.tree.Tree.Visitor
import dev.jvmguard.collector.transactions.util.AbstractNameManager.NameWriteStatements

class AgentTreeProcessor(
    private val nameManager: TypedNameManager,
    private val nameWriteStatements: NameWriteStatements,
    private val capReceiver: CapReceiver,
) {

    fun preProcess(transactionTree: AgentTransactionTree): ProcessedAgentTree {
        val destinationRoot = ProcessedAgentTree()
        transactionTree.visit(object : Visitor<AgentTransactionTree> {
            private var currentDestination = destinationRoot
            private val lookupTree = ProcessedAgentTree()

            override fun preVisit(tree: AgentTransactionTree): Boolean {
                if (tree !== transactionTree) {
                    var name = tree.name
                    val nameId = nameManager.getCappedNameId(name, NameType.TRANSACTION, nameWriteStatements)
                    if (nameManager.isCappedId(nameId)) {
                        capReceiver.hitTransactionCap(name)
                        name = TypedNameManager.CAPPED_DESCRIPTION
                    }
                    currentDestination = currentDestination.getOrCreateChild(lookupTree.init(name, tree.policyTypeString, tree.transactionType, nameId))
                    currentDestination.addTime(tree.time)
                    currentDestination.addCount(tree.count)
                }
                return true
            }

            override fun postVisit(tree: AgentTransactionTree) {
                if (tree !== transactionTree) {
                    currentDestination = currentDestination.parent
                }
            }
        })
        return destinationRoot
    }

    fun interface CapReceiver {
        fun hitTransactionCap(name: String?)
    }
}
