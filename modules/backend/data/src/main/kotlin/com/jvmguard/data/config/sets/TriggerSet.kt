package com.jvmguard.data.config.sets

import com.jvmguard.agent.config.base.DefaultConstructor
import com.jvmguard.data.base.StoredType
import com.jvmguard.data.config.triggers.Trigger

@StoredType("trigger_set")
open class TriggerSet : AbstractSet<Trigger> {

    @DefaultConstructor
    constructor()

    constructor(name: String, triggers: Collection<Trigger>) : super(name, cloneAndRemoveIds(triggers))

    companion object {
        private fun cloneAndRemoveIds(triggers: Collection<Trigger>): Collection<Trigger> =
            triggers.mapTo(ArrayList(triggers.size)) { it.clone() }
    }
}
