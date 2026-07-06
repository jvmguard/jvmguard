package com.jvmguard.server

import com.jvmguard.common.JvmGuardConfig
import com.jvmguard.common.JvmGuardProperties
import com.jvmguard.connector.api.Server
import com.jvmguard.rest.restInterface.RestInterface
import com.jvmguard.server.sso.JvmGuardOidcUserService
import com.jvmguard.ui.server.JvmGuardPrincipal
import com.jvmguard.ui.server.SecurityBridge
import com.jvmguard.ui.views.login.LoginView
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
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
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
        val entryPoint = BasicAuthenticationEntryPoint().apply { setRealmName("jvmguard api") }
        return http
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { it.anyRequest().authenticated() }
            .httpBasic { it.authenticationEntryPoint(entryPoint) }
            .exceptionHandling { it.authenticationEntryPoint(entryPoint) }
            .authenticationManager(restAuthenticationManager)
            .build()
    }

    @Bean
    @Order(1)
    fun vaadinSecurityFilterChain(
        http: HttpSecurity,
        oidcUserService: JvmGuardOidcUserService,
    ): SecurityFilterChain {
        http.authorizeHttpRequests { it.requestMatchers("/error", "/icons/**").permitAll() }
        if (JvmGuardConfig.isIntegrationTest || java.lang.Boolean.getBoolean("jvmguard.testControlFilter")) {
            http.authorizeHttpRequests { it.requestMatchers("/test", "/test/**").permitAll() }
        }
        return http.with(VaadinSecurityConfigurer.vaadin()) { configurer ->
            configurer.loginView(LoginView::class.java)
            configurer.addLogoutHandler { _, _, authentication ->
                (authentication?.principal as? JvmGuardPrincipal)?.serverConnection?.logout()
            }
        }.oauth2Login { oauth2 ->
            oauth2.loginPage("/login")
            oauth2.userInfoEndpoint { it.oidcUserService(oidcUserService) }
            oauth2.successHandler(ssoSuccessHandler())
        }.build()
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
