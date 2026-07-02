package com.jvmguard.collector.vmdata.components

import com.jvmguard.agent.config.transactions.PolicyEventType
import com.jvmguard.data.config.triggers.PolicyTrigger

internal class Count {
    private val values = LongArray(LENGTH)

    fun count(type: PolicyEventType, count: Long) {
        values[type.ordinal] += count
    }

    fun getValue(trigger: PolicyTrigger): Long {
        var result: Long = 0
        if (trigger.isNormal) {
            result += values[PolicyEventType.NORMAL.ordinal]
        }
        if (trigger.isSlow) {
            result += values[PolicyEventType.SLOW.ordinal]
        }
        if (trigger.isVerySlow) {
            result += values[PolicyEventType.VERY_SLOW.ordinal]
        }
        if (trigger.isOverdue) {
            result += values[PolicyEventType.OVERDUE.ordinal]
        }
        if (trigger.isError) {
            result += values[PolicyEventType.ERROR.ordinal]
        }
        return result
    }

    companion object {
        private val LENGTH = PolicyEventType.entries.size
    }
}
