package com.jvmguard.data.config

import com.jvmguard.data.base.StoredConfig

open class GuardrailConfig : StoredConfig() {

    var mcpReadOnly: Boolean = false
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var apiAllowedIps: String = ""
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }
}
