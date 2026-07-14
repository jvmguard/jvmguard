package dev.jvmguard.mcp.tool

import dev.jvmguard.data.vmdata.TelemetryNode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class TelemetrySeriesTest {

    private val nullValue = Long.MIN_VALUE

    @Test
    fun selectsLineFromChildNode() {
        val root = TelemetryNode()
        val heap = TelemetryNode("Heap", true)
        heap.addData("Used", "u", longArrayOf(100, 200))
        heap.addData("Free", "f", longArrayOf(900, 800))
        root.children.add(heap)

        val used = TelemetrySeries.selectLine(root, "u")

        assertEquals("Used", used?.description)
    }

    @Test
    fun dropsTrailingNullPadding() {
        val root = singleLineTree("u", longArrayOf(100, 200, nullValue, nullValue))

        val series = TelemetrySeries.build(longArrayOf(1000, 2000, 3000, 4000), TelemetrySeries.selectLine(root, "u")!!, 250)

        assertEquals(listOf(1000L, 2000L), series.timestamps)
        assertEquals(listOf(BigDecimal("100"), BigDecimal("200")), series.values)
        assertEquals(false, series.downsampled)
    }

    @Test
    fun keepsInteriorNulls() {
        val root = singleLineTree("u", longArrayOf(100, nullValue, 300))

        val series = TelemetrySeries.build(longArrayOf(1000, 2000, 3000), TelemetrySeries.selectLine(root, "u")!!, 250)

        assertEquals(3, series.timestamps.size)
        assertNull(series.values[1])
    }

    @Test
    fun computesSummaryStats() {
        val root = singleLineTree("u", longArrayOf(100, 300, 200))

        val stats = TelemetrySeries.build(longArrayOf(1, 2, 3), TelemetrySeries.selectLine(root, "u")!!, 250).stats!!

        assertEquals(BigDecimal("100"), stats["min"])
        assertEquals(BigDecimal("300"), stats["max"])
        assertEquals(BigDecimal("200"), stats["avg"])
        assertEquals(BigDecimal("200"), stats["last"])
        assertEquals(3, stats["count"])
    }

    @Test
    fun downsamplesLongWindowToBucketAverages() {
        val values = LongArray(10) { (it + 1) * 10L } // 10, 20, ... 100
        val timestamps = LongArray(10) { it * 1000L }
        val root = singleLineTree("u", values)

        val series = TelemetrySeries.build(timestamps, TelemetrySeries.selectLine(root, "u")!!, 5)

        assertTrue(series.downsampled)
        assertEquals(10, series.rawCount)
        assertEquals(5, series.timestamps.size)
        assertEquals(0L, series.timestamps.first())
        assertEquals(BigDecimal("15"), series.values.first())
    }

    private fun singleLineTree(subId: String, values: LongArray): TelemetryNode {
        val root = TelemetryNode()
        root.addData("line", subId, values)
        return root
    }
}
