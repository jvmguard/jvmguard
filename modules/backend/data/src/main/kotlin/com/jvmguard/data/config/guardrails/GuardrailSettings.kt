package com.jvmguard.data.config.guardrails

import com.jvmguard.agent.config.base.DefaultConstructor
import com.jvmguard.agent.config.base.OptionalConfig

open class GuardrailSettings @DefaultConstructor constructor() : OptionalConfig() {

    var allowHeapDump: Boolean = true
        set(value) { field = changed(field, value) }

    var allowJps: Boolean = true
        set(value) { field = changed(field, value) }

    var allowJfr: Boolean = true
        set(value) { field = changed(field, value) }

    var allowMbeanMutations: Boolean = true
        set(value) { field = changed(field, value) }

    var allowConfigEdit: Boolean = true
        set(value) { field = changed(field, value) }

    var maxRecordingSeconds: Int = 600
        set(value) { field = changed(field, value) }

    var captureCooldownSeconds: Int = 0
        set(value) { field = changed(field, value) }
}
