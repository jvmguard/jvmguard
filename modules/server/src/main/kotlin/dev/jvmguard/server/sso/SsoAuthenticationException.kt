package dev.jvmguard.server.sso

import dev.jvmguard.connector.api.SsoLoginError
import org.springframework.security.authentication.AuthenticationServiceException

class SsoAuthenticationException(
    val error: SsoLoginError,
    val idTokenValue: String? = null,
) : AuthenticationServiceException(error.message)
