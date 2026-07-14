package dev.jvmguard.data.config.sets

import dev.jvmguard.agent.config.base.DefaultConstructor
import dev.jvmguard.data.base.StoredType
import dev.jvmguard.data.config.thresholds.Threshold

@StoredType("threshold_set")
open class ThresholdSet : AbstractSet<Threshold> {

    @DefaultConstructor
    constructor()

    constructor(name: String, triggers: Collection<Threshold>) : super(name, triggers)
}
