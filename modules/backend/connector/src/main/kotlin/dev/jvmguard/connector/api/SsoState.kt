package dev.jvmguard.connector.api

/**
 * Holds the set of SSO provider registration slugs that were successfully built by
 * MutableClientRegistrationRepository
 */
object SsoState {
    @Volatile
    var activeSlugs: Set<String> = emptySet()
}
