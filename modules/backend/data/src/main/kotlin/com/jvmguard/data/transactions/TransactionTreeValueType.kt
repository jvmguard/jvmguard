package com.jvmguard.data.transactions

import com.jvmguard.agent.config.telemetry.TelemetryUnit
import com.jvmguard.data.vmdata.TimeLineValueType

enum class TransactionTreeValueType(
    private val verbose: String,
    private val telemetryUnit: TelemetryUnit,
    private val telemetryScale: Int,
    val isAverage: Boolean,
) : TimeLineValueType {
    COUNT("Invocation counts", TelemetryUnit.PLAIN, 0, false),
    AVERAGE("Average times", TelemetryUnit.NANOSECONDS, 0, true),
    TOTAL("Total times", TelemetryUnit.NANOSECONDS, 0, false);

    override fun toString(): String = verbose

    override fun getTelemetryUnit(): TelemetryUnit = telemetryUnit

    override fun getTelemetryScale(): Int = telemetryScale
}
