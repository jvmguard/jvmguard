package com.jvmguard.data.config.triggers.actions

import com.jvmguard.agent.config.base.DefaultConstructor
import com.jvmguard.agent.config.base.LogCategory

open class EmailAction : TextAction {

    var email: String = ""
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var category: String = LogCategory.WARNING.toString()
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    @DefaultConstructor
    constructor()

    constructor(email: String, category: String, text: String) : super(text) {
        this.email = email
        this.category = category
    }

    override val actionType: ActionType
        get() = ActionType.EMAIL

    override val parameterDescription: String
        get() = email
}
