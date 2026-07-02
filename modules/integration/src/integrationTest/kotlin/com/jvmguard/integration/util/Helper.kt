package com.jvmguard.integration.util

import com.jvmguard.agent.tree.AbstractTransactionTree.PolicyType

object Helper {

    fun getTypeString(str: String?): String {
        if (str == null) {
            return PolicyType.NORMAL.typeString
        }
        for (policyType in PolicyType.entries) {
            if (policyType.toString().lowercase() == str) {
                return policyType.typeString
            }
        }
        throw IllegalArgumentException("unknown type $str")
    }
}
