package dev.jvmguard.mcp

import dev.jvmguard.common.AuditLog
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Streams a captured diagnostic artifact so that an MCP client can download it. There are two ways to authorize:
 * a single-use "dl" token provided by get_snapshot_file, or the API-key  bearer token.
 */
@RestController
class McpArtifactController(
    private val toolContext: McpToolContext,
    private val downloadTokens: McpDownloadTokens,
) {

    @GetMapping("/mcp-artifacts/{id}")
    fun download(
        @PathVariable id: Long,
        @RequestParam(name = "dl", required = false) dl: String?,
        request: HttpServletRequest,
    ): ResponseEntity<Resource> {
        val clientIp = request.remoteAddr
        val loginName = (if (dl != null) downloadTokens.consume(dl, id) else authenticatedLogin())
            ?: run {
                AuditLog.record(
                    "mcp", authenticatedLogin(), "download", AuditLog.Outcome.DENIED,
                    target = "artifact=$id", detail = "unauthorized", clientIp = clientIp,
                )
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
            }
        return toolContext.withConnectionForPrincipal(loginName) { conn ->
            val file = conn.getSnapshotFile(id)
            if (file == null || !file.file.isFile) {
                AuditLog.record(
                    "mcp", loginName, "download", AuditLog.Outcome.ERROR,
                    target = "artifact=$id", detail = "not found", clientIp = clientIp,
                )
                ResponseEntity.notFound().build()
            } else {
                AuditLog.record(
                    "mcp", loginName, "download", AuditLog.Outcome.OK,
                    target = "artifact=$id", detail = "type=${file.type.name} bytes=${file.file.length()}",
                    clientIp = clientIp,
                )
                val resource: Resource = FileSystemResource(file.file)
                ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${file.targetFileName}\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(file.file.length())
                    .body(resource)
            }
        }
    }

    // The bearer-authenticated login name, or null for an anonymous request
    private fun authenticatedLogin(): String? {
        val auth = SecurityContextHolder.getContext().authentication
        return if (auth != null && auth.isAuthenticated && auth !is AnonymousAuthenticationToken) auth.name else null
    }
}
