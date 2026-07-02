package com.jvmguard.rest.entity

import jakarta.xml.bind.annotation.XmlAttribute
import jakarta.xml.bind.annotation.XmlRootElement

interface EntityList

interface SingleStringEntity

@XmlRootElement(name = "group")
class GroupEntity(
    @JvmField @XmlAttribute var name: String?,
    @JvmField @XmlAttribute var pool: Boolean,
) {
    override fun toString(): String = name ?: ""
}

@XmlRootElement(name = "telemetry")
class TelemetryDescriptor(name: String, type: TelemetryType, description: String) {

    @JvmField
    @XmlAttribute
    var name: String? = type.namePrefix + name

    @JvmField
    @XmlAttribute
    var description: String? = description

    override fun toString(): String = name ?: ""

    enum class TelemetryType(val namePrefix: String) {
        BASE(""),
        DEVOPS("devops/"),
        MBEAN("mbean/"),
    }
}
