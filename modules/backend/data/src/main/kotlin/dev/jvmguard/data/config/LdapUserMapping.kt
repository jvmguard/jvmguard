package dev.jvmguard.data.config

import dev.jvmguard.data.base.StoredConfig
import dev.jvmguard.data.user.AccessLevel

open class LdapUserMapping : StoredConfig() {

    var searchBase: String = ""
        set(value) { field = changed(field, value) }

    var userFilter: String = ""
        set(value) { field = changed(field, value) }

    var accessLevel: AccessLevel = AccessLevel.DEFAULT
        set(value) { field = changed(field, value) }

    override fun toString(): String =
        "LdapUserMapping{searchBase='$searchBase', userFilter='$userFilter', accessLevel=$accessLevel}"

    companion object {
        const val TOKEN_USER: String = "@USER@"
    }
}
