package com.jvmguard.data.config.triggers.actions

import com.jvmguard.agent.config.base.DefaultConstructor

open class InboxAction : TextAction {

    @DefaultConstructor
    constructor()

    constructor(text: String) : super(text)

    override val actionType: ActionType
        get() = ActionType.INBOX

    override val parameterDescription: String?
        get() = null
}
