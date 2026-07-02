package com.jvmguard.collector.vmdata.startup

import com.jvmguard.agent.util.Util
import com.jvmguard.collector.telemetry.CollectionTypeResolver
import com.jvmguard.collector.telemetry.TelemetryDataInterval
import com.jvmguard.collector.telemetry.TelemetryManager
import com.jvmguard.collector.telemetry.TelemetryManager.SummarizedTelemetryInfo
import com.jvmguard.collector.telemetry.TelemetryStorage
import com.jvmguard.collector.telemetry.TelemetryStorage.DataVisitor
import com.jvmguard.collector.vmdata.AbstractVmData
import com.jvmguard.collector.vmdata.VmData
import com.jvmguard.collector.vmdata.structures.TelemetryCollection.Companion.DAY_INTERVAL
import com.jvmguard.collector.vmdata.structures.TelemetryCollection.Companion.HOUR_INTERVAL
import com.jvmguard.common.helper.Timestamp
import com.jvmguard.data.base.Interval
import com.jvmguard.data.vmdata.TelemetryIdentifier
import org.springframework.stereotype.Component
import java.util.*

@Component
class SparklineBuilder(
    private val telemetryStorage: TelemetryStorage,
    private val telemetryManager: TelemetryManager,
    private val collectionTypeResolver: CollectionTypeResolver,
) {

    fun addSparklines(vmDataSet: Map<Long, AbstractVmData>, currentMillis: Long, currentNanos: Long) {
        val dataInterval = TelemetryDataInterval.TWO_MINUTES
        // at floor time, the previous interval was created, so this is the last possible one
        val endTime = Timestamp(currentMillis).floor(dataInterval.timestampInterval) - dataInterval.millis

        val dataMap = HashMap<TelemetryIdentifier, CurrentSparklineData>()
        val summarizerMap = HashMap<String, TelemetrySummarizer>()
        for (summarizedInfo in telemetryManager.summarizedSparklines) {
            summarizerMap[summarizedInfo.mainId] = TelemetrySummarizer(summarizedInfo, endTime)
        }

        telemetryStorage.visitData(dataInterval, null, endTime - Interval.DAY.millis, endTime + 1, object : DataVisitor {
            private var identifiers: List<TelemetryIdentifier>? = null
            private var vmData: AbstractVmData? = null
            private var maxTime: Long = 0

            override fun visitVmId(vmId: Long) {
                if (vmData != null) {
                    endVisit()
                }
                vmData = vmDataSet[vmId]
                dataMap.clear()
                maxTime = 0
            }

            override fun visitTelemetryIds(identifiers: List<TelemetryIdentifier>?) {
                this.identifiers = identifiers
            }

            override fun visitValues(timeStamp: Long, values: LongArray) {
                val identifiers = this.identifiers
                if (vmData != null && identifiers != null && identifiers.size == values.size) {
                    for (valueIndex in values.indices) {
                        val identifier = identifiers[valueIndex]
                        val subId = identifier.subId
                        addValue(dataMap, endTime, timeStamp, values[valueIndex], TelemetryIdentifier(identifier.mainId, subId))

                        val summarizer = summarizerMap[identifier.mainId]
                        summarizer?.add(subId, values[valueIndex])
                    }
                    for (summarizer in summarizerMap.values) {
                        summarizer.commit(dataMap, timeStamp)
                    }
                    maxTime = maxOf(timeStamp, maxTime)
                }
            }

            override fun endVisit() {
                val vmData = this.vmData ?: return
                if (vmData is VmData) {
                    vmData.lastStateNanoTime = maxOf(maxTime, vmData.lastStateNanoTime)
                }
                for ((identifier, sparklineSource) in dataMap) {
                    val sparklineData = sparklineSource.finish()
                    val telemetryType = telemetryManager.getTelemetryType(identifier.combinedId)
                    if (telemetryType != null) {
                        val telemetryCollection = vmData.getOrCreateCollector(identifier, collectionTypeResolver.getCollectionType(identifier))
                        // telemetry is valid for the complete data interval
                        val updateNanos = currentNanos - (currentMillis - endTime - dataInterval.millis) * 1000 * 1000
                        telemetryCollection.initSparklines(sparklineData.day, sparklineData.dayMin, sparklineData.dayMax, sparklineData.hour, updateNanos)
                    }
                }
            }
        })
    }

    private class TelemetrySummarizer(private val summarizedTelemetryInfo: SummarizedTelemetryInfo, private val endTime: Long) {
        private var value = Long.MIN_VALUE

        fun add(subId: String, single: Long) {
            if (summarizedTelemetryInfo.isIncluded(subId) && single > Long.MIN_VALUE) {
                if (value == Long.MIN_VALUE) {
                    value = 0
                }
                value += single
            }
        }

        fun commit(dataMap: MutableMap<TelemetryIdentifier, CurrentSparklineData>, timeStamp: Long) {
            addValue(dataMap, endTime, timeStamp, value, TelemetryIdentifier(summarizedTelemetryInfo.mainId, summarizedTelemetryInfo.summarizedId))
            value = Long.MIN_VALUE
        }
    }

    private class CurrentSparklineData {
        val day = LongArray((Interval.DAY.millis / DAY_INTERVAL.millis).toInt())
        val dayMin = LongArray(day.size)
        val dayMax = LongArray(day.size)
        private val dayCount = IntArray(day.size)

        val hour = LongArray((Interval.HOUR.millis / HOUR_INTERVAL.millis).toInt())

        init {
            Arrays.fill(dayMin, Long.MAX_VALUE)
            Arrays.fill(dayMax, Long.MIN_VALUE)
            Arrays.fill(hour, Long.MIN_VALUE)
        }

        fun addValue(endTime: Long, timeStamp: Long, value: Long) {
            var index = day.size - 1 - ((endTime - timeStamp) / DAY_INTERVAL.millis).toInt()
            if (index in 0..day.size) {
                dayCount[index]++
                day[index] += value
                dayMin[index] = minOf(dayMin[index], value)
                dayMax[index] = maxOf(dayMax[index], value)
            }
            index = hour.size - 1 - ((endTime - timeStamp) / HOUR_INTERVAL.millis).toInt()
            if (index in 1..hour.size) {
                hour[index] = value
                hour[index - 1] = value
            }
        }

        fun finish(): CurrentSparklineData {
            for (i in day.indices) {
                day[i] = if (dayCount[i] == 0) {
                    Long.MIN_VALUE
                } else {
                    Util.divideAndRoundUpToOne(day[i], dayCount[i].toLong())
                }
            }
            return this
        }
    }

    companion object {
        private fun addValue(
            dataMap: MutableMap<TelemetryIdentifier, CurrentSparklineData>,
            endTime: Long,
            timeStamp: Long,
            value: Long,
            identifier: TelemetryIdentifier
        ) {
            if (value > Long.MIN_VALUE) {
                val data = dataMap.computeIfAbsent(identifier) { _ -> CurrentSparklineData() }
                data.addValue(endTime, timeStamp, value)
            }
        }
    }
}
