package com.jvmguard.server.sso

import com.jvmguard.common.config.ConfigManager
import com.jvmguard.connector.api.MockMode
import com.jvmguard.connector.api.Server
import com.jvmguard.data.config.SsoProviderConfig
import org.springframework.security.authentication.AuthenticationServiceException
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService
import org.springframework.stereotype.Component

@Component
class JvmGuardOidcUserService(
    private val server: Server,
    private val configManager: ConfigManager,
) : OAuth2UserService<OidcUserRequest, OidcUser> {

    private val delegate = OidcUserService()

    override fun loadUser(userRequest: OidcUserRequest): OidcUser {
        val oidcUser = delegate.loadUser(userRequest)

        val issuer = userRequest.clientRegistration.providerDetails.issuerUri
            ?: throw AuthenticationServiceException("OIDC provider has no issuer URI")
        val subject = oidcUser.subject
            ?: throw AuthenticationServiceException("OIDC token has no subject claim")
        val claims = oidcUser.claims
        val email = claims["email"] as? String ?: ""
        val emailVerified = claims["email_verified"] as? Boolean ?: true

        if (email.isEmpty()) {
            throw AuthenticationServiceException("IdP did not provide an email claim")
        }
        if (!emailVerified) {
            throw AuthenticationServiceException("IdP email is not verified")
        }

        val providerConfig = configManager.getGlobalConfig(false).ssoConfig.providers
            .find { it.issuerUri.trim() == issuer.trim() && it.enabled }
        val groups = extractGroups(claims, providerConfig)
        val name = claims["name"] as? String

        val user = server.authenticateSso(issuer, subject, email, name, groups)
        val connection = server.connect(user, MockMode.NONE)

        return JvmGuardOidcUser(oidcUser, user.loginName, user.accessLevel, connection)
    }

    private fun extractGroups(claims: Map<String, Any>, providerConfig: SsoProviderConfig?): List<String> {
        if (providerConfig == null || !providerConfig.preset.supportsGroups) {
            return emptyList()
        }
        return when (val claim = claims[providerConfig.claimName]) {
            is List<*> -> claim.mapNotNull { it?.toString() }
            is String -> listOf(claim)
            else -> emptyList()
        }
    }
}
