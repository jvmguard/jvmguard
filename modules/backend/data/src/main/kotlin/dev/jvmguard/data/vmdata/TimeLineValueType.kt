package dev.jvmguard.data.vmdata

import dev.jvmguard.agent.config.telemetry.TelemetryUnit

interface TimeLineValueType {
    fun getTelemetryUnit(): TelemetryUnit
    fun getTelemetryScale(): Int
}
