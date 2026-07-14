package dev.jvmguard.data.config.triggers.actions

import dev.jvmguard.agent.config.base.ConfigDoc

sealed class TextAction : TriggerAction {

    @field:ConfigDoc("The message/log text.")
    var text: String = ""
        set(value) { field = changed(field, value) }

    protected constructor()

    protected constructor(text: String) {
        this.text = text
    }
}
