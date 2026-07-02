package com.jvmguard.collector.telemetry

import com.jvmguard.collector.vmdata.structures.TelemetryCollection.CollectionType
import com.jvmguard.data.vmdata.Telemetry
import com.jvmguard.data.vmdata.TelemetryIdentifier
import com.jvmguard.data.vmdata.TelemetryType
import org.springframework.stereotype.Component

@Component
class CollectionTypeResolver {

    private val specialCollectionTypes = HashMap<TelemetryIdentifier, CollectionType>()

    fun addSpecial(telemetryIdentifier: TelemetryIdentifier, collectionType: CollectionType) {
        specialCollectionTypes[telemetryIdentifier] = collectionType
    }

    fun getCollectionType(telemetryIdentifier: TelemetryIdentifier): CollectionType {
        val specialType = specialCollectionTypes[telemetryIdentifier]
        if (specialType != null) {
            return specialType
        }
        val subId = telemetryIdentifier.subId
        return if (telemetryIdentifier.mainId == Telemetry.HEAP.mainId && subId.startsWith(TelemetryType.SUB_ID_ADDITIONAL_PREFIX)) {
            CollectionType.TELEMETRY_ONLY
        } else {
            CollectionType.BOTH
        }
    }
}
