package com.jvmguard.data.config.triggers.actions

import com.jvmguard.agent.config.base.ConfigDoc

abstract class TextAction : TriggerAction {

    @field:ConfigDoc("The message/log text.")
    var text: String = ""
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    protected constructor()

    protected constructor(text: String) {
        this.text = text
    }
}
