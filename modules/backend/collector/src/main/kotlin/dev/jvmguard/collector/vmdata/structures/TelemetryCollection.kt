package dev.jvmguard.collector.vmdata.structures

import dev.jvmguard.collector.telemetry.TelemetryDataInterval
import dev.jvmguard.collector.telemetry.TelemetryManager
import dev.jvmguard.collector.vmdata.structures.SparklineList.CurrentData
import dev.jvmguard.data.base.Interval
import dev.jvmguard.data.vmdata.SparkLineRange

class TelemetryCollection(val recordingInterval: TelemetryDataInterval, collectionType: CollectionType) {

    var recordedData: TelemetryList? = null
        private set
    private var hourSparkline: SparklineList? = null
    private var daySparkline: TimeAggregatingSparklineList? = null

    private var currentSum: Long = 0
    private var count: Int = 0
    var lastCommitTime: Long = 0
        private set
    private var averaged = false

    init {
        if (collectionType != CollectionType.SPARKLINE_ONLY) {
            recordedData = TelemetryList(recordingInterval.storedPoints)
        }
        if (collectionType != CollectionType.TELEMETRY_ONLY) {
            hourSparkline = if (recordingInterval == TelemetryManager.TRANSACTION_RECORDING_INTERVAL) {
                NonAggregatingSparklineList((Interval.HOUR.millis / HOUR_INTERVAL.millis).toInt(), HOUR_INTERVAL)
            } else {
                TimeAggregatingSparklineList((Interval.HOUR.millis / HOUR_INTERVAL.millis).toInt(), HOUR_INTERVAL)
            }
            daySparkline = TimeAggregatingSparklineList((Interval.DAY.millis / DAY_INTERVAL.millis).toInt(), DAY_INTERVAL)
        }
    }

    fun addValue(value: Long, averaged: Boolean) {
        this.averaged = averaged
        recordedData?.setNextValue(value)
        currentSum += value
        count++
    }

    fun commitValues(nanoTime: Long): Long {
        val committedValue: Long
        val recordedData = this.recordedData
        if (recordedData != null) {
            val millis = if (recordingInterval != TelemetryManager.TRANSACTION_RECORDING_INTERVAL || lastCommitTime == 0L) {
                recordingInterval.millis
            } else {
                (nanoTime - lastCommitTime) / 1000 / 1000
            }
            lastCommitTime = nanoTime
            if (millis > 1.8 * recordingInterval.millis) {
                addSparklineValue(nanoTime, recordedData.commitValue(averaged))
            }
            committedValue = recordedData.commitValue(averaged)
            addSparklineValue(nanoTime, committedValue)
        } else {
            committedValue = TelemetryList.getCommittedValue(averaged, currentSum, count)
            addSparklineValue(nanoTime, committedValue)
        }
        currentSum = 0
        count = 0
        return committedValue
    }

    private fun addSparklineValue(nanoTime: Long, committedValue: Long) {
        hourSparkline?.addValue(committedValue, nanoTime)
        daySparkline?.addValue(committedValue, nanoTime)
    }

    fun getSparklineUnscaledData(sparkLineRange: SparkLineRange, nanoTime: Long): CurrentData? {
        return if (sparkLineRange == SparkLineRange.LAST_HOUR) {
            hourSparkline?.getCurrentUnscaledData(nanoTime)
        } else {
            daySparkline?.getCurrentUnscaledData(nanoTime)
        }
    }

    fun initSparklines(dayValues: LongArray, dayMin: LongArray, dayMax: LongArray, hour: LongArray, nanoTime: Long) {
        hourSparkline?.init(hour, nanoTime)
        daySparkline?.init(dayValues, dayMin, dayMax, nanoTime)
    }

    enum class CollectionType {
        SPARKLINE_ONLY,
        TELEMETRY_ONLY,
        BOTH,
    }

    companion object {
        val HOUR_INTERVAL: Interval = Interval.MINUTE
        val DAY_INTERVAL: Interval = Interval.HALF_AN_HOUR
    }
}
