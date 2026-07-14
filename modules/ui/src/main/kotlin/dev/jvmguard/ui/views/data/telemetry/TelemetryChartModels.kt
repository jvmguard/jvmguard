package dev.jvmguard.ui.views.data.telemetry

import dev.jvmguard.data.config.FrequencyUnit
import dev.jvmguard.data.vmdata.TelemetryData
import dev.jvmguard.data.vmdata.TelemetryInterval
import dev.jvmguard.data.vmdata.TelemetryNode
import dev.jvmguard.data.vmdata.TelemetryType
import dev.jvmguard.ui.components.echart.TelemetryChartModel
import dev.jvmguard.ui.components.echart.TelemetryPoint
import dev.jvmguard.ui.components.echart.TelemetrySeriesModel
import dev.jvmguard.ui.views.data.telemetry.TelemetryChartModels.MISSING_INTERVAL_THRESHOLD_FACTOR
import java.math.BigDecimal

/** A frozen or otherwise forced y-axis range; `null` bounds let the chart auto-scale. */
class YRange(val min: Double?, val max: Double?)

/**
 * Builds a [TelemetryChartModel] from a server [TelemetryData] tree.
 */
object TelemetryChartModels {

    private const val MISSING_INTERVAL_THRESHOLD_FACTOR = 5

    fun build(
        telemetryData: TelemetryData,
        node: TelemetryNode,
        frequencyUnit: FrequencyUnit,
        interval: TelemetryInterval,
        endTime: Long,
        isTransactions: Boolean,
        logarithmic: Boolean,
        frozen: YRange?,
        canBackward: Boolean = false,
        canForward: Boolean = false,
        animate: Boolean = false,
        markTime: Long? = null,
        markBandStart: Long? = null,
        markBandEnd: Long? = null,
    ): TelemetryChartModel {
        node.calculateUnitScale(frequencyUnit)
        val stacked = node.isStackedData
        val timestamps = telemetryData.timestamps ?: LongArray(0)
        var series = node.data.map { data ->
            TelemetrySeriesModel(
                name = data.description,
                colorKey = if (isTransactions) transactionColorKey(data.subId) else null,
                points = buildPoints(timestamps, data.unitScaledData, stacked),
            )
        }
        // Stacked series are drawn bottom-up in array order
        if (stacked) {
            series = series.reversed()
        }
        return TelemetryChartModel(
            series = series,
            xMin = endTime - interval.timeExtent,
            xMax = endTime,
            stacked = stacked,
            logarithmic = logarithmic,
            zeroBase = true,
            unitLabel = node.unitLabel,
            valueDecimals = if (node.unitLevels > 0) 2 else 0,
            bucketMillis = telemetryData.dataInterval?.millis ?: 0L,
            yMin = frozen?.min,
            yMax = frozen?.max,
            canBackward = canBackward,
            canForward = canForward,
            animate = animate,
            markTime = markTime,
            markBandStart = markBandStart,
            markBandEnd = markBandEnd,
        )
    }

    private fun transactionColorKey(subId: String?): String? = when (subId) {
        TelemetryType.SUB_ID_NORMAL -> TelemetrySeriesModel.COLOR_NORMAL
        TelemetryType.SUB_ID_SLOW -> TelemetrySeriesModel.COLOR_SLOW
        TelemetryType.SUB_ID_VERY_SLOW -> TelemetrySeriesModel.COLOR_VERY_SLOW
        TelemetryType.SUB_ID_ERROR -> TelemetrySeriesModel.COLOR_ERROR
        else -> null
    }

    private fun buildPoints(
        timestamps: LongArray, values: List<BigDecimal?>?, stacked: Boolean
    ): List<TelemetryPoint> {
        val points = ArrayList<TelemetryPoint>()
        if (values == null) {
            return points
        }
        val count = timestamps.size
        for (i in 0 until count) {
            val timestamp = timestamps[i]
            insertGapBreak(points, timestamps, i, count)
            val value = values.getOrNull(i)?.toDouble()
            if (!stacked || value != null) {
                if (stacked && i > 0 && values.getOrNull(i - 1) == null) {
                    points.add(TelemetryPoint(timestamp - 1, 0.0))
                }
                points.add(TelemetryPoint(timestamp, value))
                if (stacked && i < count - 1 && values.getOrNull(i + 1) == null) {
                    points.add(TelemetryPoint(timestamp + 1, 0.0))
                }
            }
        }
        return points
    }

    /** Drops the series to zero across a time gap larger than [MISSING_INTERVAL_THRESHOLD_FACTOR]x. */
    private fun insertGapBreak(points: MutableList<TelemetryPoint>, timestamps: LongArray, i: Int, count: Int) {
        if (i == 1 && i < count - 1) {
            val leftDelta = timestamps[i] - timestamps[0]
            val rightDelta = timestamps[i + 1] - timestamps[i]
            if (leftDelta > rightDelta * MISSING_INTERVAL_THRESHOLD_FACTOR) {
                points.add(TelemetryPoint(timestamps[0] + rightDelta, 0.0))
                points.add(TelemetryPoint(timestamps[i] - rightDelta, 0.0))
            }
        } else if (i > 1) {
            val preLastDelta = timestamps[i - 1] - timestamps[i - 2]
            val lastDelta = timestamps[i] - timestamps[i - 1]
            if (lastDelta > preLastDelta * MISSING_INTERVAL_THRESHOLD_FACTOR) {
                points.add(TelemetryPoint(timestamps[i - 1] + preLastDelta, 0.0))
                points.add(TelemetryPoint(timestamps[i] - preLastDelta, 0.0))
            }
        }
    }
}
