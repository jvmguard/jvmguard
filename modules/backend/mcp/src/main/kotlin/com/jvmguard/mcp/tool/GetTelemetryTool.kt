package com.jvmguard.mcp.tool

import com.jvmguard.data.vmdata.TelemetryInterval
import com.jvmguard.mcp.McpError
import com.jvmguard.mcp.McpJson
import com.jvmguard.mcp.McpToolContext
import com.jvmguard.common.export.TelemetryExport
import io.modelcontextprotocol.spec.McpSchema.Tool
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification

class GetTelemetryTool(ctx: McpToolContext) : McpTool(ctx) {

    companion object {
        const val NAME = "get_telemetry"
    }

    override fun createSpecification(): SyncToolSpecification {
        val tool = Tool.builder(
            NAME,
            objectSchema(
                mapOf(
                    "vm" to stringProperty("VM hierarchy path (from list_vms), or omit for all VMs."),
                    "telemetry" to stringProperty("Telemetry name (from list_telemetries), e.g. \"cpu\", \"heap\", \"gc\"."),
                    "interval" to stringProperty(
                        "Time range for the data.",
                        TelemetryInterval.entries.map { it.exportId },
                    ),
                    "startTime" to integerProperty(
                        "Start time as epoch milliseconds. Optional. If set, the window ends one interval later " +
                                "unless endTime is also given."
                    ),
                    "endTime" to integerProperty("End time as epoch milliseconds. Default: now."),
                ),
                listOf("telemetry"),
            ),
        ).description(
            "Retrieve a telemetry time series for a VM (or all VMs). " +
                    "Returns JSON with timestamp/value rows."
        ).annotations(readOnly("Get telemetry")).build()
        return SyncToolSpecification(tool) { _, request ->
            try {
                val args = request.arguments()
                val vmPath = args["vm"] as? String
                val telemetryName = args["telemetry"] as String
                val intervalId = (args["interval"] as? String) ?: "10min"
                val interval = TelemetryInterval.fromExportId(intervalId)
                    ?: throw McpError("Unknown interval: $intervalId")

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
                    if (rootNode != null && timestamps != null) {
                        val export = TelemetryExport(timestamps, rootNode)
                        jsonResult(McpJson.exportToJson(export))
                    } else {
                        textResult("[]")
                    }
                }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }
}
