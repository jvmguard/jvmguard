package dev.jvmguard.collector.threshold

import dev.jvmguard.data.vmdata.PersistentTelemetryIdentifier
import dev.jvmguard.data.vmdata.ThresholdIdentifier

class ViolationEvent(telemetryIdentifier: PersistentTelemetryIdentifier, customName: String, val isTooHigh: Boolean) {

    val thresholdIdentifier: ThresholdIdentifier = ThresholdIdentifier(telemetryIdentifier, customName)

    override fun toString(): String {
        return "ViolationEvent{thresholdIdentifier=$thresholdIdentifier, tooHigh=$isTooHigh}"
    }
}
