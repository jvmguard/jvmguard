package dev.jvmguard.collector.telemetry

import dev.jvmguard.common.helper.Timestamp.Interval
import java.util.*

class PlainTelemetryData {
    var timeStamps: LongArray? = null
    val subIdToData: MutableMap<String, LongArray> = HashMap()

    var isNoPreviousData: Boolean = false
    var dataInterval: Interval? = null

    override fun toString(): String =
        "PlainTelemetryData{timeStamps=" + timeStamps.contentToString() + ", subIdToData=" + subIdToData + "}"
}
