package dev.jvmguard.server

import dev.jvmguard.common.JvmGuardProperties
import dev.jvmguard.rest.restInterface.RestInterface
import dev.jvmguard.ui.server.JvmGuardUserDetails
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication

class RestApiKeyAuthenticationProvider(
    private val restInterface: RestInterface,
    properties: JvmGuardProperties,
) : AuthenticationProvider {

    private val failedAuthWaitMs = properties.restFailedAuthWait * 1000L

    override fun authenticate(authentication: Authentication): Authentication {
        val loginName = authentication.name
        val apiKey = authentication.credentials?.toString() ?: ""

        val accessLevel = restInterface.checkAccess(loginName, apiKey)
        if (accessLevel == null) {
            throttleBruteForce()
            throw BadCredentialsException("Invalid API key")
        }
        val principal = JvmGuardUserDetails(loginName, accessLevel, null)
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.authorities)
    }

    override fun supports(authentication: Class<*>): Boolean =
        UsernamePasswordAuthenticationToken::class.java.isAssignableFrom(authentication)

    private fun throttleBruteForce() {
        if (failedAuthWaitMs <= 0) {
            return
        }
        try {
            Thread.sleep(failedAuthWaitMs)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }
}
