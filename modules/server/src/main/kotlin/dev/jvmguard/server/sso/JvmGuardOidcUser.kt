package dev.jvmguard.server.sso

import dev.jvmguard.connector.api.ServerConnection
import dev.jvmguard.data.user.AccessLevel
import dev.jvmguard.ui.server.JvmGuardPrincipal
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.core.oidc.OidcUserInfo
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import java.io.Serializable

/**
 * SSO / OpenID Connect principal, produced by JvmGuardOidcUserService after a successful
 * OIDC login.
 */
class JvmGuardOidcUser(
    private val delegate: OidcUser,
    override val loginName: String,
    override val accessLevel: AccessLevel,
    @Transient override val serverConnection: ServerConnection?,
) : OidcUser, JvmGuardPrincipal, Serializable {

    private val authorities: List<GrantedAuthority> =
        AccessLevel.entries
            .filter { accessLevel.isAtLeast(it) }
            .map { SimpleGrantedAuthority(ROLE_PREFIX + it.name) }

    override fun getAuthorities(): Collection<GrantedAuthority> = authorities

    override fun getClaims(): Map<String, Any> = delegate.claims

    override fun getAttributes(): Map<String, Any> = delegate.attributes

    override fun getUserInfo(): OidcUserInfo? = delegate.userInfo

    override fun getIdToken(): OidcIdToken = delegate.idToken

    override fun getName(): String = loginName

    companion object {
        const val ROLE_PREFIX = "ROLE_"

        @Suppress("unused")
        private const val serialVersionUID = 1L
    }
}
