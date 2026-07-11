package com.jvmguard.server

import com.jvmguard.common.AuditLog
import com.jvmguard.common.config.ConfigManager
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class ApiIpAllowlistFilter(private val configManager: ConfigManager) : OncePerRequestFilter() {

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val path = request.requestURI.removePrefix(request.contextPath)
        val source = when {
            path == "/mcp" || path.startsWith("/mcp/") -> "mcp"
            path == "/api" || path.startsWith("/api/") -> "rest"
            else -> null
        }
        if (source == null) {
            filterChain.doFilter(request, response)
            return
        }
        val allowlist = configManager.getGlobalConfig(false).guardrailConfig.apiAllowedIps
        if (IpAllowlist.isAllowed(request.remoteAddr, allowlist)) {
            filterChain.doFilter(request, response)
        } else {
            AuditLog.record(
                source, null, "network", AuditLog.Outcome.DENIED,
                target = path, detail = "address not in allowlist", clientIp = request.remoteAddr,
            )
            response.sendError(HttpServletResponse.SC_FORBIDDEN)
        }
    }
}
