package com.jvmguard.data.config.sets

import com.jvmguard.agent.config.base.DefaultConstructor
import com.jvmguard.data.base.StoredType
import com.jvmguard.data.config.triggers.actions.TriggerAction

@StoredType("action_set")
open class ActionSet : AbstractSet<TriggerAction> {

    @DefaultConstructor
    constructor()

    constructor(name: String, triggerActions: Collection<TriggerAction>) : super(name, triggerActions)
}
