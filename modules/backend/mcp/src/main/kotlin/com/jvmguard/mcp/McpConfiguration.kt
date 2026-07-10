package com.jvmguard.mcp

import com.jvmguard.mcp.tool.McpToolRegistry
import io.modelcontextprotocol.common.McpTransportContext
import io.modelcontextprotocol.server.McpServer
import io.modelcontextprotocol.server.McpSyncServer
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.boot.web.servlet.ServletRegistrationBean
import org.springframework.context.SmartLifecycle
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
class McpConfiguration {

    @Bean
    fun mcpTransportProvider(): HttpServletStreamableServerTransportProvider =
        HttpServletStreamableServerTransportProvider.builder()
            .mcpEndpoint("/mcp")
            .contextExtractor { request ->
                val auth = request.getHeader("Authorization") ?: ""
                McpTransportContext.create(
                    mapOf("authorization" to auth, "baseUrl" to baseUrl(request), "clientIp" to (request.remoteAddr ?: "")),
                )
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

    // Closes the MCP streams before the graceful shutdown Tomcat closes active requests.
    @Bean
    fun mcpTransportShutdown(provider: HttpServletStreamableServerTransportProvider): SmartLifecycle =
        object : SmartLifecycle {
            @Volatile
            private var running = true

            override fun start() {
                running = true
            }

            override fun stop() {
                running = false
                try {
                    provider.closeGracefully().block(Duration.ofSeconds(5))
                } catch (e: Exception) {
                    LoggerFactory.getLogger(McpConfiguration::class.java)
                        .warn("Failed to close MCP transport gracefully", e)
                }
            }

            override fun isRunning(): Boolean = running

            // Higher than webServerGracefulShutdown
            override fun getPhase(): Int = Integer.MAX_VALUE
        }
}
