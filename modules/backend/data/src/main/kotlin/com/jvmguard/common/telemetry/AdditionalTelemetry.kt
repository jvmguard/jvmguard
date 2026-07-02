package com.jvmguard.common.telemetry

import com.jvmguard.data.vmdata.TelemetryType

class AdditionalTelemetry internal constructor(
    type: Int,
    name: String,
    var assignedId: Int,
    hidden: Boolean,
) : AdditionalTelemetryIdentifier(type, name) {

    val description: String
    var isHidden: Boolean = hidden
        internal set

    val nodeName: String
    private val lineNameInternal: String?

    init {
        val tabIndex = name.indexOf('\t')
        if (tabIndex == -1) {
            nodeName = name
            lineNameInternal = null
            description = nodeName
        } else {
            nodeName = name.substring(0, tabIndex)
            lineNameInternal = name.substring(tabIndex + 1)
            description = "$nodeName ($lineNameInternal)"
        }
    }

    val lineName: String
        get() = lineNameInternal ?: nodeName

    val assignedStringId: String
        get() = TelemetryType.SUB_ID_ADDITIONAL_PREFIX + assignedId.toString(36)
}
