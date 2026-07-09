package com.jvmguard.mcp

import com.jvmguard.mcp.tool.McpToolRegistry
import io.modelcontextprotocol.common.McpTransportContext
import io.modelcontextprotocol.server.McpServer
import io.modelcontextprotocol.server.McpSyncServer
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities
import jakarta.servlet.http.HttpServletRequest
import org.springframework.boot.web.servlet.ServletRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class McpConfiguration {

    @Bean
    fun mcpTransportProvider(): HttpServletStreamableServerTransportProvider =
        HttpServletStreamableServerTransportProvider.builder()
            .mcpEndpoint("/mcp")
            .contextExtractor { request ->
                val auth = request.getHeader("Authorization") ?: ""
                McpTransportContext.create(mapOf("authorization" to auth, "baseUrl" to baseUrl(request)))
            }
            .build()

    private fun baseUrl(request: HttpServletRequest): String {
        val port = request.serverPort
        val defaultPort = (request.scheme == "http" && port == 80) || (request.scheme == "https" && port == 443)
        val portPart = if (defaultPort) "" else ":$port"
        return "${request.scheme}://${request.serverName}$portPart${request.contextPath}"
    }

    @Bean
    fun mcpServletRegistration(
        provider: HttpServletStreamableServerTransportProvider,
    ): ServletRegistrationBean<HttpServletStreamableServerTransportProvider> =
        ServletRegistrationBean(provider, "/mcp/*")

    @Bean
    fun mcpServer(
        provider: HttpServletStreamableServerTransportProvider,
        toolContext: McpToolContext,
    ): McpSyncServer = McpServer.sync(provider)
        .serverInfo("jvmguard", "1.0")
        .instructions(McpInstructions.WORKFLOW_GUIDE)
        .capabilities(
            ServerCapabilities.builder()
                .tools(false)
                .build()
        )
        .tools(McpToolRegistry.allTools(toolContext))
        .build()
}
