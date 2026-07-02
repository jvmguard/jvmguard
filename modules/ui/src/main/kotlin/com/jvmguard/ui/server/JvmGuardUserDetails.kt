package com.jvmguard.ui.server

import com.jvmguard.data.user.AccessLevel
import com.jvmguard.connector.api.ServerConnection
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.io.Serializable

class JvmGuardUserDetails(
    private val loginName: String,
    val accessLevel: AccessLevel,
    @Transient val serverConnection: ServerConnection?,
) : UserDetails, Serializable {

    private val authorities: List<GrantedAuthority> =
        AccessLevel.entries
            .filter { accessLevel.isAtLeast(it) }
            .map { SimpleGrantedAuthority(ROLE_PREFIX + it.name) }

    override fun getAuthorities(): Collection<GrantedAuthority> = authorities

    override fun getPassword(): String = ""

    override fun getUsername(): String = loginName

    companion object {
        const val ROLE_PREFIX = "ROLE_"

        @Suppress("unused")
        private const val serialVersionUID = 1L
    }
}
