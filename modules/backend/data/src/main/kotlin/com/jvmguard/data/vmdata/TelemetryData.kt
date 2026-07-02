package com.jvmguard.data.vmdata

import com.jvmguard.common.helper.Timestamp.Interval

class TelemetryData {
    var rootNode: TelemetryNode? = null
    var timestamps: LongArray? = null
    var telemetryInterval: TelemetryInterval? = null
    var dataInterval: Interval? = null
    var isNoPreviousData: Boolean = false

    override fun toString(): String =
        "TelemetryData{rootNode=$rootNode, timestamps=${timestamps.contentToString()}}"
}
