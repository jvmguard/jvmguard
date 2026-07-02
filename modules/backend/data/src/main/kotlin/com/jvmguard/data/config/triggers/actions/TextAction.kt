package com.jvmguard.data.config.triggers.actions

abstract class TextAction : TriggerAction {

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
