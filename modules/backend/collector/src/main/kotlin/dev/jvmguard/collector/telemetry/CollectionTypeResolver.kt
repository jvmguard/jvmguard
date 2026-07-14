package dev.jvmguard.collector.telemetry

import dev.jvmguard.collector.vmdata.structures.TelemetryCollection.CollectionType
import dev.jvmguard.data.vmdata.Telemetry
import dev.jvmguard.data.vmdata.TelemetryIdentifier
import dev.jvmguard.data.vmdata.TelemetryType
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
