package com.jvmguard.data.config

import com.jvmguard.data.base.StoredConfig

open class GuardrailConfig : StoredConfig() {

    var mcpReadOnly: Boolean = false
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var maxRecordingSeconds: Int = 600
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var captureCooldownSeconds: Int = 0
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var allowHeapDump: Boolean = true
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var allowJps: Boolean = true
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var allowJfr: Boolean = true
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var allowRunGc: Boolean = true
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
