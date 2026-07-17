package dev.jvmguard.server

import dev.jvmguard.common.helper.LoginThrottle
import dev.jvmguard.rest.restInterface.RestInterface
import dev.jvmguard.ui.server.JvmGuardUserDetails
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication

class RestApiKeyAuthenticationProvider(
    private val restInterface: RestInterface,
    private val loginThrottle: LoginThrottle,
) : AuthenticationProvider {

    override fun authenticate(authentication: Authentication): Authentication {
        val loginName = authentication.name
        val apiKey = authentication.credentials?.toString() ?: ""

        if (loginThrottle.isThrottled(loginName)) {
            throw BadCredentialsException("Too many failed login attempts")
        }
        val accessLevel = restInterface.checkAccess(loginName, apiKey)
        if (accessLevel == null) {
            loginThrottle.loginFailed(loginName)
            throw BadCredentialsException("Invalid API key")
        }
        loginThrottle.loginSucceeded(loginName)
        val principal = JvmGuardUserDetails(loginName, accessLevel, null)
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.authorities)
    }

    override fun supports(authentication: Class<*>): Boolean =
        UsernamePasswordAuthenticationToken::class.java.isAssignableFrom(authentication)
}
