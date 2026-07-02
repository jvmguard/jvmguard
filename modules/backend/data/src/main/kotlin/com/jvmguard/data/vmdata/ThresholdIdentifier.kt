package com.jvmguard.data.vmdata

import com.jvmguard.agent.config.base.DefaultConstructor
import java.io.Serializable
import java.util.*

open class ThresholdIdentifier : Serializable, Comparable<ThresholdIdentifier> {

    var telemetryIdentifier: PersistentTelemetryIdentifier? = null

    var customName: String? = null
        set(value) {
            field = if (value.isNullOrEmpty()) null else value
        }

    @DefaultConstructor
    constructor()

    constructor(telemetryIdentifier: PersistentTelemetryIdentifier?, customName: String?) {
        this.telemetryIdentifier = telemetryIdentifier
        this.customName = customName
    }

    constructor(telemetryIdentifier: PersistentTelemetryIdentifier?) {
        this.telemetryIdentifier = telemetryIdentifier
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val that = other as ThresholdIdentifier
        if (!Objects.equals(customName, that.customName)) {
            return false
        }
        if (!Objects.equals(telemetryIdentifier, that.telemetryIdentifier)) {
            return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = telemetryIdentifier?.hashCode() ?: 0
        result = 31 * result + (customName?.hashCode() ?: 0)
        return result
    }

    override fun compareTo(other: ThresholdIdentifier): Int {
        var ret = compareValues(other.telemetryIdentifier, telemetryIdentifier)
        if (ret == 0) {
            ret = compareValues(other.customName, customName)
        }
        return ret
    }

    override fun toString(): String =
        "ThresholdIdentifier{telemetryIdentifier=$telemetryIdentifier, customName='$customName'}"
}
