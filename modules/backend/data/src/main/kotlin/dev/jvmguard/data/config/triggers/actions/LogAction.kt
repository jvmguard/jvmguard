package dev.jvmguard.data.config.triggers.actions

import dev.jvmguard.agent.config.base.ConfigDoc
import dev.jvmguard.agent.config.base.DefaultConstructor
import dev.jvmguard.agent.config.base.LogCategory

@ConfigDoc("Writes an entry to the event log.")
open class LogAction : TextAction {

    @field:ConfigDoc("Log severity (INFO/WARNING/ERROR).")
    var category: LogCategory = LogCategory.INFO
        set(value) { field = changed(field, value) }

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
