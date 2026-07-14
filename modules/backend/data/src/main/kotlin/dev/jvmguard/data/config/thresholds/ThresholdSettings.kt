package dev.jvmguard.data.config.thresholds

import dev.jvmguard.agent.config.base.ConfigDoc
import dev.jvmguard.agent.config.base.DefaultConstructor
import dev.jvmguard.agent.config.base.OptionalConfig
import dev.jvmguard.data.config.thresholds.Threshold.Target

open class ThresholdSettings @DefaultConstructor constructor() : OptionalConfig(), Comparable<ThresholdSettings> {

    @field:ConfigDoc("The configured telemetry threshold rules for this group.")
    var thresholds: MutableList<Threshold> = ArrayList()
        set(value) {
            field = value
            fireChanged(false, true) // always fire, so only call setter if changed
        }

    val activeThresholdCount: Int
        get() = thresholds.count { it.isEnabled }

    val activeGroupThresholdCount: Int
        get() = thresholds.count { it.isEnabled && it.target == Target.GROUP }

    override fun compareTo(other: ThresholdSettings): Int =
        activeThresholdCount - other.activeThresholdCount
}
