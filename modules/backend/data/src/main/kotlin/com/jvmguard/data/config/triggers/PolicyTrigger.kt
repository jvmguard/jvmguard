package com.jvmguard.data.config.triggers

import com.jvmguard.agent.config.transactions.ComparisonType
import com.jvmguard.agent.helper.matcher.RegexPatternMatcher
import java.util.regex.Pattern

open class PolicyTrigger : DataTrigger() {

    var filter: String = "*"
        @Synchronized set(value) {
            val old = field
            field = value
            pattern = null
            fireChanged(old, value)
        }

    var comparisonType: ComparisonType = ComparisonType.WILDCARD
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    private var normal: Boolean = false
    private var slow: Boolean = false
    private var verySlow: Boolean = true
    private var overdue: Boolean = false
    private var error: Boolean = false

    var isNormal: Boolean
        get() = normal
        set(value) {
            val old = normal
            normal = value
            fireChanged(old, value)
        }

    var isSlow: Boolean
        get() = slow
        set(value) {
            val old = slow
            slow = value
            fireChanged(old, value)
        }

    var isVerySlow: Boolean
        get() = verySlow
        set(value) {
            val old = verySlow
            verySlow = value
            fireChanged(old, value)
        }

    var isOverdue: Boolean
        get() = overdue
        set(value) {
            val old = overdue
            overdue = value
            fireChanged(old, value)
        }

    var isError: Boolean
        get() = error
        set(value) {
            val old = error
            error = value
            fireChanged(old, value)
        }

    @Transient
    private var pattern: Pattern? = null

    fun isReplaceable(policyTrigger: PolicyTrigger): Boolean =
        isIdenticalCounterType(policyTrigger) &&
                filter == policyTrigger.filter &&
                comparisonType == policyTrigger.comparisonType &&
                normal == policyTrigger.normal &&
                slow == policyTrigger.slow &&
                verySlow == policyTrigger.verySlow &&
                error == policyTrigger.error &&
                overdue == policyTrigger.overdue

    override val triggerType: TriggerType
        get() = TriggerType.POLICY

    override val description: String
        get() {
            val states = when {
                normal && slow && verySlow && error && overdue -> "all"
                !normal && !slow && !verySlow && !error && !overdue -> "no"
                else -> buildList {
                    if (normal) add("normal")
                    if (slow) add("slow")
                    if (verySlow) add("very slow")
                    if (error) add("error")
                    if (overdue) add("overdue")
                }.joinToString(",")
            }
            val perInterval = if (interval == Interval.NONE) "" else " per $interval"
            return "$triggerType [$filter: $count $states events$perInterval]"
        }

    fun matches(transactionName: String): Boolean = pattern().matcher(transactionName).matches()

    @Synchronized
    fun pattern(): Pattern =
        pattern ?: run {
            val effectiveFilter = if (comparisonType == ComparisonType.WILDCARD) {
                RegexPatternMatcher.convertToWildcardFilter(filter, false)
            } else {
                filter
            }
            Pattern.compile(effectiveFilter).also { pattern = it }
        }

    override fun toString(): String =
        "PolicyTrigger{filter='$filter', comparisonType=$comparisonType, " +
                "normal=$normal, slow=$slow, verySlow=$verySlow, overdue=$overdue, error=$error}"
}
