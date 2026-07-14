package dev.jvmguard.data.config.triggers.actions

import dev.jvmguard.agent.config.base.ConfigDoc
import dev.jvmguard.agent.config.base.DefaultConstructor
import dev.jvmguard.agent.config.base.LogCategory

@ConfigDoc("Sends an email notification.")
open class EmailAction : TextAction {

    @field:ConfigDoc("Recipient email address.")
    var email: String = ""
        set(value) { field = changed(field, value) }

    @field:ConfigDoc("Severity label of the email (Info/Warning/Error).")
    var category: String = LogCategory.WARNING.toString()
        set(value) { field = changed(field, value) }

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
