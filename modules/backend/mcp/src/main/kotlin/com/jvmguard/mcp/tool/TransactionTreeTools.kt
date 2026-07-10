package com.jvmguard.mcp.tool

import com.jvmguard.data.transactions.TransactionDataType
import com.jvmguard.data.transactions.TransactionTreeInterval
import com.jvmguard.mcp.McpError
import com.jvmguard.mcp.McpJson
import com.jvmguard.mcp.McpToolContext
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
        rootKey: String,
        hotSpots: Boolean,
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
            val args = request.arguments()
            val vmPath = args["vm"] as? String
            val intervalId = (args["interval"] as? String) ?: "10min"
            val mergePolicies = (args["mergePolicies"] as? Boolean) ?: false

            val interval = TransactionTreeInterval.fromExportId(intervalId)
                ?: throw McpError("Unknown interval: $intervalId")

            ctx.withConnection { conn ->
                val vm = VmResolver.resolveVmOrNull(conn, vmPath)
                val cursor = conn.getCurrentTransactionTreeCursor(vm, interval, dataType)
                val treeData = if (hotSpots) {
                    conn.getHotspots(cursor, mergePolicies)
                } else {
                    conn.getCallTree(cursor, mergePolicies)
                }
                jsonResult(McpJson.write(McpTransactionTree.toResult(rootKey, treeData.transactionTree)))
            }
        }
    }
}

private const val TIME_FIELDS_DOC =
    "Each node has count, totalMicros (cumulative), selfMicros (excluding children), and avgMicros " +
            "(per invocation), all in microseconds; children are sorted heaviest-first. The call-point 'type' " +
            "is reported once at the top level when uniform, or per node when the tree mixes types."

class GetCallTreeTool(ctx: McpToolContext) : AbstractTransactionTreeTool(
    ctx,
    "get_call_tree",
    "Get call tree",
    "Retrieve the call tree (transaction tree) for a VM or all VMs, as a nested 'callTree' array. " +
            TIME_FIELDS_DOC,
) {
    override fun createSpecification() = buildSpec(
        TransactionDataType.TRANSACTION,
        rootKey = "callTree",
        hotSpots = false,
    )
}

class GetHotspotsTool(ctx: McpToolContext) : AbstractTransactionTreeTool(
    ctx,
    "get_hotspots",
    "Get hot spots",
    "Retrieve the hotspots (aggregated backtraces) for a VM or all VMs, as a 'hotSpots' array showing " +
            "where the most time is spent across all call paths. " + TIME_FIELDS_DOC,
) {
    override fun createSpecification() = buildSpec(
        TransactionDataType.TRANSACTION,
        rootKey = "hotSpots",
        hotSpots = true,
    )
}

class GetOverdueTransactionsTool(ctx: McpToolContext) : AbstractTransactionTreeTool(
    ctx,
    "get_overdue_transactions",
    "Get overdue transactions",
    "Retrieve transactions that exceeded their configured time thresholds, for a VM or all VMs, as an " +
            "'overdue' array. An empty array means none were overdue in the interval (healthy). " + TIME_FIELDS_DOC,
) {
    override fun createSpecification() = buildSpec(
        TransactionDataType.OVERDUE,
        rootKey = "overdue",
        hotSpots = true,
    )
}
