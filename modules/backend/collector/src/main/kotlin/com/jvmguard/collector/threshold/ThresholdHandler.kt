package com.jvmguard.collector.threshold

import com.jvmguard.agent.config.telemetry.TelemetryUnit
import com.jvmguard.data.config.thresholds.Threshold
import com.jvmguard.data.vmdata.TelemetryType

class ThresholdHandler(threshold: Threshold, private val telemetryType: TelemetryType, lastTimeTriggeredNanos: Long) {

    private var entries: MutableList<ThresholdEntry> =
        mutableListOf(createEntry(threshold, telemetryType, lastTimeTriggeredNanos))

    fun addThreshold(threshold: Threshold, lastTimeTriggeredNanos: Long) {
        entries.add(createEntry(threshold, telemetryType, lastTimeTriggeredNanos))
    }

    fun check(nanoTime: Long, value: Long): List<ViolationEvent>? {
        var usedValue = value
        if (usedValue == Long.MIN_VALUE) {
            if (TelemetryUnit.isExtentOfTime(telemetryType.unit)) {
                return null
            } else {
                usedValue = 0
            }
        }

        var result: MutableList<ViolationEvent>? = null
        for (entry in entries) {
            val event = entry.check(nanoTime, usedValue)
            if (event != null) {
                if (result == null) {
                    result = ArrayList()
                }
                result.add(event)
            }
        }
        return result
    }

    companion object {
        private fun createEntry(threshold: Threshold, telemetryType: TelemetryType, lastTimeTriggeredNanos: Long): ThresholdEntry {
            val lowerBound = if (threshold.isLowerBoundEnabled) {
                telemetryType.scaleThreshold(threshold.lowerBound, threshold.lowerBoundUnitLevel)
            } else {
                Long.MIN_VALUE
            }
            val upperBound = if (threshold.isUpperBoundEnabled) {
                telemetryType.scaleThreshold(threshold.upperBound, threshold.upperBoundUnitLevel)
            } else {
                Long.MAX_VALUE
            }
            return ThresholdEntry(threshold, lowerBound, upperBound, lastTimeTriggeredNanos)
        }
    }
}
