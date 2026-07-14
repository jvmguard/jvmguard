package dev.jvmguard.collector.transactions.provider

import dev.jvmguard.agent.tree.AbstractTransactionTree.PolicyType
import dev.jvmguard.agent.tree.Tree.Visitor
import dev.jvmguard.collector.transactions.util.Percentage
import dev.jvmguard.common.transactions.TransactionCursorImpl
import dev.jvmguard.data.transactions.TransactionInfo
import dev.jvmguard.data.transactions.TransactionTree
import dev.jvmguard.data.transactions.TransactionTreeData
import java.util.*

internal class RawTransactionData(
    private val transactionCursor: TransactionCursorImpl,
    private val transactionTree: TransactionTree,
    private val containedVmIds: LongArray,
    percentage: Percentage?,
) {
    private val minIntervalPercentage: Int = percentage?.minIntervalPercentage ?: 0
    private val maxIntervalPercentage: Int = percentage?.maxIntervalPercentage ?: 0

    fun getTransactionInfo(): Set<TransactionInfo> {
        val ret = HashSet<TransactionInfo>()
        transactionTree.visit(object : Visitor<TransactionTree> {
            override fun preVisit(tree: TransactionTree): Boolean {
                if (tree.name != null) {
                    ret.add(TransactionInfo(tree.name, tree.transactionType))
                }
                return true
            }

            override fun postVisit(tree: TransactionTree) {
            }
        })
        return ret
    }

    fun calculateTransactionTree(mergePolicies: Boolean): TransactionTreeData {
        val mergedTree = TransactionTree()
        val lookupTree = TransactionTree()
        transactionTree.visit(object : Visitor<TransactionTree> {
            private var currentMergedTree = mergedTree

            override fun preVisit(tree: TransactionTree): Boolean {
                if (tree !== transactionTree) {
                    currentMergedTree = getOrCreateChild(tree, currentMergedTree, mergePolicies, lookupTree)
                    currentMergedTree.addData(tree)
                }
                return true
            }

            override fun postVisit(tree: TransactionTree) {
                if (tree !== transactionTree) {
                    currentMergedTree = currentMergedTree.parent
                }
            }
        })
        return TransactionTreeData(transactionCursor, containedVmIds, minIntervalPercentage, maxIntervalPercentage, mergedTree, false, mergePolicies)
    }

    fun calculateHotspotTree(mergePolicies: Boolean): TransactionTreeData {
        val hotspotTree = TransactionTree()

        val hotspots = HashMap<TransactionInfo, TransactionTree>()
        val deque: Deque<TransactionTree> = ArrayDeque()

        transactionTree.visit(object : Visitor<TransactionTree> {
            private val lookupTree = TransactionTree()

            override fun preVisit(tree: TransactionTree): Boolean {
                if (tree !== transactionTree) {
                    val inherentValue = tree.inherentTime
                    val count = tree.count

                    if (count > 0 || inherentValue > 0) {
                        val transactionInfo = TransactionInfo(tree.name, tree.transactionType)
                        val hotspot = hotspots.computeIfAbsent(transactionInfo) { i ->
                            hotspotTree.getOrCreateChild(TransactionTree(i.name, i.type!!, PolicyType.NORMAL.typeString))
                        }
                        hotspot.addTime(inherentValue)
                        hotspot.addCount(count)

                        var myTree = hotspot
                        for (newTree in deque) {
                            myTree = getOrCreateChild(newTree, myTree, mergePolicies, lookupTree)
                            myTree.addTime(inherentValue)
                            myTree.addCount(count)
                        }
                    }
                    deque.push(tree)
                }
                return true
            }

            override fun postVisit(tree: TransactionTree) {
                if (tree !== transactionTree) {
                    deque.pop()
                }
            }
        })
        return TransactionTreeData(transactionCursor, containedVmIds, minIntervalPercentage, maxIntervalPercentage, hotspotTree, true, mergePolicies)
    }

    private fun getOrCreateChild(
        visitedTree: TransactionTree,
        currentMergedTree: TransactionTree,
        mergePolicies: Boolean,
        lookupTree: TransactionTree
    ): TransactionTree {
        val transactionType = visitedTree.transactionType
        return currentMergedTree.getOrCreateChild(
            lookupTree.init(
                0,
                visitedTree.name,
                transactionType,
                if (mergePolicies) PolicyType.NORMAL.typeString else visitedTree.policyTypeString
            )
        )
    }
}
