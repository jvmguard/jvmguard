package dev.jvmguard.ui.server

import dev.jvmguard.data.user.AccessLevel
import dev.jvmguard.data.user.Roles
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RolesTest {

    @Test
    fun roleConstantsMirrorAccessLevelNames() {
        assertEquals(AccessLevel.VIEWER.name, Roles.VIEWER)
        assertEquals(AccessLevel.PROFILER.name, Roles.PROFILER)
        assertEquals(AccessLevel.ADMIN.name, Roles.ADMIN)
        // Every AccessLevel needs a matching role constant so @RolesAllowed can reference all of them.
        val constants = setOf(Roles.VIEWER, Roles.PROFILER, Roles.ADMIN)
        assertEquals(AccessLevel.entries.map { it.name }.toSet(), constants)
    }
}
