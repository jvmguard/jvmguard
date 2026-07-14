package dev.jvmguard.data.user

/**
 * Spring Security role names mirror the [AccessLevel] enum constant names so that
 * `@RolesAllowed`/`@PreAuthorize` and the `JvmGuardUserDetails` authority expansion are linked.
 */
object Roles {
    const val VIEWER: String = "VIEWER"
    const val PROFILER: String = "PROFILER"
    const val ADMIN: String = "ADMIN"
}
