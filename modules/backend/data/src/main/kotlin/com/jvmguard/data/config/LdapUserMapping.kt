package com.jvmguard.data.config

import com.jvmguard.data.base.StoredConfig
import com.jvmguard.data.user.AccessLevel

open class LdapUserMapping : StoredConfig() {

    var searchBase: String = ""
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var userFilter: String = ""
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var accessLevel: AccessLevel = AccessLevel.DEFAULT
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    override fun toString(): String =
        "LdapUserMapping{searchBase='$searchBase', userFilter='$userFilter', accessLevel=$accessLevel}"

    companion object {
        const val TOKEN_USER: String = "@USER@"
    }
}
