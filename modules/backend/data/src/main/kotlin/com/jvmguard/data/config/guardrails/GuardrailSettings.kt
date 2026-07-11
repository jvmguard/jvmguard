package com.jvmguard.data.config.guardrails

import com.jvmguard.agent.config.base.DefaultConstructor
import com.jvmguard.agent.config.base.OptionalConfig

open class GuardrailSettings @DefaultConstructor constructor() : OptionalConfig() {

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

    var allowMbeanMutations: Boolean = true
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
}
