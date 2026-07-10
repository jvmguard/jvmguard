package com.jvmguard.mcp.tool

import com.jvmguard.data.vmdata.TelemetryInterval
import com.jvmguard.data.vmdata.TelemetryType
import com.jvmguard.mcp.McpError
import com.jvmguard.mcp.McpJson
import com.jvmguard.mcp.McpToolContext
import io.modelcontextprotocol.spec.McpSchema.Tool
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification

class GetTelemetryTool(ctx: McpToolContext) : McpTool(ctx) {

    companion object {
        const val NAME = "get_telemetry"
        private const val DEFAULT_MAX_POINTS = 250
    }

    override fun createSpecification(): SyncToolSpecification {
        val tool = Tool.builder(
            NAME,
            objectSchema(
                mapOf(
                    "vm" to stringProperty("VM hierarchy path (from list_vms), or omit for all VMs."),
                    "telemetry" to stringProperty("Telemetry name (from list_telemetries), e.g. \"cpu\", \"hpu\", \"trt\"."),
                    "interval" to stringProperty(
                        "Time range for the data.",
                        TelemetryInterval.entries.map { it.exportId },
                    ),
                    "startTime" to integerProperty(
                        "Start time as epoch milliseconds. Optional. If set, the window ends one interval later " +
                                "unless endTime is also given."
                    ),
                    "endTime" to integerProperty("End time as epoch milliseconds. Default: now."),
                    "maxPoints" to integerProperty(
                        "Cap the returned series at this many points, bucket-averaging longer windows. " +
                                "Default: $DEFAULT_MAX_POINTS. Use 0 to disable downsampling."
                    ),
                ),
                listOf("telemetry"),
            ),
        ).description(
            "Retrieve one telemetry time series for a VM (or all VMs) in a compact columnar shape: " +
                    "{ id, description, unit, interval, count, downsampled, t: [epochMillis...], v: [values...], " +
                    "stats: { min, max, avg, last, count } }. 't' and 'v' are parallel arrays; trailing empty " +
                    "padding is dropped and long windows are bucket-averaged down to maxPoints."
        ).annotations(readOnly("Get telemetry")).build()
        return SyncToolSpecification(tool) { _, request ->
            try {
                val args = request.arguments()
                val vmPath = args["vm"] as? String
                val telemetryName = args["telemetry"] as String
                val intervalId = (args["interval"] as? String) ?: "10min"
                val interval = TelemetryInterval.fromExportId(intervalId)
                    ?: throw McpError("Unknown interval: $intervalId")
                val maxPoints = (args["maxPoints"] as? Number)?.toInt() ?: DEFAULT_MAX_POINTS

                // Epoch milliseconds exceed Int range
                val startTime = (args["startTime"] as? Number)?.toLong()
                val endTime = (args["endTime"] as? Number)?.toLong()
                    ?: startTime?.let { it + interval.timeExtent }
                    ?: System.currentTimeMillis()

                ctx.withConnection { conn ->
                    val vm = VmResolver.resolveVmOrNull(conn, vmPath)
                    val telemetryType = conn.idToTelemetryType[telemetryName]
                        ?: throw McpError("Unknown telemetry: $telemetryName")

                    val data = conn.getTelemetryData(
                        vm,
                        telemetryType.telemetryIdentifier.mainId,
                        interval,
                        endTime,
                    )

                    val rootNode = data.rootNode
                    val timestamps = data.timestamps
                    val line = rootNode?.let { TelemetrySeries.selectLine(it, telemetryType.searchSubIdForTelemetry) }

                    if (rootNode == null || timestamps == null || line == null) {
                        jsonResult(McpJson.write(emptySeries(telemetryName, telemetryType, intervalId)))
                    } else {
                        val series = TelemetrySeries.build(timestamps, line, maxPoints)
                        val result = buildMap<String, Any?> {
                            put("id", telemetryName)
                            put("description", telemetryType.name)
                            put("unit", telemetryType.unit.name)
                            put("interval", intervalId)
                            put("count", series.timestamps.size)
                            if (series.downsampled) {
                                put("downsampled", true)
                                put("rawCount", series.rawCount)
                            }
                            series.stats?.let { put("stats", it) }
                            put("t", series.timestamps)
                            put("v", series.values)
                        }
                        jsonResult(McpJson.write(result))
                    }
                }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    private fun emptySeries(
        telemetryName: String,
        telemetryType: TelemetryType,
        intervalId: String,
    ): Map<String, Any?> = mapOf(
        "id" to telemetryName,
        "description" to telemetryType.name,
        "unit" to telemetryType.unit.name,
        "interval" to intervalId,
        "count" to 0,
        "t" to emptyList<Long>(),
        "v" to emptyList<Any?>(),
    )
}
