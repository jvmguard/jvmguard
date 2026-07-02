package com.jvmguard.ui.server

import com.jvmguard.connector.api.MockMode
import com.jvmguard.connector.api.ServerConnection
import com.vaadin.flow.server.VaadinService
import com.vaadin.flow.server.VaadinServletRequest
import com.vaadin.flow.server.VaadinServletResponse
import com.vaadin.flow.spring.security.AuthenticationContext
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.security.web.context.SecurityContextRepository

object SecurityBridge {

    private val securityContextRepository: SecurityContextRepository = HttpSessionSecurityContextRepository()

    @Volatile
    private var authenticationManager: AuthenticationManager? = null

    @Volatile
    private var authenticationContext: AuthenticationContext? = null

    fun init(authenticationManager: AuthenticationManager, authenticationContext: AuthenticationContext) {
        this.authenticationManager = authenticationManager
        this.authenticationContext = authenticationContext
    }

    fun authenticate(userName: String, password: String, authenticatorCode: String?, mockMode: MockMode): ServerConnection {
        val manager = checkNotNull(authenticationManager) { "SecurityBridge is not initialized" }
        val token = UsernamePasswordAuthenticationToken(userName, password)
        token.details = JvmGuardLoginDetails(authenticatorCode, mockMode)
        val authentication = manager.authenticate(token)
        val context = SecurityContextHolder.createEmptyContext()
        context.authentication = authentication
        SecurityContextHolder.setContext(context)
        persist(context)
        val principal = authentication.principal as JvmGuardUserDetails
        return checkNotNull(principal.serverConnection) { "Authenticated principal has no server connection" }
    }

    fun logout() {
        authenticationContext?.logout()
    }

    private fun persist(context: SecurityContext) {
        val request = VaadinService.getCurrentRequest()
        val response = VaadinService.getCurrentResponse()
        if (request is VaadinServletRequest && response is VaadinServletResponse) {
            securityContextRepository.saveContext(context, request.httpServletRequest, response.httpServletResponse)
        }
    }
}
