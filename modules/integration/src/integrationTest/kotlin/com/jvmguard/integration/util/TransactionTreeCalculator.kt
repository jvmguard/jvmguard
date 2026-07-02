package com.jvmguard.integration.util

import com.jvmguard.data.transactions.TransactionTree

object TransactionTreeCalculator {

    fun subtract(base: TransactionTree, other: TransactionTree?) {
        if (other != null) {
            base.addTime(-other.time)
            base.addCount(-other.count)
            for (otherChild in other) {
                val myChild = base.getOrCreateChild(otherChild)
                subtract(myChild, otherChild)
            }
            base.removeEmpty(false)
        }
    }
}
