package com.jvmguard.common.telemetry

open class AdditionalTelemetryIdentifier(val type: Int, val name: String?) {

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is AdditionalTelemetryIdentifier) {
            return false
        }
        return type == other.type && name == other.name
    }

    override fun hashCode(): Int {
        var result = type
        result = 31 * result + (name?.hashCode() ?: 0)
        return result
    }
}
