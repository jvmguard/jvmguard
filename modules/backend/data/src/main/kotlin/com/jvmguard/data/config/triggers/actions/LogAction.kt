package com.jvmguard.data.config.triggers.actions

import com.jvmguard.agent.config.base.DefaultConstructor
import com.jvmguard.agent.config.base.LogCategory

open class LogAction : TextAction {

    var category: LogCategory = LogCategory.INFO
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    @DefaultConstructor
    constructor()

    constructor(category: LogCategory, text: String) : super(text) {
        this.category = category
    }

    override val actionType: ActionType
        get() = ActionType.LOG

    override val parameterDescription: String?
        get() = null
}
