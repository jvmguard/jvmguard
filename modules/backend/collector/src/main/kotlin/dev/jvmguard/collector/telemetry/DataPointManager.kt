package dev.jvmguard.collector.telemetry

import dev.jvmguard.collector.main.VmRegistry
import dev.jvmguard.collector.telemetry.TelemetryStorage.DataVisitor
import dev.jvmguard.collector.telemetry.TelemetryStorage.TelemetryWriteTask
import dev.jvmguard.collector.util.CurrentConnectionEntry
import dev.jvmguard.common.DatabaseWriter
import dev.jvmguard.common.Loggers
import dev.jvmguard.common.helper.Timestamp
import dev.jvmguard.data.vmdata.TelemetryIdentifier
import dev.jvmguard.data.vmdata.TelemetryInterval
import dev.jvmguard.data.vmdata.VM
import org.springframework.stereotype.Component
import java.sql.SQLException
import java.util.*
import javax.sql.DataSource

@Component
class DataPointManager(
    private val vmRegistry: VmRegistry,
    private val collectionTypeResolver: CollectionTypeResolver,
    private val telemetryStorage: TelemetryStorage,
    private val databaseWriter: DatabaseWriter,
    private val dataSource: DataSource,
) {
    @Volatile
    private var lastTimestamp: Timestamp? = null

    @Volatile
    private var lastSetNanoTime: Long = 0

    fun addData(connectionEntry: CurrentConnectionEntry, mainId: String, subId: String, groupAveraged: Boolean, value: Long) {
        val lastTimestamp = this.lastTimestamp ?: return
        val telemetryIdentifier = TelemetryIdentifier(mainId, subId)
        connectionEntry.vmData.addTelemetryData(
            lastSetNanoTime,
            lastTimestamp.time,
            telemetryIdentifier,
            value,
            true,
            groupAveraged,
            collectionTypeResolver.getCollectionType(telemetryIdentifier)
        )
    }

    fun startSet(timestamp: Timestamp, setNanoTime: Long, recordingInterval: TelemetryDataInterval) {
        val lastTimestamp = this.lastTimestamp
        if (lastTimestamp != null) {
            vmRegistry.rootVmGroupData.commitTelemetryData(setNanoTime, timestamp.time, recordingInterval)

            val orderedTasks = ArrayList<TelemetryWriteTask>()

            val databaseIntervals = TelemetryDataInterval.getDatabaseIntervals()
            for (i in databaseIntervals.indices) {
                val databaseInterval = databaseIntervals[i]

                val storageTime = timestamp.floor(databaseInterval.timestampInterval)
                if (storageTime != lastTimestamp.floor(databaseInterval.timestampInterval)) {
                    if (i == 0) {
                        orderedTasks.add(telemetryStorage.createInitialDataTask(storageTime))
                    } else {
                        val previousInterval = databaseIntervals[i - 1]
                        orderedTasks.add(telemetryStorage.createConsolidationTask(databaseInterval, previousInterval, storageTime))
                    }
                }
            }

            if (orderedTasks.isNotEmpty()) {
                databaseWriter.executeInWriter { runTasks(orderedTasks) }
            }
        }
        this.lastTimestamp = timestamp
        lastSetNanoTime = setNanoTime
    }

    private fun runTasks(tasks: List<TelemetryWriteTask>) {
        try {
            dataSource.connection.use { connection ->
                for (task in tasks) {
                    try {
                        task.run(connection)
                    } catch (e: Exception) {
                        SERVER_LOGGER.error("error storing telemetry data", e)
                    }
                }
            }
        } catch (e: SQLException) {
            SERVER_LOGGER.error("error storing telemetry data", e)
        }
    }

    fun getTelemetryData(vm: VM?, mainId: String, displayInterval: TelemetryInterval, endTime: Long): PlainTelemetryData {
        val dataInterval = TelemetryDataInterval.fromDisplayInterval(displayInterval, mainId)
        val flooredEndTime = Timestamp.floor(endTime, dataInterval.timestampInterval)

        val telemetryData = if (dataInterval == TelemetryDataInterval.getRecordingInterval(mainId)) {
            getRecordedData(vm, mainId, displayInterval, dataInterval, flooredEndTime)
        } else {
            getDatabaseData(vm, mainId, displayInterval, dataInterval, flooredEndTime)
        }
        if (telemetryData.timeStamps == null) {
            telemetryData.timeStamps = LongArray(0)
        }
        telemetryData.dataInterval = dataInterval.timestampInterval

        return telemetryData
    }

    private fun getRecordedData(
        vm: VM?,
        mainId: String,
        displayInterval: TelemetryInterval,
        dataInterval: TelemetryDataInterval,
        endTime: Long
    ): PlainTelemetryData {
        val telemetryData = PlainTelemetryData()
        val currentTime = Timestamp.floor(System.currentTimeMillis(), dataInterval.timestampInterval)
        val skippedPoints = ((currentTime - endTime) / dataInterval.millis).toInt()
        if (skippedPoints < 0 || skippedPoints >= dataInterval.storedPoints) {
            telemetryData.isNoPreviousData = true
        } else {
            val vmData = vmRegistry.rootVmGroupData.getVmData(vm)
            if (vmData == null) {
                telemetryData.isNoPreviousData = true
            } else {
                val displayedPoints = dataInterval.getDisplayedPoints(displayInterval)
                val previousData = vmData.fillTelemetry(mainId, telemetryData.subIdToData, displayedPoints, skippedPoints)
                telemetryData.isNoPreviousData = !previousData
                if (previousData) {
                    val timestamps = LongArray(displayedPoints)
                    val startTime = currentTime - skippedPoints * dataInterval.millis - displayInterval.timeExtent
                    Arrays.setAll(timestamps) { i -> startTime + i * dataInterval.millis + dataInterval.millis / 2 }
                    telemetryData.timeStamps = timestamps
                }
            }
        }
        return telemetryData
    }

    private fun getDatabaseData(
        vm: VM?,
        mainId: String,
        displayInterval: TelemetryInterval,
        dataInterval: TelemetryDataInterval,
        endTime: Long
    ): PlainTelemetryData {
        val telemetryData = PlainTelemetryData()
        val timeExtent = displayInterval.timeExtent

        telemetryStorage.visitData(
            dataInterval,
            vm,
            endTime - timeExtent,
            endTime,
            DataBaseVisitor(mainId, dataInterval, displayInterval, endTime - timeExtent, telemetryData.subIdToData)
        )

        if (telemetryData.subIdToData.isEmpty()) {
            if (!telemetryStorage.hasData(dataInterval, vm, endTime - timeExtent)) {
                telemetryData.isNoPreviousData = true
            }
        }

        if (!telemetryData.isNoPreviousData) {
            val timestamps = LongArray(dataInterval.getDisplayedPoints(displayInterval))
            Arrays.setAll(timestamps) { i -> endTime - timeExtent + i * dataInterval.millis + dataInterval.millis / 2 }
            telemetryData.timeStamps = timestamps
        }

        return telemetryData
    }

    private class DataBaseVisitor(
        private val mainId: String,
        private val dataInterval: TelemetryDataInterval,
        private val displayInterval: TelemetryInterval,
        private val startTime: Long,
        private val subIdToData: MutableMap<String, LongArray>,
    ) : DataVisitor {
        private var identifiers: List<TelemetryIdentifier>? = null

        override fun visitVmId(vmId: Long) {
        }

        override fun visitTelemetryIds(identifiers: List<TelemetryIdentifier>?) {
            this.identifiers = identifiers
        }

        override fun visitValues(timeStamp: Long, values: LongArray) {
            val identifiers = this.identifiers
            if (identifiers != null && identifiers.size == values.size) { // should always be the case
                val destinationIndex = ((timeStamp - startTime) / dataInterval.millis).toInt()
                for (valueIndex in values.indices) {
                    val identifier = identifiers[valueIndex]
                    if (identifier.mainId == mainId) {
                        val subId = identifier.subId
                        var destinationValues = subIdToData[subId]
                        if (destinationValues == null) {
                            destinationValues = LongArray(dataInterval.getDisplayedPoints(displayInterval))
                            Arrays.fill(destinationValues, Long.MIN_VALUE)
                            subIdToData[subId] = destinationValues
                        }
                        destinationValues[destinationIndex] = values[valueIndex]
                    }
                }
            }
        }

        override fun endVisit() {
        }
    }

    companion object {
        private val SERVER_LOGGER = Loggers.SERVER
    }
}
