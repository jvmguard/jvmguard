package com.jvmguard.collector.threshold

import com.jvmguard.data.vmdata.PersistentTelemetryIdentifier
import com.jvmguard.data.vmdata.ThresholdIdentifier

class ViolationEvent(telemetryIdentifier: PersistentTelemetryIdentifier, customName: String, val isTooHigh: Boolean) {

    val thresholdIdentifier: ThresholdIdentifier = ThresholdIdentifier(telemetryIdentifier, customName)

    override fun toString(): String {
        return "ViolationEvent{thresholdIdentifier=$thresholdIdentifier, tooHigh=$isTooHigh}"
    }
}
