package dev.jvmguard.collector.transactions.util

import dev.jvmguard.agent.tree.AbstractTransactionTree
import dev.jvmguard.agent.tree.AbstractTransactionTree.PolicyType
import dev.jvmguard.agent.tree.Tree.Visitor

class CountVisitor<T, U : AbstractTransactionTree<T, U>> : Visitor<U> {
    val totalCount = Count()

    override fun preVisit(tree: U): Boolean {
        if (tree.name != null) {
            totalCount.count(tree.policyType, tree.count, tree.time)
        }
        return true
    }

    override fun postVisit(tree: U) {
    }

    class Count {
        var time = 0L
            private set
        var normal = 0L
            private set
        var slow = 0L
            private set
        var verySlow = 0L
            private set
        var error = 0L
            private set

        internal fun count(policyType: PolicyType, count: Long, time: Long) {
            this.time += time
            when (policyType) {
                PolicyType.NORMAL -> normal += count
                PolicyType.SLOW -> slow += count
                PolicyType.VERY_SLOW -> verySlow += count
                PolicyType.ERROR -> error += count
                else -> {}
            }
        }

        val completed: Long
            get() = normal + slow + verySlow + error

        fun getAverageTime(zeroForUnavailable: Boolean): Long {
            val count = normal + slow + verySlow + error
            return if (count > 0) {
                time / count
            } else {
                if (zeroForUnavailable) 0 else Long.MIN_VALUE
            }
        }
    }
}
