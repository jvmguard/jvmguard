package com.jvmguard.data.config

import com.jvmguard.data.base.StoredConfig

open class SsoProviderConfig : StoredConfig(), AuthenticationContainer {

    var displayName: String = ""
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var preset: SsoPreset = SsoPreset.GENERIC_OIDC
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var issuerUri: String = ""
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    override var isAuthenticate: Boolean
        get() = true
        set(value) {}

    override var userName: String = ""
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    override var password: String = ""
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var clientId: String
        get() = userName
        set(value) { userName = value }

    var clientSecret: String
        get() = password
        set(value) { password = value }

    var domainRestriction: String = ""
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var scopes: String = "openid,profile,email"
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var userNameAttribute: String = "email"
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var claimName: String = "groups"
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var enabled: Boolean = true
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var requireVerifiedEmail: Boolean = true
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var accessRules: MutableList<SsoGroupMapping> = ArrayList()

    override fun toString(): String =
        "SsoProviderConfig{displayName='$displayName', preset=$preset, enabled=$enabled}"

    companion object {
        fun slugify(name: String): String =
            name.lowercase()
                .replace(Regex("[^a-z0-9]+"), "-")
                .trim('-')
                .ifEmpty { "provider" }
    }
}
