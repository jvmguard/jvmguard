package dev.jvmguard.ui.views.data.telemetry

import dev.jvmguard.data.vmdata.SparkLineData
import dev.jvmguard.data.vmdata.TelemetryType
import dev.jvmguard.ui.components.sparkline.SparklineDisplay
import dev.jvmguard.ui.components.sparkline.SparklineState

class TelemetryOverviewRow(
    val telemetryType: TelemetryType,
    private val lastHour: SparkLineData?,
    private val lastDay: SparkLineData?,
) {

    val name: String get() = telemetryType.name

    fun lastDayState(): SparklineState = graphState(lastDay)

    fun lastHourState(): SparklineState = graphState(lastHour)

    fun currentState(): SparklineState =
        lastHour?.let { SparklineState.forVm(it, it.scaledMaxDisplayValue, SparklineDisplay.VALUE_ONLY) }
            ?: SparklineState.empty()

    private fun graphState(data: SparkLineData?): SparklineState =
        data?.let { SparklineState.forVm(it, it.scaledMaxDisplayValue, SparklineDisplay.GRAPH_ONLY) }
            ?: SparklineState.empty()
}
