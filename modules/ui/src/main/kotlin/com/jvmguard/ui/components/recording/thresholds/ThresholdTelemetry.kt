package com.jvmguard.ui.components.recording.thresholds

import com.jvmguard.data.config.thresholds.Threshold
import com.jvmguard.data.vmdata.PersistentTelemetryIdentifier
import com.jvmguard.data.vmdata.TelemetryType

fun thresholdDisplayName(threshold: Threshold, types: Collection<TelemetryType>): String {
    if (threshold.customName.isChecked && threshold.customName.value.isNotBlank()) {
        return threshold.customName.value
    }
    return telemetryTypeOf(threshold.telemetryIdentifier, types)?.name
        ?: threshold.telemetryIdentifier?.combinedId
        ?: "(no telemetry)"
}

fun telemetryTypeOf(id: PersistentTelemetryIdentifier?, types: Collection<TelemetryType>): TelemetryType? {
    if (id == null) {
        return null
    }
    return types.firstOrNull {
        val other = it.telemetryIdentifier
        other.combinedId == id.combinedId &&
                other.additionalType == id.additionalType &&
                other.additionalName == id.additionalName
    }
}
