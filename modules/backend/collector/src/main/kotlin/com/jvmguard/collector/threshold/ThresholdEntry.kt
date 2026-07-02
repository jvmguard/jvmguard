package com.jvmguard.collector.threshold

import com.jvmguard.data.config.thresholds.Threshold

class ThresholdEntry(
    private val threshold: Threshold,
    private val lowerBound: Long,
    private val upperBound: Long,
    private var lastTimeTriggeredNanos: Long,
) {
    private var firstTimeReachedNanos = Long.MIN_VALUE
    private var inInhibitableEvent = false

    @Synchronized
    fun check(nanoTime: Long, value: Long): ViolationEvent? {
        var violationEvent: ViolationEvent? = null

        val telemetryIdentifier = threshold.telemetryIdentifier ?: return null
        val tooLow = value < lowerBound
        val tooHigh = value > upperBound
        if (tooLow || tooHigh) {
            if (firstTimeReachedNanos == Long.MIN_VALUE) {
                firstTimeReachedNanos = nanoTime
            }
            if (!inInhibitableEvent &&
                nanoTime - firstTimeReachedNanos >= threshold.minimumTimeUnit.getNanos(threshold.minimumTime) &&
                (lastTimeTriggeredNanos == Long.MIN_VALUE || nanoTime - lastTimeTriggeredNanos >= threshold.inhibitDuplicateTimeUnit.getNanos(threshold.inhibitDuplicateTime))
            ) {
                violationEvent = ViolationEvent(telemetryIdentifier, threshold.customName.usedValue, tooHigh)
                lastTimeTriggeredNanos = nanoTime
                inInhibitableEvent = threshold.isInhibitDuplicateForContinuousViolation
            }
        } else {
            inInhibitableEvent = false
            firstTimeReachedNanos = Long.MIN_VALUE
        }
        return violationEvent
    }
}
