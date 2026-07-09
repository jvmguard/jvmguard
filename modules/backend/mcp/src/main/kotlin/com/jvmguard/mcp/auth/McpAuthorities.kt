package com.jvmguard.mcp.auth

import com.jvmguard.data.user.AccessLevel
import org.springframework.security.core.authority.SimpleGrantedAuthority

object McpAuthorities {

    const val ROLE_PREFIX = "ROLE_"

    fun forAccessLevel(accessLevel: AccessLevel): List<SimpleGrantedAuthority> =
        AccessLevel.entries
            .filter { accessLevel.isAtLeast(it) }
            .map { SimpleGrantedAuthority(ROLE_PREFIX + it.name) }
}
