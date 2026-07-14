package dev.jvmguard.mcp.auth

import dev.jvmguard.common.JvmGuardProperties
import dev.jvmguard.data.user.UserManager
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter

@Configuration
class McpResourceServerConfig(
    private val properties: JvmGuardProperties,
    private val userManager: UserManager,
) {

    @Bean
    @Order(1)
    fun mcpResourceServerFilterChain(http: HttpSecurity): SecurityFilterChain {
        http.securityMatcher("/mcp/**", "/mcp-artifacts/**").csrf { it.disable() }
        if (!properties.isMcpEnabled) {
            return http
                .authorizeHttpRequests { it.anyRequest().denyAll() }
                .exceptionHandling {
                    it.authenticationEntryPoint { _, response, _ ->
                        response.sendError(HttpServletResponse.SC_NOT_FOUND)
                    }
                }
                .build()
        }
        val apiKeyFilter = ApiKeyBearerAuthenticationFilter(userManager)
        return http
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                // Single-use downloads
                it.requestMatchers("/mcp-artifacts/**").permitAll()
                it.anyRequest().authenticated()
            }
            // Must run before the anonymous filter
            .addFilterBefore(apiKeyFilter, AnonymousAuthenticationFilter::class.java)
            .exceptionHandling {
                it.authenticationEntryPoint { _, response, _ ->
                    response.setHeader("WWW-Authenticate", "Bearer")
                    response.status = HttpServletResponse.SC_UNAUTHORIZED
                }
            }
            .build()
    }
}
