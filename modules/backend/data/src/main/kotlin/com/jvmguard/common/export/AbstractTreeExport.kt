package com.jvmguard.common.export

import com.jvmguard.agent.tree.AbstractTransactionTree.PolicyType
import com.jvmguard.agent.tree.Tree.Visitor
import com.jvmguard.common.export.base.AbstractExport
import com.jvmguard.data.transactions.TransactionTree

abstract class AbstractTreeExport<T : AbstractTreeExport<T>> protected constructor(dataName: String) :
    AbstractExport<T>(dataName) {

    fun exportTransactionTree(transactionTree: TransactionTree) {
        transactionTree.visit(isSorted(), object : Visitor<TransactionTree> {
            override fun preVisit(tree: TransactionTree): Boolean {
                val generator = gen!!
                if (tree !== transactionTree) {
                    generator.writeStartObject()
                    generator.write("name", tree.name)
                    if (includeTime) {
                        generator.write("time", convertNanos(tree.time))
                    }
                    generator.write("count", tree.count)

                    val transactionType = tree.transactionType
                    generator.write("type", transactionType.name.lowercase())

                    if (supportsOptional()) {
                        val policyType = tree.policyType
                        if (policyType != PolicyType.NORMAL) {
                            generator.write("policy", policyType.toString().lowercase())
                            if (policyType == PolicyType.ERROR) {
                                generator.write("error", tree.policyTypeString)
                            }
                        }
                    } else {
                        generator.write("policy", tree.getVerbosePolicyType(true))
                    }
                }
                if (tree.childCount > 0) {
                    generator.writeStartArray("children")
                }
                return true
            }

            override fun postVisit(tree: TransactionTree) {
                val generator = gen!!
                if (tree.childCount > 0) {
                    generator.writeEndArray()
                }
                if (tree !== transactionTree) {
                    generator.writeEndObject()
                }
            }
        })
    }
}
