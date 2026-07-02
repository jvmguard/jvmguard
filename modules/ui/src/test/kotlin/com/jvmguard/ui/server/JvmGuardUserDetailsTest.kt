package com.jvmguard.ui.server

import com.jvmguard.data.user.AccessLevel
import com.jvmguard.data.user.Roles
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class JvmGuardUserDetailsTest {

    private fun roles(details: JvmGuardUserDetails): Set<String> =
        details.authorities.mapNotNull { it.authority }.toSet()

    @Test
    fun authoritiesExpandTheAccessLevelHierarchy() {
        assertEquals(setOf(role(Roles.VIEWER)), roles(JvmGuardUserDetails("u", AccessLevel.VIEWER, null)))
        assertEquals(setOf(role(Roles.VIEWER), role(Roles.PROFILER)), roles(JvmGuardUserDetails("u", AccessLevel.PROFILER, null)))
        assertEquals(setOf(role(Roles.VIEWER), role(Roles.PROFILER), role(Roles.ADMIN)), roles(JvmGuardUserDetails("u", AccessLevel.ADMIN, null)))
    }

    private fun role(name: String) = JvmGuardUserDetails.ROLE_PREFIX + name
}
