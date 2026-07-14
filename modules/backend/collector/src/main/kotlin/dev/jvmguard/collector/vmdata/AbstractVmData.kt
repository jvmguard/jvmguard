package dev.jvmguard.collector.vmdata

import dev.jvmguard.collector.main.CollectorContext
import dev.jvmguard.collector.telemetry.TelemetryDataInterval
import dev.jvmguard.collector.telemetry.TelemetryManager
import dev.jvmguard.collector.threshold.ThresholdHandler
import dev.jvmguard.collector.threshold.ViolationEvent
import dev.jvmguard.collector.vmdata.structures.TelemetryCollection
import dev.jvmguard.collector.vmdata.structures.TelemetryCollection.CollectionType
import dev.jvmguard.data.config.FrequencyUnit
import dev.jvmguard.data.config.thresholds.Threshold
import dev.jvmguard.data.vmdata.*
import java.util.*

abstract class AbstractVmData(
    protected val collectorContext: CollectorContext,
    val parent: VmGroupData?,
    val vm: VM,
) {
    protected val telemetries: MutableMap<TelemetryIdentifier, TelemetryCollection> = HashMap()
    protected var telemetryListId: Int? = null
    private var sortedTelemetries: MutableList<TelemetryIdentifier> = ArrayList()

    private val idToThresholdHandler: MutableMap<TelemetryIdentifier, ThresholdHandler> = HashMap()

    abstract fun getVmDataHolder(
        vmFilter: VmFilter,
        sparkLineRange: SparkLineRange,
        telemetryTypes: Collection<TelemetryType>,
        nanoTime: Long,
        timeStamp: Long,
        frequencyUnit: FrequencyUnit,
    ): VmDataHolder?

    protected fun commitOwnData(connectedValue: Int, nanoTime: Long, recordingTime: Long, recordingInterval: TelemetryDataInterval, checkThresholds: Boolean) {
        synchronized(telemetries) {
            if (connectedValue > 0 && recordingInterval == TelemetryDataInterval.getRecordingInterval(CONNECTIONS_IDENTIFIER.mainId)) {
                val collector = getOrCreateCollector(CONNECTIONS_IDENTIFIER, CollectionType.BOTH)
                collector.addValue(connectedValue.toLong(), false)
            }

            for ((identifier, collection) in telemetries) {
                if (TelemetryDataInterval.getRecordingInterval(identifier.mainId) == recordingInterval) {
                    val value = collection.commitValues(nanoTime)
                    if (checkThresholds) {
                        checkThreshold(nanoTime, recordingTime, value, identifier)
                    }
                }
            }
        }
    }

    protected open fun addViolation(snapshotTimeStamp: Long, nanoTime: Long, violationEvent: ViolationEvent) {
    }

    protected fun freeTelemetryData() {
        synchronized(telemetries) {
            telemetries.clear()
            telemetryListId = null
            sortedTelemetries.clear()
        }
    }

    fun setThresholds(thresholds: List<Threshold>) {
        val newIdToThresholdHandler = HashMap<TelemetryIdentifier, ThresholdHandler>()
        for (threshold in thresholds) {
            val telemetryIdentifier = threshold.telemetryIdentifier
            if (threshold.isEnabled && telemetryIdentifier != null) {
                val lastTimeTriggeredNanos = Long.MIN_VALUE
                val thresholdHandler = newIdToThresholdHandler[telemetryIdentifier]
                if (thresholdHandler == null) {
                    val telemetryType = collectorContext.telemetryManager.getOrCreateTelemetryType(telemetryIdentifier)!!
                    newIdToThresholdHandler[telemetryIdentifier] = ThresholdHandler(threshold, telemetryType, lastTimeTriggeredNanos)
                } else {
                    thresholdHandler.addThreshold(threshold, lastTimeTriggeredNanos)
                }
            }
        }
        synchronized(idToThresholdHandler) {
            idToThresholdHandler.clear()
            idToThresholdHandler.putAll(newIdToThresholdHandler)
        }
    }

    open fun addTelemetryData(
        nanoTime: Long,
        recordingTime: Long,
        telemetryIdentifier: TelemetryIdentifier,
        value: Long,
        addToParent: Boolean,
        groupAveraged: Boolean,
        collectionType: CollectionType
    ) {
        synchronized(telemetries) {
            getOrCreateCollector(telemetryIdentifier, collectionType).addValue(value, groupAveraged)
        }
        if (addToParent && parent != null) {
            parent.addTelemetryData(nanoTime, recordingTime, telemetryIdentifier, value, true, groupAveraged, collectionType)
        }
    }

    protected fun checkThreshold(setNanoTime: Long, recordingTime: Long, value: Long, telemetryIdentifier: TelemetryIdentifier) {
        val thresholdHandler: ThresholdHandler?
        synchronized(idToThresholdHandler) {
            thresholdHandler = idToThresholdHandler[telemetryIdentifier]
        }
        if (thresholdHandler != null) {
            val violationEvents = thresholdHandler.check(setNanoTime, value)
            if (violationEvents != null) {
                for (violationEvent in violationEvents) {
                    addViolation(recordingTime, setNanoTime, violationEvent)
                }
            }
        }
    }

    fun getOrCreateCollector(telemetryIdentifier: TelemetryIdentifier, collectionType: CollectionType): TelemetryCollection {
        var collector = telemetries[telemetryIdentifier]
        if (collector == null) {
            val recordingInterval = TelemetryDataInterval.getRecordingInterval(telemetryIdentifier.mainId)
            collector = TelemetryCollection(recordingInterval, collectionType)
            telemetries[telemetryIdentifier] = collector
            if (collectionType != CollectionType.SPARKLINE_ONLY) {
                telemetryListId = null
            }
        }
        return collector
    }

    open fun visitTelemetryData(visitor: TelemetryDataVisitor, dataInterval: TelemetryDataInterval): Boolean {
        synchronized(telemetries) {
            if (telemetryListId == null) {
                sortedTelemetries = ArrayList()
                for ((identifier, collection) in telemetries) {
                    if (collection.recordedData != null) {
                        sortedTelemetries.add(identifier)
                    }
                }
                sortedTelemetries.sort()
                telemetryListId = visitor.initListId(sortedTelemetries)
            }
            if (telemetryListId != null) {
                val values = LongArray(sortedTelemetries.size)
                var index = 0
                for (telemetryIdentifier in sortedTelemetries) {
                    val collector = telemetries[telemetryIdentifier]
                    values[index++] = if (collector == null) {
                        Long.MIN_VALUE
                    } else {
                        collector.recordedData!!.getAggregatedValue(dataInterval.seconds / TelemetryDataInterval.getRecordingInterval(telemetryIdentifier.mainId).seconds)
                    }
                }
                visitor.visit(telemetryListId, vm, values)
                return true
            }
        }
        return false
    }

    fun fillTelemetry(mainId: String, subIdToData: MutableMap<String, LongArray>, displayedPoints: Int, skippedPoints: Int): Boolean {
        val nanoTime = System.nanoTime()
        var previousData = false
        synchronized(telemetries) {
            for ((identifier, telemetryCollection) in telemetries) {
                if (identifier.mainId == mainId && telemetryCollection.recordedData != null) {
                    val values = LongArray(displayedPoints)
                    Arrays.fill(values, Long.MIN_VALUE)

                    val telemetryList = telemetryCollection.recordedData!!

                    var usedDisplayPoints = displayedPoints
                    if (telemetryCollection.recordingInterval == TelemetryManager.TRANSACTION_RECORDING_INTERVAL) {
                        val difference = ((nanoTime - telemetryCollection.lastCommitTime) / 1000 / 1000 / telemetryCollection.recordingInterval.millis).toInt()
                        if (difference > 1) {
                            usedDisplayPoints -= difference - 1
                        }
                    }

                    if (telemetryList.fillCount > skippedPoints) {
                        previousData = true
                    }

                    val startIndex = telemetryList.size - skippedPoints - usedDisplayPoints
                    var destinationIndex = 0
                    var sourceIndex = startIndex
                    while (sourceIndex < startIndex + usedDisplayPoints) {
                        if (sourceIndex >= 0) {
                            values[destinationIndex] = telemetryList.getLong(sourceIndex)
                        }
                        destinationIndex++
                        sourceIndex++
                    }

                    subIdToData[identifier.subId] = values
                }
            }
        }
        return previousData
    }

    protected fun addSparklineData(
        sparkLineRange: SparkLineRange,
        telemetryTypes: Collection<TelemetryType>,
        nanoTime: Long,
        vmDataHolder: VmDataHolder,
        frequencyUnit: FrequencyUnit
    ) {
        synchronized(telemetries) {
            for (telemetryType in telemetryTypes) {
                val telemetryCollector = telemetries[telemetryType.telemetryIdentifier]
                val sparkLineData = if (telemetryCollector != null) {
                    val sparklineUnscaledData = telemetryCollector.getSparklineUnscaledData(sparkLineRange, nanoTime)!!
                    SparkLineData(
                        telemetryType,
                        frequencyUnit,
                        sparkLineRange,
                        sparklineUnscaledData.data,
                        sparklineUnscaledData.min,
                        sparklineUnscaledData.max
                    )
                } else {
                    SparkLineData(telemetryType, frequencyUnit, sparkLineRange)
                }
                vmDataHolder.addSparkLineData(telemetryType, sparkLineData)
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        return vm == (other as AbstractVmData).vm
    }

    override fun hashCode(): Int {
        return vm.hashCode()
    }

    interface TelemetryDataVisitor {
        fun initListId(telemetryIdentifiers: List<TelemetryIdentifier>): Int?
        fun visit(listId: Int?, vm: VM, values: LongArray)
    }

    interface TelemetryValueVisitor {
        fun visit(vm: VM, value: Long)
    }

    companion object {
        private val CONNECTIONS_IDENTIFIER = TelemetryIdentifier(Telemetry.CONNECTIONS.mainId, "")
    }
}
