package dev.jvmguard.ui.views.data.telemetry

import dev.jvmguard.data.vmdata.TelemetryData
import dev.jvmguard.data.vmdata.TelemetryInterval
import dev.jvmguard.data.vmdata.TelemetryNode
import tools.jackson.databind.json.JsonMapper

object TelemetryExport {

    private val mapper = JsonMapper.builder().build()

    // node must already have had calculateUnitScale applied
    fun toJson(data: TelemetryData, node: TelemetryNode, interval: TelemetryInterval, endTime: Long): ByteArray {
        val timestamps = data.timestamps ?: LongArray(0)
        val scaled = node.data.associateWith { it.unitScaledData }
        val points = timestamps.indices.map { i ->
            LinkedHashMap<String, Any?>().apply {
                put("time", timestamps[i])
                node.data.forEach { series -> scaled[series]?.getOrNull(i)?.let { put(series.description, it) } }
            }
        }
        val root = linkedMapOf<String, Any?>(
            "telemetry" to node.description,
            "unit" to node.unitLabel,
            "interval" to interval.toString(),
            "endTime" to endTime,
            "points" to points,
        )
        return mapper.writeValueAsBytes(root)
    }
}
