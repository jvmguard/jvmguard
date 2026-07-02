package com.jvmguard.integration.tests.jvmguard.telemetry

import com.jvmguard.collector.telemetry.TelemetryDataInterval
import com.jvmguard.data.config.FrequencyUnit
import com.jvmguard.data.vmdata.*
import com.jvmguard.integration.Controller
import com.jvmguard.integration.JvmGuardTest
import com.jvmguard.integration.TestServerConnection
import com.jvmguard.integration.TestVmManager
import com.jvmguard.integration.config.VMConfig
import com.jvmguard.integration.tests.jvmguard.dashboard.toBigDecimal
import com.jvmguard.integration.util.nonNullRootNode
import com.jvmguard.integration.util.nonNullTimestamps
import com.jvmguard.integration.util.nonNullUnitScaledData
import java.math.BigDecimal

class TelemetryDisconnectTest : JvmGuardTest() {
    override fun getJvmGuardOptions(runNo: Int, vmNo: Int, libraryNo: Int) = super.getJvmGuardOptions(runNo, vmNo, libraryNo) + " -Xmx64m"
    override fun getVmCount(vmConfig: VMConfig, runNo: Int) = 2

    override fun connect(vmManager: TestVmManager, serverConnection: TestServerConnection, controller: Controller) {
        val vms = waitForConnections(serverConnection)
        val singleVm = vms.first()

        val interval = 1000L * 4 * 60

        sleep(interval)

        var time = serverConnection.currentTime
        checkConnected(serverConnection, null, time, interval, 0)
        checkConnected(serverConnection, singleVm, time, interval, 0)

        println("terminating")
        terminate(vmManager, singleVm)
        sleep(interval)

        println("check2")
        time = serverConnection.currentTime
        checkConnected(serverConnection, null, time, interval * 2, 0)
        checkConnected(serverConnection, singleVm, time, interval, interval)


        println("terminating")
        terminate(vmManager, vms.first { it.id != singleVm.id })
        sleep(interval)

        println("check3")
        time = serverConnection.currentTime
        checkConnected(serverConnection, null, time, interval * 2, interval)
        checkConnected(serverConnection, singleVm, time, interval, interval * 2)

        println("finished")
    }

    private fun checkConnected(serverConnection: TestServerConnection, vm: VM?, time: Long, interval: Long, emptyInterval: Long) {
        val usedTelemetryInterval = if (interval + emptyInterval > 60 * 1000 * 10) 60L * 1000 * 10 - emptyInterval else interval

        getAndCheckCustomTelemetryData(TelemetryInterval.TEN_MINUTES, "custom1", serverConnection, vm, time).apply {
            val data = telemetryData.nonNullRootNode.calculateUnitScale(FrequencyUnit.PER_MINUTE).data[0]
            checkDataPoints(data.nonNullUnitScaledData, usedTelemetryInterval / recordingMillis, emptyInterval / recordingMillis)
        }

        getAndCheckTelemetryData(TelemetryInterval.TEN_MINUTES, Telemetry.HEAP.mainId, serverConnection, vm, time).apply {
            val childNode = telemetryData.nonNullRootNode.children.first { it.description == "Heap" }
            val data = childNode.calculateUnitScale(FrequencyUnit.PER_MINUTE).data.first { it.description == "Used" }
            checkDataPoints(data.nonNullUnitScaledData, usedTelemetryInterval / recordingMillis, emptyInterval / recordingMillis)
        }

        getAndCheckTelemetryData(TelemetryInterval.TEN_MINUTES, Telemetry.TRANSACTIONS.mainId, serverConnection, vm, time).apply {
            var childNode = telemetryData.nonNullRootNode.children.first { it.description == "Transactions" }
            var data = childNode.calculateUnitScale(FrequencyUnit.PER_MINUTE).data.first { it.description == "Normal" }
            checkDataPoints(data.nonNullUnitScaledData, usedTelemetryInterval / recordingMillis, emptyInterval / recordingMillis)

            childNode = telemetryData.nonNullRootNode.children.find { it.description == "Average Transaction Duration" }!!
            data = childNode.calculateUnitScale(FrequencyUnit.PER_MINUTE).data[0]
            checkDataPoints(data.nonNullUnitScaledData, usedTelemetryInterval / recordingMillis, emptyInterval / recordingMillis)

            val telemetryTypes = getTelemetryTypes(serverConnection)
            val holders = serverConnection.getVmDataHolders(VmFilter.RECENT, SparkLineRange.LAST_HOUR, telemetryTypes)
            val vmDataHolder = if (vm == null) holders.data!! else holders.groupChildren.values.first().vmDataMap[vm]!!
            if (vm != null && emptyInterval > 0) {
                assertFalse(vmDataHolder.isConnected)
            }
            telemetryTypes.forEach { telemetryType ->
                val sparkLineData = vmDataHolder.getSparkLineData(telemetryType)
                if (emptyInterval > 0) {
                    assertEqual(sparkLineData.scaledCurrent.toBigDecimal(), 0.toBigDecimal()) {
                        println(telemetryType)
                        println(data.unitScaledData)
                    }
                } else {
                    assertTrue(sparkLineData.scaledCurrent.toBigDecimal() > 0.toBigDecimal()) {
                        println(sparkLineData.scaledCurrent)
                        println(telemetryType)
                        println(data.unitScaledData)
                    }
                }
                checkDataPoints(data.nonNullUnitScaledData, interval / 1000 / 60, emptyInterval / 1000 / 60)
            }
        }
    }

