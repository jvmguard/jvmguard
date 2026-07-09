package com.jvmguard.mcp.tool

import com.jvmguard.mcp.McpToolContext
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification

object McpToolRegistry {

    fun allTools(ctx: McpToolContext): List<SyncToolSpecification> =
        rawTools(ctx).map { spec ->
            val originalHandler = spec.callHandler
            SyncToolSpecification(spec.tool) { exchange, request ->
                // Propagate the auth token and base URL from the transport context to the handler thread
                val authHeader = exchange.transportContext().get("authorization") as? String ?: ""
                val baseUrl = exchange.transportContext().get("baseUrl") as? String ?: ""
                McpToolContext.authTokenHolder.set(authHeader)
                McpToolContext.baseUrlHolder.set(baseUrl)
                try {
                    originalHandler.apply(exchange, request)
                } finally {
                    McpToolContext.authTokenHolder.remove()
                    McpToolContext.baseUrlHolder.remove()
                }
            }
        }

    private fun rawTools(ctx: McpToolContext): List<SyncToolSpecification> = listOf(
        ListGroupsTool(ctx),
        ListVmsTool(ctx),
        ListTelemetriesTool(ctx),
        GetTelemetryTool(ctx),
        GetCallTreeTool(ctx),
        GetHotspotsTool(ctx),
        GetOverdueTransactionsTool(ctx),
        HeapDumpTool(ctx),
        ThreadDumpTool(ctx),
        RunGcTool(ctx),
        RecordJfrTool(ctx),
        RecordJpsTool(ctx),
        ListMbeansTool(ctx),
        GetMbeanDataTool(ctx),
        ListSnapshotFilesTool(ctx),
        GetSnapshotFileTool(ctx),
        ListLogFilesTool(ctx),
        GetLogFileTool(ctx),
        GetInboxTool(ctx),
    ).map { it.createSpecification() }
}
