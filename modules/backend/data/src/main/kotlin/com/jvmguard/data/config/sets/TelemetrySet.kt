package com.jvmguard.data.config.sets

import com.jvmguard.agent.config.base.DefaultConstructor
import com.jvmguard.agent.config.telemetry.MBeanTelemetryConfig
import com.jvmguard.data.base.StoredType

@StoredType("telemetry_set")
open class TelemetrySet : AbstractSet<MBeanTelemetryConfig> {

    @DefaultConstructor
    constructor()

    constructor(name: String, transactionDefs: Collection<MBeanTelemetryConfig>) : super(name, transactionDefs)
}
