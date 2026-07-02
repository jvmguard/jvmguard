package com.jvmguard.integration.util

import com.jvmguard.data.transactions.TransactionTree
import java.util.concurrent.TimeUnit

class TransactionTreeComparator(val timeComparator: TimeComparator) {

    var isDifferential: Boolean = true
        private set

    val isIncludeTime: Boolean
        get() = timeComparator != TimeComparator.NONE

    val timeUnit: TimeUnit = TimeUnit.MILLISECONDS

    fun isEqual(tree1: TransactionTree, tree2: TransactionTree): Boolean {
        return try {
            checkContentEqual(tree1, tree2)
            true
        } catch (_: AssertionError) {
            false
        }
    }

    fun checkContentEqual(tree1: TransactionTree, tree2: TransactionTree) {
        if (tree1.count != tree2.count) {
            println("count not equal " + tree1.count + " != " + tree2.count)
            printTree(tree1)
            throw AssertionError()
        }

        if (!timeComparator.isEqual(tree1.time, tree1.count, tree2.time, tree2.count)) {
            println("time not equal " + tree1.time + " != " + tree2.time)
            printTree(tree1)
            throw AssertionError()
        }

        if (tree1.children().size != tree2.children().size) {
            println("child count not equal " + tree1.children().size + " != " + tree2.children().size)
            printTree(tree1)
            throw AssertionError()
        }

        try {
            for (child1 in tree1.children()) {
                val child2 = findTree(tree2.children(), child1)
                if (child2 == null) {
                    println("tree not found $child1")
                    throw AssertionError()
                } else {
                    checkContentEqual(child1, child2)
                }
            }
        } catch (e: AssertionError) {
            printTree(tree1)
            throw e
        }
    }

    private fun findTree(children: Iterable<TransactionTree>, child1: TransactionTree): TransactionTree? {
        for (child2 in children) {
            if (child2.name == child1.name &&
                child1.transactionType == child2.transactionType &&
                child1.policyTypeString == child2.policyTypeString
            ) {
                return child2
            }
        }
        return null
    }

    companion object {
        fun printTree(tree1: TransactionTree) {
            println(tree1.name + " TYPE: " + tree1.policyTypeString)
        }
    }
}
