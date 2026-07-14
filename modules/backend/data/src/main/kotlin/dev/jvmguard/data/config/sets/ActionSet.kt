package dev.jvmguard.data.config.sets

import dev.jvmguard.agent.config.base.DefaultConstructor
import dev.jvmguard.data.base.StoredType
import dev.jvmguard.data.config.triggers.actions.TriggerAction

@StoredType("action_set")
open class ActionSet : AbstractSet<TriggerAction> {

    @DefaultConstructor
    constructor()

    constructor(name: String, triggerActions: Collection<TriggerAction>) : super(name, triggerActions)
}
