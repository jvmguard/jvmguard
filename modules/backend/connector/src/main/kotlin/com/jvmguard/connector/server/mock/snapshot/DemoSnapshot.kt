@file:Suppress("unused")

package com.jvmguard.connector.server.mock.snapshot

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * Serializable snapshot of the data that the web UI queries, captured from a live demo server and replayed by
 * [com.jvmguard.connector.server.mock.SnapshotReplayConnection]. All timestamps are absolute epoch millis as
 * captured; [captureAnchor] is the wall-clock time at capture.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class DemoSnapshot {
    var schemaVersion: Int = SCHEMA_VERSION
    var captureAnchor: Long = 0L

    var vms: MutableList<VmDto> = mutableListOf()
    var telemetryTypes: MutableList<TelemetryTypeDto> = mutableListOf()
    var customTelemetryNodes: MutableList<CustomTelemetryNodeDto> = mutableListOf()

    var sparklines: MutableMap<Long, MutableMap<String, SparkLineHolderDto>> = mutableMapOf()

    var telemetry: MutableMap<Long, MutableMap<String, MutableMap<String, TelemetrySeriesDto>>> = mutableMapOf()

    var customTelemetry: MutableMap<Long, MutableMap<String, MutableMap<String, TelemetrySeriesDto>>> = mutableMapOf()

    var transactions: MutableMap<Long, MutableMap<String, MutableList<TransactionBucketDto>>> = mutableMapOf()

    var inbox: MutableList<InboxItemDto> = mutableListOf()
    var snapshotFiles: MutableList<SnapshotFileDto> = mutableListOf()

    companion object {
        const val SCHEMA_VERSION: Int = 2
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class VmDto {
    var id: Long = 0L
    var type: String = ""
    var rawName: String = ""
    var groupName: String = ""
    var hostName: String = ""
    var port: Int = 0
    var instanceId: Long = 0L

    constructor()
    constructor(id: Long, type: String, rawName: String, groupName: String, hostName: String, port: Int, instanceId: Long) {
        this.id = id; this.type = type; this.rawName = rawName; this.groupName = groupName
        this.hostName = hostName; this.port = port; this.instanceId = instanceId
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class TelemetryTypeDto {
    var mainId: String = ""
    var subId: String? = null
    var additionalType: Int = 0
    var additionalName: String = ""
    var categoryName: String = ""
    var name: String = ""
    var unit: String = ""
    var scale: Int = 0

    constructor()
    constructor(
        mainId: String, subId: String?, additionalType: Int, additionalName: String,
        categoryName: String, name: String, unit: String, scale: Int
    ) {
        this.mainId = mainId; this.subId = subId; this.additionalType = additionalType
        this.additionalName = additionalName; this.categoryName = categoryName; this.name = name
        this.unit = unit; this.scale = scale
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class CustomTelemetryNodeDto {
    var type: String = ""
    var name: String = ""

    constructor()
    constructor(type: String, name: String) {
        this.type = type; this.name = name
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class SparkLineHolderDto {
    var isConnected: Boolean = false
    var isOutdatedAgent: Boolean = false
    var statusChangeTime: Long = 0L
    var hostName: String = ""
    var port: Int = 0
    var frequencyUnit: String = ""
    var series: MutableMap<String, SparkLineSeriesDto> = mutableMapOf()

    constructor()
}

@JsonIgnoreProperties(ignoreUnknown = true)
class SparkLineSeriesDto {
    var values: LongArray = LongArray(0)
    var min: Long = 0L
    var max: Long = 0L

    constructor()
    constructor(values: LongArray, min: Long, max: Long) {
        this.values = values; this.min = min; this.max = max
    }

    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = System.identityHashCode(this)
}

@JsonIgnoreProperties(ignoreUnknown = true)
class TelemetrySeriesDto {
    var timestamps: LongArray = LongArray(0)
    var root: TelemetryNodeDto = TelemetryNodeDto()

    constructor()
}

/** A node in the telemetry tree. The chart selects one node and draws its [feeds]; container nodes (e.g. "Heap")
 *  hold their series in [children], so the whole tree must be captured, not just the root's direct feeds. */
@JsonIgnoreProperties(ignoreUnknown = true)
class TelemetryNodeDto {
    var description: String = ""
    var stacked: Boolean = false
    var unit: String = ""
    var scale: Int = 0
    var feeds: MutableList<TelemetryFeedDto> = mutableListOf()
    var children: MutableList<TelemetryNodeDto> = mutableListOf()

    constructor()
}

@JsonIgnoreProperties(ignoreUnknown = true)
class TelemetryFeedDto {
    var description: String = ""
    var subId: String = ""
    var data: LongArray = LongArray(0)

    constructor()
    constructor(description: String, subId: String, data: LongArray) {
        this.description = description; this.subId = subId; this.data = data
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class TransactionBucketDto {
    var startTime: Long = 0L
    var interval: String = ""
    var latest: Boolean = false
    var available: Boolean = false
    var minIntervalPercentage: Int = 0
    var maxIntervalPercentage: Int = 0
    var callTree: TransactionTreeDto? = null
    var hotspots: TransactionTreeDto? = null

    constructor()
}

@JsonIgnoreProperties(ignoreUnknown = true)
class TransactionTreeDto {
    var name: String? = null
    var transactionType: String = ""
    var policyTypeString: String = ""
    var time: Long = 0L
    var count: Long = 0L
    var children: MutableList<TransactionTreeDto> = mutableListOf()

    constructor()
}

@JsonIgnoreProperties(ignoreUnknown = true)
class InboxItemDto {
    var id: Long = 0L
    var millis: Long = 0L
    var snapshotFileId: Long? = null
    var snapshotFileType: String? = null
    var vmId: Long = 0L
    var name: String = ""
    var message: String = ""
    var itemRead: Boolean = false

    constructor()
}

@JsonIgnoreProperties(ignoreUnknown = true)
class SnapshotFileDto {
    var id: Long = 0L
    var vmId: Long = 0L
    var type: String = ""
    var snapshotTime: Long = 0L
    var name: String = ""
    var uncompressedLength: Long = -1L

    constructor()
}
