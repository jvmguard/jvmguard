package dev.jvmguard.server

import dev.jvmguard.common.AuditLog
import dev.jvmguard.common.JvmGuardConfig
import dev.jvmguard.common.JvmGuardProperties
import dev.jvmguard.common.Loggers
import dev.jvmguard.connector.api.Server
import dev.jvmguard.connector.api.SsoLoginError
import dev.jvmguard.rest.restInterface.RestInterface
import dev.jvmguard.server.sso.JvmGuardOidcUserService
import dev.jvmguard.server.sso.SsoAuthenticationException
import dev.jvmguard.ui.server.JvmGuardPrincipal
import dev.jvmguard.ui.server.SecurityBridge
import dev.jvmguard.ui.views.login.LoginView
import com.vaadin.flow.component.UI
import com.vaadin.flow.spring.security.AuthenticationContext
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.security.web.context.SecurityContextRepository

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfiguration(private val properties: JvmGuardProperties) {

    @Bean
    @Order(0)
    fun restApiSecurityFilterChain(http: HttpSecurity, restInterface: RestInterface): SecurityFilterChain {
        http.securityMatcher("/api/**").csrf { it.disable() }
        if (!properties.isRestApiEnabled) {
            return http
                .authorizeHttpRequests { it.anyRequest().denyAll() }
                .exceptionHandling { handling ->
                    handling.authenticationEntryPoint { _, response, _ -> response.sendError(HttpServletResponse.SC_NOT_FOUND) }
                }
                .build()
        }
        val restAuthenticationManager = ProviderManager(RestApiKeyAuthenticationProvider(restInterface, properties))
        val basicEntryPoint = BasicAuthenticationEntryPoint().apply { setRealmName("jvmguard api") }
        val entryPoint = AuthenticationEntryPoint { request, response, authException ->
            if (request.getHeader("Authorization") != null) {
                AuditLog.record(
                    "rest", null, "auth", AuditLog.Outcome.AUTH_FAILED,
                    detail = "invalid credentials", clientIp = request.remoteAddr,
                )
            }
            basicEntryPoint.commence(request, response, authException)
        }
        return http
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { it.anyRequest().authenticated() }
            .httpBasic { it.authenticationEntryPoint(entryPoint) }
            .exceptionHandling { it.authenticationEntryPoint(entryPoint) }
            .authenticationManager(restAuthenticationManager)
            .build()
    }

    @Bean
    @Order(2)
    fun vaadinSecurityFilterChain(
        http: HttpSecurity,
        oidcUserService: JvmGuardOidcUserService,
        clientRegistrationRepository: ClientRegistrationRepository,
    ): SecurityFilterChain {
        http.authorizeHttpRequests { it.requestMatchers("/error", "/icons/**").permitAll() }
        if (JvmGuardConfig.isIntegrationTest || java.lang.Boolean.getBoolean("jvmguard.testControlFilter")) {
            http.authorizeHttpRequests { it.requestMatchers("/test", "/test/**").permitAll() }
        }
        return http.with(VaadinSecurityConfigurer.vaadin()) { configurer ->
            configurer.loginView(LoginView::class.java)
            configurer.logoutSuccessHandler(ssoLogoutSuccessHandler(clientRegistrationRepository))
            configurer.addLogoutHandler { _, _, authentication ->
                (authentication?.principal as? JvmGuardPrincipal)?.serverConnection?.logout()
            }
        }.oauth2Login { oauth2 ->
            oauth2.loginPage("/login")
            oauth2.userInfoEndpoint { it.oidcUserService(oidcUserService) }
            oauth2.successHandler(ssoSuccessHandler())
            oauth2.failureHandler(ssoFailureHandler(clientRegistrationRepository))
        }.build()
    }

    private fun ssoFailureHandler(
        clientRegistrationRepository: ClientRegistrationRepository,
    ): AuthenticationFailureHandler =
        AuthenticationFailureHandler { request, response, exception ->
            val detail = (exception as? OAuth2AuthenticationException)?.error?.let { err ->
                "${err.errorCode}${err.description?.let { d -> ": $d" } ?: ""}"
            } ?: exception.message
            Loggers.SERVER.warn("SSO login failed: {}", detail, exception)
            val error = generateSequence(exception as Throwable?) { it.cause }
                .firstNotNullOfOrNull { (it as? SsoAuthenticationException)?.error }
                ?: SsoLoginError.GENERIC

            val registrationId = request.requestURI.substringAfter("/login/oauth2/code/", "")
                .takeIf { it.isNotEmpty() }
            val endSessionEndpoint = registrationId
                ?.let { clientRegistrationRepository.findByRegistrationId(it) }
                ?.providerDetails
                ?.configurationMetadata
                ?.get("end_session_endpoint") as? String

            if (endSessionEndpoint != null) {
                val baseUrl = "${request.scheme}://${request.serverName}:${request.serverPort}${request.contextPath}"
                val postLogoutUri = java.net.URLEncoder.encode("$baseUrl/login", "UTF-8")
                val idTokenHint = generateSequence(exception as Throwable?) { it.cause }
                    .firstNotNullOfOrNull { (it as? SsoAuthenticationException)?.idTokenValue }
                val params = buildString {
                    append("post_logout_redirect_uri=$postLogoutUri")
                    idTokenHint?.let { append("&id_token_hint=").append(java.net.URLEncoder.encode(it, "UTF-8")) }
                }
                request.session.setAttribute("ssoError", error.code)
                response.sendRedirect("$endSessionEndpoint?$params")
            } else {
                response.sendRedirect("/login?ssoError=${error.code}")
            }
        }

    private fun ssoSuccessHandler(): AuthenticationSuccessHandler =
        AuthenticationSuccessHandler { request, response, authentication ->
            val context = SecurityContextHolder.createEmptyContext()
            context.authentication = authentication
            SecurityContextHolder.setContext(context)
            val repo: SecurityContextRepository = HttpSessionSecurityContextRepository()
            repo.saveContext(context, request, response)
            response.sendRedirect("/")
        }

    private fun ssoLogoutSuccessHandler(
        clientRegistrationRepository: ClientRegistrationRepository,
    ): LogoutSuccessHandler {
        val handler = OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository)
        handler.setPostLogoutRedirectUri("{baseUrl}/login")
        handler.setDefaultTargetUrl("/login")
        handler.setRedirectStrategy { _, _, url ->
            UI.getCurrent()?.page?.setLocation(url)
        }
        return handler
    }

    @Bean
    fun jvmguardAuthenticationProvider(server: Server): JvmGuardAuthenticationProvider = JvmGuardAuthenticationProvider(server)

    @Bean
    fun authenticationManager(jvmguardAuthenticationProvider: JvmGuardAuthenticationProvider): AuthenticationManager =
        ProviderManager(jvmguardAuthenticationProvider)

    @Bean
    fun securityBridgeInitializer(
        authenticationManager: AuthenticationManager,
        authenticationContext: AuthenticationContext,
    ): SmartInitializingSingleton = SmartInitializingSingleton {
        SecurityBridge.init(authenticationManager, authenticationContext)
    }
}
