package com.jvmguard.data.user.viewsettings

import com.jvmguard.data.vmdata.TelemetryIdentifier
import com.jvmguard.data.vmdata.TelemetryType
import java.io.Serializable

open class SparkLineMemento() : Serializable {

    var mainId: String? = null
    var subId: String? = null

    constructor(mainId: String?, subId: String?) : this() {
        this.mainId = mainId
        this.subId = subId
    }

    constructor(telemetryIdentifier: TelemetryIdentifier) :
            this(telemetryIdentifier.mainId, telemetryIdentifier.subId)

    constructor(telemetryType: TelemetryType) : this(telemetryType.telemetryIdentifier)

    fun matches(telemetryType: TelemetryType): Boolean {
        val telemetryIdentifier = telemetryType.telemetryIdentifier
        return telemetryIdentifier.mainId == mainId && telemetryIdentifier.subId == subId
    }
}
