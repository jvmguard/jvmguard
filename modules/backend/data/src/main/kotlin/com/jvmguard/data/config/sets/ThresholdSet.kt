package com.jvmguard.data.config.sets

import com.jvmguard.agent.config.base.DefaultConstructor
import com.jvmguard.data.base.StoredType
import com.jvmguard.data.config.thresholds.Threshold

@StoredType("threshold_set")
open class ThresholdSet : AbstractSet<Threshold> {

    @DefaultConstructor
    constructor()

    constructor(name: String, triggers: Collection<Threshold>) : super(name, triggers)
}
