package dev.jvmguard.data.config.sets

import dev.jvmguard.agent.config.base.DefaultConstructor
import dev.jvmguard.agent.config.telemetry.MBeanTelemetryConfig
import dev.jvmguard.data.base.StoredType

@StoredType("telemetry_set")
open class TelemetrySet : AbstractSet<MBeanTelemetryConfig> {

    @DefaultConstructor
    constructor()

    constructor(name: String, transactionDefs: Collection<MBeanTelemetryConfig>) : super(name, transactionDefs)
}
