package com.jvmguard.data.config

import com.jvmguard.data.base.StoredConfig
import com.jvmguard.data.user.AccessLevel

open class SsoGroupMapping : StoredConfig() {

    var claimValue: String = ""
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var accessLevel: AccessLevel = AccessLevel.VIEWER
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    val isCatchAll: Boolean
        get() = claimValue == CATCH_ALL

    override fun toString(): String =
        "SsoGroupMapping{claimValue='$claimValue', accessLevel=$accessLevel}"

    companion object {
        const val CATCH_ALL: String = "*"
    }
}
