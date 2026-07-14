package dev.jvmguard.server.sso

import dev.jvmguard.common.config.ConfigManager
import dev.jvmguard.connector.api.MockMode
import dev.jvmguard.connector.api.Server
import dev.jvmguard.connector.api.SsoLoginError
import dev.jvmguard.connector.api.SsoLoginException
import dev.jvmguard.data.config.SsoProviderConfig
import org.springframework.security.authentication.AuthenticationServiceException
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService
import org.springframework.stereotype.Component
import javax.security.auth.login.LoginException

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

        val providerConfig = configManager.getGlobalConfig(false).ssoConfig.providers
            .find { it.issuerUri.trim().trimEnd('/') == issuer.trim().trimEnd('/') && it.enabled }

        if (email.isEmpty()) {
            throw SsoAuthenticationException(SsoLoginError.EMAIL_MISSING, oidcUser.idToken.tokenValue)
        }
        if (!emailVerified && providerConfig?.requireVerifiedEmail != false) {
            throw SsoAuthenticationException(SsoLoginError.EMAIL_NOT_VERIFIED, oidcUser.idToken.tokenValue)
        }

        val groups = extractGroups(claims, providerConfig)
        val name = claims["name"] as? String

        val user = try {
            server.authenticateSso(issuer, subject, email, name, groups)
        } catch (e: SsoLoginException) {
            throw SsoAuthenticationException(e.error, oidcUser.idToken.tokenValue)
        } catch (_: LoginException) {
            throw SsoAuthenticationException(SsoLoginError.GENERIC, oidcUser.idToken.tokenValue)
        }
        val connection = server.connect(user, MockMode.NONE)

        return JvmGuardOidcUser(oidcUser, user.loginName, user.accessLevel, connection)
    }

    private fun extractGroups(claims: Map<String, Any>, providerConfig: SsoProviderConfig?): List<String> {
        if (providerConfig == null || !providerConfig.preset.supportsGroups) {
            return emptyList()
        }
        val claimName = providerConfig.claimName
        return when (val claim = claims[claimName]) {
            is List<*> -> claim.mapNotNull { it?.toString() }
            is String -> listOf(claim)
            else -> emptyList()
        }
    }
}
