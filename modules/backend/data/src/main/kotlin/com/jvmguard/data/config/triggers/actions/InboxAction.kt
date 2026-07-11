package com.jvmguard.data.config.triggers.actions

import com.jvmguard.agent.config.base.ConfigDoc
import com.jvmguard.agent.config.base.DefaultConstructor

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
