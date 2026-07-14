package dev.jvmguard.ui.views.data.telemetry

import dev.jvmguard.agent.config.telemetry.TelemetryUnit
import dev.jvmguard.data.config.FrequencyUnit
import dev.jvmguard.data.vmdata.TelemetryData
import dev.jvmguard.data.vmdata.TelemetryInterval
import dev.jvmguard.data.vmdata.TelemetryNode
import dev.jvmguard.data.vmdata.TelemetryType
import dev.jvmguard.ui.components.echart.TelemetrySeriesModel
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TelemetryChartModelsTest {

    @Test
    fun lineChartMapsASingleSeriesWithUnitsAndWindow() {
        val node = TelemetryNode("CPU Load", false).apply {
            setTelemetryUnit(TelemetryUnit.PERCENT, 0)
            addData("CPU Load", "", longArrayOf(5, 12, 8))
        }
        val data = telemetryData(node, longArrayOf(1000, 2000, 3000))

        val model = build(data, node, endTime = 3000)

        assertFalse(model.stacked)
        assertEquals("%", model.unitLabel)
        assertEquals(3000 - TelemetryInterval.TEN_MINUTES.timeExtent, model.xMin)
        assertEquals(3000, model.xMax)
        assertEquals(1, model.series.size)
        val series = model.series.single()
        assertEquals("CPU Load", series.name)
        assertNull(series.colorKey)
        assertEquals(listOf(5.0, 12.0, 8.0), series.points.map { it.v })
    }

    @Test
    fun stackedSeriesAreReversedSoTheFirstSitsOnTop() {
        val node = TelemetryNode("Heap Usage", true).apply {
            setTelemetryUnit(TelemetryUnit.BYTES, 0)
            addData("Used", TelemetryType.SUB_ID_USED_HEAP, longArrayOf(100, 100))
            addData("Free", TelemetryType.SUB_ID_FREE_HEAP, longArrayOf(50, 50))
        }
        val data = telemetryData(node, longArrayOf(1000, 2000))

        val model = build(data, node, endTime = 2000)

        assertTrue(model.stacked)
        assertEquals(listOf("Free", "Used"), model.series.map { it.name })
    }

    @Test
    fun transactionSubIdsMapToStatusColors() {
        val node = TelemetryNode("Transactions", false).apply {
            setTelemetryUnit(TelemetryUnit.PER_SECOND, 0)
            addData("Normal", TelemetryType.SUB_ID_NORMAL, longArrayOf(1, 1))
            addData("Error", TelemetryType.SUB_ID_ERROR, longArrayOf(2, 2))
        }
        val data = telemetryData(node, longArrayOf(1000, 2000))

        val model = build(data, node, endTime = 2000, isTransactions = true)

        val colors = model.series.associate { it.name to it.colorKey }
        assertEquals(TelemetrySeriesModel.COLOR_NORMAL, colors["Normal"])
        assertEquals(TelemetrySeriesModel.COLOR_ERROR, colors["Error"])
    }

    @Test
    fun largeTimeGapsBreakTheSeriesToZero() {
        val node = TelemetryNode("CPU Load", false).apply {
            setTelemetryUnit(TelemetryUnit.PERCENT, 0)
            addData("CPU Load", "", longArrayOf(5, 6, 7, 8, 9))
        }
        // A 98s gap dwarfs the 1s sampling interval, inserting two synthetic zero points.
        val data = telemetryData(node, longArrayOf(0, 1000, 2000, 100_000, 101_000))

        val model = build(data, node, endTime = 101_000)

        val points = model.series.single().points
        assertEquals(7, points.size)
        assertTrue(points.any { it.v == 0.0 }, "expected a synthetic zero break")
    }

    private fun telemetryData(node: TelemetryNode, timestamps: LongArray): TelemetryData =
        TelemetryData().apply {
            rootNode = node
            this.timestamps = timestamps
        }

    private fun build(
        data: TelemetryData,
        node: TelemetryNode,
        endTime: Long,
        isTransactions: Boolean = false,
    ) = TelemetryChartModels.build(
        telemetryData = data,
        node = node,
        frequencyUnit = FrequencyUnit.PER_MINUTE,
        interval = TelemetryInterval.TEN_MINUTES,
        endTime = endTime,
        isTransactions = isTransactions,
        logarithmic = false,
        frozen = null,
    )
}
