package dev.jvmguard.data.vmdata

import dev.jvmguard.data.base.Interval

enum class SparkLineRange(
    private val verbose: String,
    val rangeInterval: Interval,
    val numberOfPoints: Int,
    val dataInterval: Interval,
) {
    LAST_HOUR("Last hour", Interval.HOUR, 60, Interval.MINUTE),
    LAST_DAY("Last day", Interval.DAY, 48, Interval.HALF_AN_HOUR);

    override fun toString(): String = verbose
}
