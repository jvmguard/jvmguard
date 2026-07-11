package com.jvmguard.data.vmdata

import com.jvmguard.agent.config.base.ConfigDoc
import com.jvmguard.agent.config.base.DefaultConstructor
import java.io.Serializable

open class TelemetryIdentifier : Serializable, Comparable<TelemetryIdentifier> {

    @field:ConfigDoc("Primary telemetry key (the metric id).")
    var mainId: String = ""
    @field:ConfigDoc("Secondary telemetry key (the sub-metric id).")
    var subId: String = ""

    @DefaultConstructor
    constructor()

    constructor(mainId: String?, subId: String?) {
        this.mainId = mainId ?: ""
        this.subId = subId ?: ""
    }

    val combinedId: String
        get() = "$mainId$subId"

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is TelemetryIdentifier) {
            return false
        }
        return combinedId == other.combinedId
    }

    override fun hashCode(): Int = combinedId.hashCode()

    override fun toString(): String = combinedId

    override fun compareTo(other: TelemetryIdentifier): Int =
        other.combinedId.compareTo(combinedId)
}
