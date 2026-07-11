package com.jvmguard.mcp.tool

import com.jvmguard.common.AuditLog
import com.jvmguard.mcp.GuardrailException
import com.jvmguard.mcp.McpToolContext
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest
import io.modelcontextprotocol.spec.McpSchema.CallToolResult
import io.modelcontextprotocol.spec.McpSchema.TextContent
import org.springframework.security.access.AccessDeniedException

object McpToolRegistry {

    fun allTools(ctx: McpToolContext): List<SyncToolSpecification> =
        rawTools(ctx).map { spec ->
            val originalHandler = spec.callHandler
            val toolName = spec.tool.name
            val audited = spec.tool.annotations?.readOnlyHint() != true
            SyncToolSpecification(spec.tool) { exchange, request ->
                // Propagate the auth token, base URL and client IP from the transport context to the handler thread
                val authHeader = exchange.transportContext().get("authorization") as? String ?: ""
                val baseUrl = exchange.transportContext().get("baseUrl") as? String ?: ""
                val clientIp = exchange.transportContext().get("clientIp") as? String ?: ""
                McpToolContext.authTokenHolder.set(authHeader)
                McpToolContext.baseUrlHolder.set(baseUrl)
                McpToolContext.clientIpHolder.set(clientIp)
                try {
                    if (audited && ctx.globalGuardrails().mcpReadOnly) {
                        throw GuardrailException("MCP is in read-only mode; the '$toolName' tool is disabled by an administrator.")
                    }
                    val result = originalHandler.apply(exchange, request)
                    if (audited) {
                        auditResult(ctx, toolName, request, result)
                    }
                    result
                } catch (e: Exception) {
                    val outcome = classify(e)
                    if (audited || outcome == AuditLog.Outcome.DENIED) {
                        AuditLog.record(
                            "mcp", ctx.currentPrincipal(), toolName, outcome,
                            target = request.arguments()["vm"] as? String, detail = e.message,
                            clientIp = ctx.currentClientIp(),
                        )
                    }
                    toErrorResult(e)
                } finally {
                    McpToolContext.authTokenHolder.remove()
                    McpToolContext.baseUrlHolder.remove()
                    McpToolContext.clientIpHolder.remove()
                }
            }
        }

    private fun auditResult(ctx: McpToolContext, toolName: String, request: CallToolRequest, result: CallToolResult) {
        val target = request.arguments()["vm"] as? String
        val principal = ctx.currentPrincipal()
        val clientIp = ctx.currentClientIp()
        if (result.isError() != true) {
            AuditLog.record("mcp", principal, toolName, AuditLog.Outcome.OK, target = target, clientIp = clientIp)
        } else {
            val text = result.content().filterIsInstance<TextContent>().joinToString(" ") { it.text() }
            AuditLog.record("mcp", principal, toolName, AuditLog.Outcome.ERROR, target = target, detail = text, clientIp = clientIp)
        }
    }

    private fun classify(e: Throwable): AuditLog.Outcome =
        if (isDenial(e)) AuditLog.Outcome.DENIED else AuditLog.Outcome.ERROR

    private fun isDenial(throwable: Throwable?): Boolean {
        var cause = throwable
        var depth = 0
        while (cause != null && depth < 5) {
            if (cause is AccessDeniedException || cause is SecurityException || cause is GuardrailException) {
                return true
            }
            cause = cause.cause
            depth++
        }
        return false
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
        RecordJfrTool(ctx),
        RecordJpsTool(ctx),
        ListMbeansTool(ctx),
        GetMbeanDataTool(ctx),
        SetMbeanAttributeTool(ctx),
        InvokeMbeanOperationTool(ctx),
        GetGroupConfigTool(ctx),
        SetGroupConfigTool(ctx),
        ListSnapshotFilesTool(ctx),
        GetSnapshotFileTool(ctx),
        ListLogFilesTool(ctx),
        GetLogFileTool(ctx),
        GetInboxTool(ctx),
    ).map { it.createSpecification() }
}
