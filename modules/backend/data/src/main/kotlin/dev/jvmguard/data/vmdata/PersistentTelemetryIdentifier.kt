package dev.jvmguard.data.vmdata

import dev.jvmguard.agent.config.base.ConfigDoc
import dev.jvmguard.agent.config.base.DefaultConstructor

open class PersistentTelemetryIdentifier : TelemetryIdentifier {

    @field:ConfigDoc("Discriminator for an additional telemetry dimension (0 = none).")
    var additionalType: Int = DEFAULT_ADDITIONAL_TYPE
        private set
    @field:ConfigDoc("Name of the additional telemetry dimension.")
    var additionalName: String = DEFAULT_ADDITIONAL_NAME
        private set

    @DefaultConstructor
    constructor()

    constructor(mainId: String?, subId: String?) : super(mainId, subId)

    constructor(mainId: String?, subId: String?, additionalType: Int, additionalName: String) : this(mainId, subId) {
        this.additionalType = additionalType
        this.additionalName = additionalName
    }

    companion object {
        const val DEFAULT_ADDITIONAL_TYPE: Int = 0
        const val DEFAULT_ADDITIONAL_NAME: String = ""
    }
}
