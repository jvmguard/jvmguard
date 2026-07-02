package com.jvmguard.data.vmdata

import com.jvmguard.agent.config.telemetry.TelemetryUnit

interface TimeLineValueType {
    fun getTelemetryUnit(): TelemetryUnit
    fun getTelemetryScale(): Int
}
