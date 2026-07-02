package com.jvmguard.server

import com.jvmguard.ui.server.JvmGuardLoginDetails
import com.jvmguard.ui.server.JvmGuardUserDetails
import com.jvmguard.connector.api.MockMode
import com.jvmguard.connector.api.Server
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import javax.security.auth.login.FailedLoginException

/**
 * Bridges Spring Security to the existing backend login. It does not validate credentials itself:
 * it delegates to [Server.login] (which owns password/TOTP/LDAP/mock handling) and, on success,
 * wraps the returned [com.jvmguard.connector.api.ServerConnection] and the user's access level in a
 * [JvmGuardUserDetails] principal. Backend authorization (`ServerConnectionImpl.require`) remains the
 * authoritative boundary; see `docs/migration/spring-boot-phase2.md`.
 */
class JvmGuardAuthenticationProvider(private val server: Server) : AuthenticationProvider {

    override fun authenticate(authentication: Authentication): Authentication {
        val loginName = authentication.name
        val password = authentication.credentials?.toString() ?: ""

        var authenticatorCode: String? = null
        var mockMode = MockMode.NONE
        (authentication.details as? JvmGuardLoginDetails)?.let {
            authenticatorCode = it.authenticatorCode
            mockMode = it.mockMode
        }

        try {
            val user = server.authenticate(loginName, password, authenticatorCode)
            val connection = server.connect(user, mockMode)
            val principal = JvmGuardUserDetails(user.loginName, user.accessLevel, connection)
            return UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.authorities)
        } catch (e: FailedLoginException) {
            throw BadCredentialsException("Invalid user name, password, or authenticator code.", e)
        }
    }

    override fun supports(authentication: Class<*>): Boolean =
        UsernamePasswordAuthenticationToken::class.java.isAssignableFrom(authentication)
}
