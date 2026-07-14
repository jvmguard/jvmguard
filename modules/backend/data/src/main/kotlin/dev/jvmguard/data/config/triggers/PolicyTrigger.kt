package dev.jvmguard.data.config.triggers

import dev.jvmguard.agent.config.base.ConfigDoc
import dev.jvmguard.agent.config.transactions.ComparisonType
import dev.jvmguard.agent.helper.matcher.RegexPatternMatcher
import java.util.regex.Pattern

@ConfigDoc("Fires on transaction policy events (slow / very slow / overdue / error).")
open class PolicyTrigger : DataTrigger() {

    @field:ConfigDoc("Transaction-name filter selecting which transactions' policy events are counted.")
    var filter: String = "*"
        @Synchronized set(value) {
            pattern = null
            field = changed(field, value)
        }

    @field:ConfigDoc("Whether filter is wildcard or regex.")
    var comparisonType: ComparisonType = ComparisonType.WILDCARD
        set(value) { field = changed(field, value) }

    @field:ConfigDoc("Count NORMAL-status policy events.")
    private var normal: Boolean = false
    @field:ConfigDoc("Count SLOW policy events.")
    private var slow: Boolean = false
    @field:ConfigDoc("Count VERY-SLOW policy events.")
    private var verySlow: Boolean = true
    @field:ConfigDoc("Count OVERDUE policy events.")
    private var overdue: Boolean = false
    @field:ConfigDoc("Count ERROR policy events.")
    private var error: Boolean = false

    var isNormal: Boolean
        get() = normal
        set(value) { normal = changed(normal, value) }

    var isSlow: Boolean
        get() = slow
        set(value) { slow = changed(slow, value) }

    var isVerySlow: Boolean
        get() = verySlow
        set(value) { verySlow = changed(verySlow, value) }

    var isOverdue: Boolean
        get() = overdue
        set(value) { overdue = changed(overdue, value) }

    var isError: Boolean
        get() = error
        set(value) { error = changed(error, value) }

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
