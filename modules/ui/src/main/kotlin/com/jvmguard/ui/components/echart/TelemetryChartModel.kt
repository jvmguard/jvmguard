package com.jvmguard.ui.components.echart

/** One point of a telemetry series: epoch millis [t] and a scaled value [v] (`null` is a gap). */
class TelemetryPoint(val t: Long, val v: Double?)

/**
 * A single telemetry series. [colorKey] picks a fixed semantic color (transaction status). `null`
 * means the chart assigns a palette color.
 */
class TelemetrySeriesModel(
    val name: String,
    val colorKey: String?,
    val points: List<TelemetryPoint>,
) {
    companion object {
        const val COLOR_NORMAL = "normal"
        const val COLOR_SLOW = "slow"
        const val COLOR_VERY_SLOW = "verySlow"
        const val COLOR_ERROR = "error"
    }
}

/**
 * The domain description of a telemetry chart, pushed to `jvmguard-echart.ts`, which builds the
 * Apache ECharts option from it.
 */
@Suppress("unused")
class TelemetryChartModel(
    val series: List<TelemetrySeriesModel>,
    val xMin: Long,
    val xMax: Long,
    val stacked: Boolean,
    val logarithmic: Boolean,
    val zeroBase: Boolean,
    val unitLabel: String,
    val valueDecimals: Int,
    val bucketMillis: Long,
    val yMin: Double?,
    val yMax: Double?,
    /** Whether the in-chart edge overlays for paging earlier / later are shown. */
    val canBackward: Boolean = false,
    val canForward: Boolean = false,
    /** Animate the y-axis rescale for this update — only set when paging backward/forward. */
    val animate: Boolean = false,
    /** Epoch millis of a point to highlight drawn as a large signal-colored marker. */
    val markTime: Long? = null,
    /** Epoch-millis range to highlight as a band */
    val markBandStart: Long? = null,
    val markBandEnd: Long? = null,
) {
    companion object {
        val EMPTY = TelemetryChartModel(
            series = emptyList(),
            xMin = 0,
            xMax = 0,
            stacked = false,
            logarithmic = false,
            zeroBase = true,
            unitLabel = "",
            valueDecimals = 0,
            bucketMillis = 0,
            yMin = null,
            yMax = null,
        )
    }
}
