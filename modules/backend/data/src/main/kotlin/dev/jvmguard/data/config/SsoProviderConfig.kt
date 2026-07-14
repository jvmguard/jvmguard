package dev.jvmguard.data.config

import dev.jvmguard.data.base.StoredConfig

open class SsoProviderConfig : StoredConfig(), AuthenticationContainer {

    var displayName: String = ""
        set(value) { field = changed(field, value) }

    var preset: SsoPreset = SsoPreset.GENERIC_OIDC
        set(value) { field = changed(field, value) }

    var issuerUri: String = ""
        set(value) { field = changed(field, value) }

    override var isAuthenticate: Boolean
        get() = true
        set(value) {}

    override var userName: String = ""
        set(value) { field = changed(field, value) }

    override var password: String = ""
        set(value) { field = changed(field, value) }

    var clientId: String
        get() = userName
        set(value) { userName = value }

    var clientSecret: String
        get() = password
        set(value) { password = value }

    var domainRestriction: String = ""
        set(value) { field = changed(field, value) }

    var scopes: String = "openid,profile,email"
        set(value) { field = changed(field, value) }

    var userNameAttribute: String = "email"
        set(value) { field = changed(field, value) }

    var claimName: String = "groups"
        set(value) { field = changed(field, value) }

    var enabled: Boolean = true
        set(value) { field = changed(field, value) }

    var requireVerifiedEmail: Boolean = true
        set(value) { field = changed(field, value) }

    var accessRules: MutableList<SsoGroupMapping> = ArrayList()

    fun effectiveClientId(): String = clientIdEnvOverride(displayName) ?: clientId
    fun effectiveClientSecret(): String = clientSecretEnvOverride(displayName) ?: clientSecret

    override fun toString(): String =
        "SsoProviderConfig{displayName='$displayName', preset=$preset, enabled=$enabled}"

    companion object {
        fun slugify(name: String): String =
            name.lowercase()
                .replace(Regex("[^a-z0-9]+"), "-")
                .trim('-')
                .ifEmpty { "provider" }

        fun clientIdEnvOverride(displayName: String): String? = envOverride(displayName, "CLIENT_ID")
        fun clientSecretEnvOverride(displayName: String): String? = envOverride(displayName, "CLIENT_SECRET")

        private fun envOverride(displayName: String, suffix: String): String? {
            if (displayName.isBlank()) return null
            val prefix = "JVMGUARD_SSO_" + displayName.uppercase().replace(Regex("[^A-Z0-9]+"), "_").trim('_')
            return System.getenv("${prefix}_$suffix")?.takeIf { it.isNotBlank() }
        }
    }
}
