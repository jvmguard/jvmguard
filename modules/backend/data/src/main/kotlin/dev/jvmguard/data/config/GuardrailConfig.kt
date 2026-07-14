package dev.jvmguard.data.config

import dev.jvmguard.data.base.StoredConfig

open class GuardrailConfig : StoredConfig() {

    var mcpReadOnly: Boolean = false
        set(value) { field = changed(field, value) }

    var apiAllowedIps: String = ""
        set(value) { field = changed(field, value) }
}
