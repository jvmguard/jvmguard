package dev.jvmguard.mcp.tool

import dev.jvmguard.data.vmdata.TelemetryNode
import java.math.BigDecimal
import java.math.RoundingMode

object TelemetrySeries {

    class Series(
        val timestamps: List<Long>,
        val values: List<BigDecimal?>,
        val rawCount: Int,
        val downsampled: Boolean,
        val stats: Map<String, Any>?,
    )

    fun collectDataLines(node: TelemetryNode): List<TelemetryNode.Data> {
        val out = ArrayList<TelemetryNode.Data>()
        fun visit(n: TelemetryNode) {
            out.addAll(n.data)
            n.children.forEach(::visit)
        }
        visit(node)
        return out
    }

    fun selectLine(node: TelemetryNode, subId: String): TelemetryNode.Data? {
        val lines = collectDataLines(node)
        return lines.firstOrNull { it.subId == subId } ?: lines.singleOrNull()
    }

    fun build(timestamps: LongArray, line: TelemetryNode.Data, maxPoints: Int): Series {
        val scaledValues = line.plainScaledData
        val pairs = ArrayList<Pair<Long, BigDecimal?>>(timestamps.size)
        for (i in timestamps.indices) {
            pairs.add(timestamps[i] to scaledValues?.getOrNull(i))
        }
        // Trailing nulls are just padding to "now" and carry no measurement
        while (pairs.isNotEmpty() && pairs.last().second == null) {
            pairs.removeAt(pairs.size - 1)
        }

        val rawCount = pairs.size
        val stats = summaryStats(pairs.mapNotNull { it.second })

        val shaped = if (maxPoints > 0 && pairs.size > maxPoints) downsample(pairs, maxPoints) else pairs
        return Series(
            timestamps = shaped.map { it.first },
            values = shaped.map { it.second },
            rawCount = rawCount,
            downsampled = shaped.size < rawCount,
            stats = stats,
        )
    }

    private fun downsample(pairs: List<Pair<Long, BigDecimal?>>, maxPoints: Int): List<Pair<Long, BigDecimal?>> {
        val bucketSize = (pairs.size + maxPoints - 1) / maxPoints
        val result = ArrayList<Pair<Long, BigDecimal?>>()
        var index = 0
        while (index < pairs.size) {
            val bucket = pairs.subList(index, minOf(index + bucketSize, pairs.size))
            val present = bucket.mapNotNull { it.second }
            result.add(bucket.first().first to if (present.isEmpty()) null else average(present))
            index += bucketSize
        }
        return result
    }

    private fun average(values: List<BigDecimal>): BigDecimal {
        val scale = values.first().scale()
        return values.reduce(BigDecimal::add).divide(BigDecimal(values.size), scale, RoundingMode.HALF_UP)
    }

    private fun summaryStats(values: List<BigDecimal>): Map<String, Any>? {
        if (values.isEmpty()) {
            return null
        }
        return mapOf(
            "min" to values.min(),
            "max" to values.max(),
            "avg" to average(values),
            "last" to values.last(),
            "count" to values.size,
        )
    }
}
