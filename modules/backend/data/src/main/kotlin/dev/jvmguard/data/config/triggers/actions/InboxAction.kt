package dev.jvmguard.data.config.triggers.actions

import dev.jvmguard.agent.config.base.ConfigDoc
import dev.jvmguard.agent.config.base.DefaultConstructor

@ConfigDoc("Creates an inbox item.")
open class InboxAction : TextAction {

    @DefaultConstructor
    constructor()

    constructor(text: String) : super(text)

    override val actionType: ActionType
        get() = ActionType.INBOX

    override val parameterDescription: String?
        get() = null
}
