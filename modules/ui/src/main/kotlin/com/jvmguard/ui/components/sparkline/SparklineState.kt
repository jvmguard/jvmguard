package com.jvmguard.ui.components.sparkline

import com.jvmguard.agent.config.telemetry.TelemetryUnit
import com.jvmguard.data.vmdata.SparkLineData
import java.math.BigDecimal
import kotlin.math.roundToLong

enum class SparklineDisplay {
    /** Graph + current value + min/max labels */
    FULL,
    GRAPH_ONLY,
    VALUE_ONLY,
}

/**
 * JavaBean serialized to the `jvmguard-sparkline` Lit element via `Element.setPropertyBean`. The Kotlin
 * property getters map to the same JSON keys the TS component reads (`data`, `displayRangeMax`,
 * `valueDotVisible`, …).
 */
@Suppress("unused")
class SparklineState private constructor() {

    var data: DoubleArray = DoubleArray(0)
        private set
    val graphWidth = 100
    val graphHeight = 20
    val displayRangeMin = 0.0
    var displayRangeMax = 1.0
        private set
    val pathWidth = 1
    val valueDotVisible = true
    val minMaxDotsVisible = false
    val minMaxLabelsVisible = true
    val valueLabelVisible = true
    var explicitMinLabelValue: String? = null
        private set
    var explicitMaxLabelValue: String? = null
        private set
    var explicitCurrentLabelValue: String? = null
        private set
    var unitLabel = ""
        private set
    private var display = SparklineDisplay.FULL
    val currentValueOnly: Boolean get() = display == SparklineDisplay.VALUE_ONLY
    val hideCurrentValue: Boolean get() = display == SparklineDisplay.GRAPH_ONLY

    companion object {

        fun forVm(
            sparkLineData: SparkLineData,
            graphMax: Double,
            display: SparklineDisplay = SparklineDisplay.FULL,
        ): SparklineState {
            val unit = sparkLineData.telemetryType.unit
            val scaled = sparkLineData.scaledData
            return SparklineState().apply {
                data = DoubleArray(scaled.size) { scaled[it].toDouble() }
                displayRangeMax = if (graphMax == 0.0) 1.0 else graphMax
                explicitMaxLabelValue = format(sparkLineData.scaledMax, unit)
                explicitMinLabelValue = format(sparkLineData.scaledMin, unit)
                explicitCurrentLabelValue = format(sparkLineData.scaledCurrent, unit)
                unitLabel = sparkLineData.label ?: ""
                this.display = display
            }
        }

        fun empty(): SparklineState = SparklineState()

        internal fun format(value: Number, unit: TelemetryUnit): String {
            var label = if (value is BigDecimal) value.toPlainString() else value.toString()
            val displayDigits = unit.displayDigits
            if (label.length > displayDigits) {
                val rounded = value.toDouble().roundToLong().toString()
                if (rounded.length <= displayDigits) {
                    return rounded
                }
                if (displayDigits >= 4 && unit.labels.size == 1) {
                    var scaled = value.toLong() / 1000
                    if (scaled < 1000) return "${scaled}K"
                    scaled /= 1000
                    if (scaled < 1000) return "${scaled}M"
                    scaled /= 1000
                    if (scaled < 1000) return "${scaled}G"
                }
                if (displayDigits >= 3) {
                    label = String.format("%1.0E", value.toDouble()).replace("E+0", "E")
                }
            }
            return label
        }
    }
}
