package dev.jvmguard.connector.server.mock

import dev.jvmguard.agent.config.telemetry.TelemetryUnit
import dev.jvmguard.data.vmdata.*
import java.util.*
import kotlin.math.sin

class MockTelemetries(private val serverConnection: MockServerConnectionImpl) {

    private val telemetryDataHolders = HashMap<String, TelemetryDataHolder>()

    fun getTelemetryData(mainId: String, startTime: Long, endTime: Long): TelemetryData {
        val telemetryDataHolder = getTelemetryDataHolder(mainId)

        val timestamps = telemetryDataHolder.timestamps
        val (startIndex, endIndex) = sliceRange(timestamps, startTime, endTime)

        val telemetryData = TelemetryData()
        val rootNode = TelemetryNode("Example data", true)
        rootNode.setTelemetryUnit(TelemetryUnit.PLAIN, 0)
        telemetryData.rootNode = rootNode
        val feedOne = rootNode.addData(SUB_ID_FEED_1, "one")
        val feedTwo = rootNode.addData(SUB_ID_FEED_2, "two")
        feedOne.data = convertLongList(telemetryDataHolder.dataOne.subList(startIndex, endIndex))
        feedTwo.data = convertLongList(telemetryDataHolder.dataTwo.subList(startIndex, endIndex))
        telemetryData.timestamps = convertLongList(timestamps.subList(startIndex, endIndex))
        return telemetryData
    }

    // Custom telemetry: a single (non-stacked) line series named after the node.
    fun getCustomTelemetryData(node: CustomTelemetryNodeIdentifier, startTime: Long, endTime: Long): TelemetryData {
        val telemetryDataHolder = getTelemetryDataHolder("custom:${node.name}")
        val timestamps = telemetryDataHolder.timestamps
        val (startIndex, endIndex) = sliceRange(timestamps, startTime, endTime)

        val telemetryData = TelemetryData()
        val rootNode = TelemetryNode(node.name, false)
        rootNode.setTelemetryUnit(TelemetryUnit.PLAIN, 0)
        telemetryData.rootNode = rootNode
        rootNode.addData(node.name, "v")
            .data = convertLongList(telemetryDataHolder.dataOne.subList(startIndex, endIndex))
        telemetryData.timestamps = convertLongList(timestamps.subList(startIndex, endIndex))
        return telemetryData
    }

    private fun sliceRange(timestamps: List<Long>, startTime: Long, endTime: Long): IntArray {
        if (hasIntersection(timestamps, startTime, endTime)) {
            return intArrayOf(
                maxOf(0, getIndex(timestamps, startTime) - 1),
                minOf(timestamps.size - 1, getIndex(timestamps, endTime) + 1),
            )
        }
        return intArrayOf(0, 0)
    }

    private fun hasIntersection(timestamps: List<Long>, startTime: Long, endTime: Long): Boolean {
        if (timestamps.isEmpty()) {
            return false
        }
        val firstTimestamp = timestamps.first()
        val lastTimestamp = timestamps.last()
        return startTime < lastTimestamp && endTime > firstTimestamp
    }

    private fun getTelemetryDataHolder(mainId: String): TelemetryDataHolder =
        telemetryDataHolders.getOrPut(mainId) {
            val currentTime = serverConnection.currentTime
            val random = serverConnection.getRandom()
            val timestamps = ArrayList<Long>()
            val dataOne = ArrayList<Long>()
            val dataTwo = ArrayList<Long>()
            var time = currentTime - TimeHelper.MILLISECONDS_TO_HOUR * TimeHelper.HOURS_IN_WEEK
            while (time < currentTime) {
                timestamps.add(time)
                dataOne.add(createDataPointDemoData(mainId, SUB_ID_FEED_1, time, random))
                dataTwo.add(createDataPointDemoData(mainId, SUB_ID_FEED_2, time, random))
                val deltaToNow = currentTime - time
                time += when {
                    deltaToNow > TimeHelper.MILLISECONDS_TO_HOUR * TimeHelper.HOURS_IN_DAY -> TimeHelper.MILLISECONDS_TO_HOUR
                    deltaToNow > TimeHelper.MILLISECONDS_TO_HOUR -> 30 * TimeHelper.MILLISECONDS_TO_MINUTE
                    else -> TimeHelper.MILLISECONDS_TO_MINUTE
                }
            }
            TelemetryDataHolder(timestamps, dataOne, dataTwo)
        }

    private fun getIndex(timestamps: List<Long>, targetTime: Long): Int {
        for (i in timestamps.indices) {
            if (timestamps[i] >= targetTime) {
                return i
            }
        }
        return timestamps.size
    }

    private fun convertLongList(list: List<Long>): LongArray = list.toLongArray()

    private fun createDataPointDemoData(mainId: String, subId: String, time: Long, random: Random): Long =
        (getAmplitude(
            mainId,
            subId
        ) * (1f + (0.1f - 1f * random.nextInt(100) / 500) + 0.3 * sin((1f * time / (3 * TimeHelper.MILLISECONDS_TO_HOUR)).toDouble()))).toLong()

    private fun getAmplitude(mainId: String, subId: String): Double =
        if (mainId == Telemetry.HEAP.mainId) {
            if (subId == "one") 100000.0 else 300000.0
        } else {
            if (subId == "one") 30.0 else 60.0
        }

    private class TelemetryDataHolder(
        val timestamps: List<Long>,
        val dataOne: List<Long>,
        val dataTwo: List<Long>,
    )

    companion object {
        private const val SUB_ID_FEED_1 = "Feed 1"
        private const val SUB_ID_FEED_2 = "Feed 2"
    }
}
