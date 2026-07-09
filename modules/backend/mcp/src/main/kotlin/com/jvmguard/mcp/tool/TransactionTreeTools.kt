package com.jvmguard.mcp.tool

import com.jvmguard.data.transactions.TransactionDataType
import com.jvmguard.data.transactions.TransactionTreeInterval
import com.jvmguard.mcp.McpError
import com.jvmguard.mcp.McpJson
import com.jvmguard.mcp.McpToolContext
import com.jvmguard.common.export.TransactionTreeExport
import io.modelcontextprotocol.spec.McpSchema.Tool
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification

abstract class AbstractTransactionTreeTool(
    ctx: McpToolContext,
    private val toolName: String,
    private val toolTitle: String,
    private val toolDescription: String,
) : McpTool(ctx) {

    protected fun buildSpec(
        dataType: TransactionDataType,
        exportType: TransactionTreeExport.DataType,
    ): SyncToolSpecification {
        val tool = Tool.builder(
            toolName,
            objectSchema(
                mapOf(
                    "vm" to stringProperty("VM hierarchy path (from list_vms), or omit for all VMs."),
                    "interval" to stringProperty(
                        "Time range for the data.",
                        TransactionTreeInterval.entries.map { it.exportId },
                    ),
                    "mergePolicies" to booleanProperty("If true, merge call points with identical policies. Default: false."),
                ),
            ),
        ).description(toolDescription).annotations(readOnly(toolTitle)).build()
        return SyncToolSpecification(tool) { _, request ->
            try {
                val args = request.arguments()
                val vmPath = args["vm"] as? String
                val intervalId = (args["interval"] as? String) ?: "10min"
                val mergePolicies = (args["mergePolicies"] as? Boolean) ?: false

                val interval = TransactionTreeInterval.fromExportId(intervalId)
                    ?: throw McpError("Unknown interval: $intervalId")

                ctx.withConnection { conn ->
                    val vm = VmResolver.resolveVmOrNull(conn, vmPath)
                    val cursor = conn.getCurrentTransactionTreeCursor(vm, interval, dataType)
                    val treeData = if (dataType == TransactionDataType.OVERDUE) {
                        conn.getHotspots(cursor, mergePolicies)
                    } else {
                        @Suppress("USELESS_CAST")
                        if (exportType == TransactionTreeExport.DataType.HOT_SPOTS) {
                            conn.getHotspots(cursor, mergePolicies)
                        } else {
                            conn.getCallTree(cursor, mergePolicies)
                        }
                    }
                    val export = TransactionTreeExport(exportType, treeData.transactionTree)
                    jsonResult(McpJson.exportToJson(export))
                }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }
}

class GetCallTreeTool(ctx: McpToolContext) : AbstractTransactionTreeTool(
    ctx,
    "get_call_tree",
    "Get call tree",
    "Retrieve the call tree (transaction tree) for a VM or all VMs. " +
            "Returns nested JSON with method names, execution time, invocation count, and type.",
) {
    override fun createSpecification() = buildSpec(
        TransactionDataType.TRANSACTION,
        TransactionTreeExport.DataType.CALL_TREE,
    )
}

class GetHotspotsTool(ctx: McpToolContext) : AbstractTransactionTreeTool(
    ctx,
    "get_hotspots",
    "Get hot spots",
    "Retrieve the hotspots (aggregated backtraces) for a VM or all VMs. " +
            "Hotspots show where the most time is spent, aggregated across all call paths.",
) {
    override fun createSpecification() = buildSpec(
        TransactionDataType.TRANSACTION,
        TransactionTreeExport.DataType.HOT_SPOTS,
    )
}

class GetOverdueTransactionsTool(ctx: McpToolContext) : AbstractTransactionTreeTool(
    ctx,
    "get_overdue_transactions",
    "Get overdue transactions",
    "Retrieve overdue transactions for a VM or all VMs. " +
            "These are transactions that exceeded their configured time thresholds.",
) {
    override fun createSpecification() = buildSpec(
        TransactionDataType.OVERDUE,
        TransactionTreeExport.DataType.OVERDUE,
    )
}