    private data class CheckResult(val telemetryData: TelemetryData, val recordingMillis: Long, val totalPoints: Int)

    private fun getAndCheckTelemetryData(
        displayInterval: TelemetryInterval,
        mainId: String,
        serverConnection: TestServerConnection,
        vm: VM?,
        time: Long
    ): CheckResult {
        val recordingMillis = getRecordingMillis(displayInterval, mainId).toLong()
        val telemetryData = serverConnection.getTelemetryData(vm, mainId, displayInterval, time)
        checkTelemetryData(telemetryData, time, recordingMillis, displayInterval)
        return CheckResult(telemetryData, recordingMillis, telemetryData.nonNullTimestamps.size)
    }

    private fun getAndCheckCustomTelemetryData(
        displayInterval: TelemetryInterval,
        nodeName: String,
        serverConnection: TestServerConnection,
        vm: VM?,
        time: Long
    ): CheckResult {
        val recordingMillis = getRecordingMillis(displayInterval, Telemetry.CUSTOM.mainId).toLong()
        val customTelemetryNodeIdentifier = serverConnection.customTelemetryNodes.first { it.name == nodeName }
        val telemetryData = serverConnection.getCustomTelemetryData(vm, customTelemetryNodeIdentifier, displayInterval, time)
        checkTelemetryData(telemetryData, time, recordingMillis, displayInterval)
        return CheckResult(telemetryData, recordingMillis, telemetryData.nonNullTimestamps.size)
    }

    private fun checkTelemetryData(telemetryData: TelemetryData, time: Long, recordingMillis: Long, displayInterval: TelemetryInterval) {
        assertTrue(telemetryData.nonNullTimestamps[telemetryData.nonNullTimestamps.size - 1] < time + 5000 && telemetryData.nonNullTimestamps[telemetryData.nonNullTimestamps.size - 1] > time - recordingMillis * 2)
        for (i in 1 until telemetryData.nonNullTimestamps.size) {
            assertEqual(telemetryData.nonNullTimestamps[i], telemetryData.nonNullTimestamps[i - 1] + recordingMillis)
        }
        assertTrue(telemetryData.telemetryInterval == displayInterval)
        assertEqual(telemetryData.nonNullTimestamps.size, displayInterval.timeExtent / recordingMillis)
        assertFalse(telemetryData.isNoPreviousData)
    }

    private fun getRecordingMillis(displayInterval: TelemetryInterval, mainId: String) =
        TelemetryDataInterval.fromDisplayInterval(displayInterval, mainId).millis.toBigDecimal()

    private fun checkDataPoints(data: List<BigDecimal?>, expectedPoints: Long, checkEmpty: Long) {

        var dataPointCount = 0L
        var emptyAtEnd = 0L
        data.forEach {
            if (it != null && it > 0.toBigDecimal()) {
                dataPointCount++
            } else if (dataPointCount > 0) {
                emptyAtEnd++
            }
        }
        println("$checkEmpty == $emptyAtEnd")
        assertTrue(dataPointCount > expectedPoints - 3 && dataPointCount < expectedPoints + 2) {
            println("$dataPointCount != $expectedPoints")
        }
        if (checkEmpty > 0) {
            assertTrue(emptyAtEnd > checkEmpty - 3 && emptyAtEnd < checkEmpty + 2) {
                println("$emptyAtEnd != $checkEmpty")
            }
        } else {
            assertTrue(emptyAtEnd <= 1) {
                println(emptyAtEnd)
            }
        }
    }

    private fun getTelemetryTypes(serverConnection: TestServerConnection): List<TelemetryType> {
        val usedHeap = serverConnection.idToTelemetryType.values.first {
            it.name == "Used Heap" && it.categoryName == "VM Telemetries"
        }
        val custom = serverConnection.idToTelemetryType.values.first {
            it.name == "custom1" && it.categoryName == "Custom Telemetries"
        }
        return listOf(usedHeap, custom)
    }
}
