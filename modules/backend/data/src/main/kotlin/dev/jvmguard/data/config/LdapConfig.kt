package dev.jvmguard.data.config

import dev.jvmguard.data.base.StoredConfig

open class LdapConfig : StoredConfig(), AuthenticationContainer {

    var url: String = ""
        set(value) { field = changed(field, value) }

    var useStartTls: Boolean = false
        set(value) { field = changed(field, value) }

    private var authenticate: Boolean = false

    override var isAuthenticate: Boolean
        get() = authenticate
        set(value) { authenticate = changed(authenticate, value) }

    override var userName: String = ""
        set(value) { field = changed(field, value) }

    override var password: String = ""
        set(value) { field = changed(field, value) }

    var userMappings: MutableList<LdapUserMapping> = ArrayList()

    val verbose: String
        get() = if (url.isEmpty()) {
            "<span class=\"error-text\">No LDAP server defined</span>"
        } else {
            buildString {
                append("<b>$url")
                if (useStartTls) {
                    append(", use StartTLS")
                }
                val size = userMappings.size
                if (size > 0) {
                    append(", $size ${if (size == 1) "automatic mapping" else "automatic mappings"}")
                }
            }
        }
}
