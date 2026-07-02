package com.jvmguard.data.vmdata

class CustomTelemetryNodeIdentifier(
    val type: Type,
    val name: String,
) : Comparable<CustomTelemetryNodeIdentifier> {

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val that = other as CustomTelemetryNodeIdentifier
        if (name != that.name) {
            return false
        }
        if (type != that.type) {
            return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }

    override fun compareTo(other: CustomTelemetryNodeIdentifier): Int {
        val ret = type.ordinal - other.type.ordinal
        return if (ret == 0) {
            name.compareTo(other.name)
        } else {
            ret
        }
    }

    override fun toString(): String =
        "CustomTelemetryNodeIdentifier{type=$type, name='$name'}"

    enum class Type {
        MBEAN,
        DEVOPS,
    }
}
