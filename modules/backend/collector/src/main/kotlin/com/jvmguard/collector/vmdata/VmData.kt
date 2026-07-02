package com.jvmguard.collector.vmdata

import com.jvmguard.collector.main.CollectorContext
import com.jvmguard.collector.telemetry.TelemetryDataInterval
import com.jvmguard.collector.threshold.ViolationEvent
import com.jvmguard.collector.vmdata.structures.TelemetryCollection.CollectionType
import com.jvmguard.data.config.FrequencyUnit
import com.jvmguard.data.vmdata.*

class VmData : AbstractVmData {
    var lastStateNanoTime: Long
    private var connected = false
    private var outdatedAgent = false

    var hostName = ""
        private set
    var port = 0
        private set

    constructor(collectorContext: CollectorContext, parent: VmGroupData, vm: VM) : super(collectorContext, parent, vm) {
        lastStateNanoTime = System.nanoTime()
    }

    constructor(collectorContext: CollectorContext, parent: VmGroupData, vm: VM, lastStateNanoTime: Long) : super(collectorContext, parent, vm) {
        this.lastStateNanoTime = lastStateNanoTime
    }

    fun commitTelemetryData(nanoTime: Long, recordingTime: Long, recordingInterval: TelemetryDataInterval): Int {
        val connectedValue = if (isConnected()) 1 else 0
        commitOwnData(connectedValue, nanoTime, recordingTime, recordingInterval, false)
        return connectedValue
    }

    fun clearDisconnected(nanoTime: Long): Boolean {
        // needed for one-day group spark line data and dashboard violations. Could be optimized.
        return !connected && (nanoTime - lastStateNanoTime) / 1000 / 1000 > 1000 * 60 * 60 * 25
    }

    @Synchronized
    fun connect(nanoTime: Long, outdatedAgent: Boolean, hostName: String, port: Int) {
        this.hostName = hostName
        this.port = port
        connected = true
        lastStateNanoTime = nanoTime
        this.outdatedAgent = outdatedAgent
    }

    @Synchronized
    fun disconnect(nanoTime: Long) {
        lastStateNanoTime = nanoTime
        connected = false
        outdatedAgent = false
        parent!!.disconnectVmChild(this)
    }

    @Synchronized
    fun isConnected(): Boolean {
        return connected
    }

    override fun addViolation(snapshotTimeStamp: Long, nanoTime: Long, violationEvent: ViolationEvent) {
        super.addViolation(snapshotTimeStamp, nanoTime, violationEvent)
        parent!!.triggerHandler.addViolation(snapshotTimeStamp, nanoTime, violationEvent, vm, collectorContext)
    }

    override fun addTelemetryData(
        nanoTime: Long,
        recordingTime: Long,
        telemetryIdentifier: TelemetryIdentifier,
        value: Long,
        addToParent: Boolean,
        groupAveraged: Boolean,
        collectionType: CollectionType
    ) {
        super.addTelemetryData(nanoTime, recordingTime, telemetryIdentifier, value, addToParent, groupAveraged, collectionType)
        checkThreshold(nanoTime, recordingTime, value, telemetryIdentifier)
    }

    fun visitConnectedVmTelemetryValue(telemetryIdentifier: TelemetryIdentifier, visitor: TelemetryValueVisitor) {
        val connected = synchronized(this) { this.connected }
        if (connected) {
            if (telemetryIdentifier.mainId == Telemetry.CONNECTIONS.mainId) {
                visitor.visit(vm, 1)
            } else {
                var available = false
                var value: Long = 0
                synchronized(telemetries) {
                    val collector = telemetries[telemetryIdentifier]
                    if (collector != null) {
                        value = collector.recordedData!!.getAggregatedValue(1)
                        available = true
                    }
                }
                if (available) {
                    visitor.visit(vm, value)
                }
            }
        }
    }

    override fun visitTelemetryData(visitor: TelemetryDataVisitor, dataInterval: TelemetryDataInterval): Boolean {
        val connected = synchronized(this) { this.connected }
        return if (connected) {
            super.visitTelemetryData(visitor, dataInterval)
        } else {
            false
        }
    }

    override fun getVmDataHolder(
        vmFilter: VmFilter,
        sparkLineRange: SparkLineRange,
        telemetryTypes: Collection<TelemetryType>,
        nanoTime: Long,
        timeStamp: Long,
        frequencyUnit: FrequencyUnit,
    ): VmDataHolder? {
        var vmDataHolder: VmDataHolder?
        synchronized(this) {
            val elapsedTime = (nanoTime - lastStateNanoTime) / 1000 / 1000
            vmDataHolder = if (!connected && (vmFilter == VmFilter.CONNECTED || elapsedTime > sparkLineRange.rangeInterval.millis)) {
                null
            } else {
                VmDataHolder(vm, connected, outdatedAgent, timeStamp - elapsedTime, sparkLineRange, frequencyUnit, hostName, port)
            }
        }
        val holder = vmDataHolder
        if (holder != null) {
            addSparklineData(sparkLineRange, telemetryTypes, nanoTime, holder, frequencyUnit)
        }
        return holder
    }
}
