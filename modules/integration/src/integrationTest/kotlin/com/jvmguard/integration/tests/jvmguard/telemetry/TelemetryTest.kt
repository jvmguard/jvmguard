package com.jvmguard.integration.tests.jvmguard.telemetry

import com.jvmguard.collector.telemetry.TelemetryDataInterval
import com.jvmguard.data.config.FrequencyUnit
import com.jvmguard.data.transactions.TransactionDataType
import com.jvmguard.data.transactions.TransactionTreeInterval
import com.jvmguard.data.vmdata.*
import com.jvmguard.integration.Controller
import com.jvmguard.integration.JvmGuardTest
import com.jvmguard.integration.TestServerConnection
import com.jvmguard.integration.TestVmManager
import com.jvmguard.integration.config.VMConfig
import com.jvmguard.integration.tests.jvmguard.dashboard.equalTo
import com.jvmguard.integration.util.TimeComparator
import com.jvmguard.integration.util.TransactionTreeComparator
import com.jvmguard.integration.util.nonNullRootNode
import com.jvmguard.integration.util.nonNullTimestamps
import com.jvmguard.integration.util.nonNullUnitScaledData
import java.math.BigDecimal

@Suppress("DuplicatedCode")
class TelemetryTest : JvmGuardTest() {
    override fun getJvmGuardOptions(runNo: Int, vmNo: Int, libraryNo: Int) = super.getJvmGuardOptions(runNo, vmNo, libraryNo) + " -Xmx64m"
    override fun getVmCount(vmConfig: VMConfig, runNo: Int) = 4
    override fun isPool(vmNo: Int) = vmNo > 2
    override fun getGroupName(vmNo: Int) = if (vmNo > 2) "pool" else "mygroup"

    override fun getVmName(vmNo: Int) = when {
        vmNo > 2 -> "pool"
        vmNo == 2 -> "default"
        else -> "idle"
    }

    override fun connect(vmManager: TestVmManager, serverConnection: TestServerConnection, controller: Controller) {
        val vms = waitForConnections(serverConnection)
        val defaultVm = vms.first { it.name == "default" }

        val startTime = serverConnection.currentTime

        waitForNextConfigRequest(serverConnection, vms)
        checkTree(
            serverConnection,
            TransactionTreeInterval.HOUR,
            TransactionDataType.TRANSACTION,
            1,
            false,
            TransactionTreeComparator(TimeComparator.THIRTY_PERCENT)
        )

        val time = serverConnection.currentTime

        getAndCheckCustomTelemetryData(TelemetryInterval.TEN_MINUTES, "vmNoSingle", serverConnection, null, time).apply {
            checkCustomSingle(telemetryData, 2.5.toBigDecimal(), ((time - startTime) / recordingMillis).toInt(), totalPoints)
        }

        getAndCheckCustomTelemetryData(TelemetryInterval.TEN_MINUTES, "vmNoGroup", serverConnection, null, time).apply {
            checkCustomGroup(telemetryData, 2.5.toBigDecimal(), ((time - startTime) / recordingMillis).toInt(), totalPoints)
        }

        getAndCheckCustomTelemetryData(TelemetryInterval.TEN_MINUTES, "vmNoSingle", serverConnection, defaultVm, time).apply {
            checkCustomSingle(telemetryData, 2.toBigDecimal(), ((time - startTime) / recordingMillis).toInt(), totalPoints)
        }

        getAndCheckCustomTelemetryData(TelemetryInterval.TEN_MINUTES, "vmNoGroup", serverConnection, defaultVm, time).apply {
            checkCustomGroup(telemetryData, 2.toBigDecimal(), ((time - startTime) / recordingMillis).toInt(), totalPoints)
        }

        getAndCheckTelemetryData(TelemetryInterval.TEN_MINUTES, Telemetry.CONNECTIONS.mainId, serverConnection, null, time).apply {
            checkConnected(telemetryData, 4.toBigDecimal(), ((time - startTime) / recordingMillis).toInt(), totalPoints)
        }

        getAndCheckTelemetryData(TelemetryInterval.TEN_MINUTES, Telemetry.HEAP.mainId, serverConnection, null, time).apply {
            checkHeap(telemetryData, ((time - startTime) / recordingMillis).toInt(), totalPoints)
        }

        getAndCheckTelemetryData(TelemetryInterval.THREE_HOURS, Telemetry.HEAP.mainId, serverConnection, null, time).apply {
            checkHeapHours(telemetryData, totalPoints)
            assertFalse(telemetryData.isNoPreviousData)
        }

        serverConnection.getTelemetryData(null, Telemetry.HEAP.mainId, TelemetryInterval.THREE_HOURS, time - 1000 * 60 * 20).apply {
            assertTrue(isNoPreviousData)
        }

        serverConnection.getTelemetryData(null, Telemetry.HEAP.mainId, TelemetryInterval.TEN_MINUTES, time - 1000 * 60 * 15).apply {
            assertTrue(isNoPreviousData)
        }

        getAndCheckCustomTelemetryData(TelemetryInterval.THREE_HOURS, "vmNoGroup", serverConnection, null, time).apply {
            checkCustomGroupHours(telemetryData, 2.5.toBigDecimal(), totalPoints)
            assertFalse(telemetryData.isNoPreviousData)
        }
        val customTelemetryNodeIdentifier = serverConnection.customTelemetryNodes.first { it.name == "vmNoGroup" }
        serverConnection.getCustomTelemetryData(null, customTelemetryNodeIdentifier, TelemetryInterval.THREE_HOURS, time - 1000 * 60 * 20).apply {
            assertTrue(isNoPreviousData)
        }

        serverConnection.getCustomTelemetryData(null, customTelemetryNodeIdentifier, TelemetryInterval.TEN_MINUTES, time - 1000 * 60 * 15).apply {
            assertTrue(isNoPreviousData)
        }

        getAndCheckTelemetryData(TelemetryInterval.TEN_MINUTES, Telemetry.TRANSACTIONS.mainId, serverConnection, null, time).apply {
            checkTransaction(telemetryData, ((time - startTime) / recordingMillis).toInt(), 1.toBigDecimal())
        }

        getAndCheckTelemetryData(TelemetryInterval.TEN_MINUTES, Telemetry.TRANSACTIONS.mainId, serverConnection, defaultVm, time).apply {
            checkTransaction(telemetryData, ((time - startTime) / recordingMillis).toInt(), BigDecimal("0.33333333"))
        }
    }

    private data class CheckResult(val telemetryData: TelemetryData, val recordingMillis: Long, val totalPoints: Int)

    private fun getAndCheckCustomTelemetryData(
        displayInterval: TelemetryInterval,
        name: String,
        serverConnection: TestServerConnection,
        vm: VM?,
        time: Long
    ): CheckResult {
        val recordingMillis = getRecordingMillis(displayInterval, Telemetry.CUSTOM.mainId).toLong()
        val identifier = serverConnection.customTelemetryNodes.first { it.name == name && it.type == CustomTelemetryNodeIdentifier.Type.DEVOPS }
        val telemetryData = serverConnection.getCustomTelemetryData(vm, identifier, displayInterval, time)
        checkTelemetryData(telemetryData, time, recordingMillis, displayInterval)
        return CheckResult(telemetryData, recordingMillis, telemetryData.nonNullTimestamps.size)
    }

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

    private fun checkHeap(telemetryData: TelemetryData, expectedPoints: Int, totalPoints: Int) {
        assertEqual(telemetryData.nonNullRootNode.data.size, 0)
        assertEqual(telemetryData.nonNullRootNode.children.size, 2)
        val childNode = telemetryData.nonNullRootNode.children.first { it.description == "Heap" }
        assertTrue(childNode.children.isNotEmpty())
        assertEqual(childNode.data.size, 2)
        childNode.calculateUnitScale(FrequencyUnit.PER_MINUTE)
        assertEqual(childNode.unitLabel, "MB")
        assertEqual(childNode.unitLevels, 2)
        assertTrue(childNode.data.find { it.description == "Free" } != null)
        val data = childNode.data.first { it.description == "Used" }
        checkDataPointRange(data, 1.toBigDecimal(), 50.toBigDecimal(), expectedPoints, totalPoints)
    }

    private fun checkHeapHours(telemetryData: TelemetryData, totalPoints: Int) {
        assertEqual(telemetryData.nonNullRootNode.data.size, 0)
        assertEqual(telemetryData.nonNullRootNode.children.size, 2)
        val childNode = telemetryData.nonNullRootNode.children.first { it.description == "Heap" }
        assertTrue(childNode.children.isNotEmpty())
        assertEqual(childNode.data.size, 2)
        childNode.calculateUnitScale(FrequencyUnit.PER_MINUTE)
        assertEqual(childNode.unitLabel, "MB")
        assertEqual(childNode.unitLevels, 2)
        assertTrue(childNode.data.find { it.description == "Free" } != null)
        val data = childNode.data.first { it.description == "Used" }
        checkDataPointRangeHours(data, 1.toBigDecimal(), 50.toBigDecimal(), totalPoints)
    }

    private fun checkEmptyTransaction(data: TelemetryNode.Data, expectedPoints: Int) {
        var dataPointCount = 0
        data.nonNullUnitScaledData.forEach {
            if (it != null && it equalTo 0.toBigDecimal()) {
                dataPointCount++
            }
        }
        assertTrue(dataPointCount > expectedPoints - 3 && dataPointCount < expectedPoints + 2) {
            println("$dataPointCount != $expectedPoints")
            println(data.unitScaledData)
        }
    }

    private fun checkTransaction(telemetryData: TelemetryData, expectedPoints: Int, percentage: BigDecimal) {
        assertEqual(telemetryData.nonNullRootNode.children.size, 2)
        assertEqual(telemetryData.nonNullRootNode.data.size, 0)
        checkTransactionCount(
            telemetryData.nonNullRootNode.children.first { it.description == "Transactions" },
            expectedPoints,
            telemetryData.telemetryInterval!!,
            percentage
        )

        val durationNode = telemetryData.nonNullRootNode.children.first { it.description == "Average Transaction Duration" }
        assertEqual(durationNode.children.size, 0)
        assertEqual(durationNode.data.size, 1)
        durationNode.calculateUnitScale(FrequencyUnit.PER_MINUTE)
        assertEqual(durationNode.unitLabel, "ms")

        var dataPointCount = 0
        var validDataPointCount = 0
        durationNode.data[0].nonNullUnitScaledData.forEach { value ->
            if (value != null) {
                dataPointCount++
                if (value > 150.toBigDecimal() && value < 1200.toBigDecimal()) {
                    validDataPointCount++
                }
            }
        }
        assertTrue(dataPointCount > expectedPoints - 3 && dataPointCount < expectedPoints + 2) {
            println("$dataPointCount != $expectedPoints")
            println(durationNode.data[0].unitScaledData)
        }
        assertTrue(validDataPointCount > expectedPoints - 4 && validDataPointCount < expectedPoints + 2) {
            println("$validDataPointCount != $expectedPoints")
            println(durationNode.data[0].unitScaledData)
        }
    }

    private fun checkTransactionCount(countNode: TelemetryNode, expectedPoints: Int, telemetryInterval: TelemetryInterval, percentage: BigDecimal) {
        assertEqual(countNode.children.size, 0)
        assertEqual(countNode.data.size, 4)

        FrequencyUnit.entries.forEach { frequencyUnit ->
            countNode.calculateUnitScale(frequencyUnit)
            assertEqual(countNode.unitLabel, frequencyUnit.label)

            checkEmptyTransaction(countNode.data.first { it.description == "Error" }, expectedPoints)
            checkEmptyTransaction(countNode.data.first { it.description == "Very Slow" }, expectedPoints)

            var data = countNode.data.first { it.description == "Normal" }

            var dataPointCount = 0
            var totalValue = BigDecimal(0)
            data.nonNullUnitScaledData.forEach {
                if (it != null) {
                    dataPointCount++
                    totalValue += it
                }
            }
            assertTrue(dataPointCount > expectedPoints - 3 && dataPointCount < expectedPoints + 2) {
                println("$dataPointCount != $expectedPoints")
                println(data.unitScaledData)
            }
            var calculatedTransactionCount =
                totalValue * getRecordingMillis(telemetryInterval, Telemetry.TRANSACTIONS.mainId) / (frequencyUnit.multiplier * 1000).toBigDecimal()
            assertSimilar(calculatedTransactionCount, 33000.toBigDecimal() * percentage) {
                println(data.unitScaledData)
            }

            data = countNode.data.first { it.description == "Slow" }

            dataPointCount = 0
            totalValue = BigDecimal(0)
            data.nonNullUnitScaledData.forEach {
                if (it != null) {
                    dataPointCount++
                    totalValue += it
                }
            }
            assertTrue(dataPointCount > expectedPoints - 3 && dataPointCount < expectedPoints + 2) {
                println("$dataPointCount != $expectedPoints")
                println(data.unitScaledData)
            }
            calculatedTransactionCount =
                totalValue * getRecordingMillis(telemetryInterval, Telemetry.TRANSACTIONS.mainId) / (frequencyUnit.multiplier * 1000).toBigDecimal()
            assertSimilar(calculatedTransactionCount, 600.toBigDecimal() * percentage) {
                println(data.unitScaledData)
            }
        }
    }

    private fun checkConnected(telemetryData: TelemetryData, expectedValue: BigDecimal, expectedPoints: Int, totalPoints: Int) {
        telemetryData.nonNullRootNode.calculateUnitScale(FrequencyUnit.PER_MINUTE)
        assertEqual(telemetryData.nonNullRootNode.description, "Connected VMs")
        assertEqual(telemetryData.nonNullRootNode.data.size, 1)
        assertEqual(telemetryData.nonNullRootNode.children.size, 0)
        assertEqual(telemetryData.nonNullRootNode.unitLabel, "")
        assertEqual(telemetryData.nonNullRootNode.unitLevels, 0)
        val data = telemetryData.nonNullRootNode.data.first { it.description == "Connected VMs" }
        checkDataPoints(data, expectedValue, expectedPoints, totalPoints, true)
    }

    private fun checkCustomSingle(telemetryData: TelemetryData, expectedValue: BigDecimal, expectedPoints: Int, totalPoints: Int) {
        assertEqual(telemetryData.nonNullRootNode.description, "vmNoSingle")
        assertEqual(telemetryData.nonNullRootNode.children.size, 0)
        assertEqual(telemetryData.nonNullRootNode.data.size, 1)
        assertEqual(telemetryData.nonNullRootNode.unitLabel, "")
        assertEqual(telemetryData.nonNullRootNode.unitLevels, 0)
        val data = telemetryData.nonNullRootNode.calculateUnitScale(FrequencyUnit.PER_MINUTE).data[0]
        assertEqual(data.description, "vmNoSingle")
        checkDataPoints(data, expectedValue, expectedPoints, totalPoints)
    }

    private fun checkCustomGroup(telemetryData: TelemetryData, expectedValue: BigDecimal, expectedPoints: Int, totalPoints: Int) {
        assertEqual(telemetryData.nonNullRootNode.description, "vmNoGroup")
        assertEqual(telemetryData.nonNullRootNode.children.size, 0)
        assertEqual(telemetryData.nonNullRootNode.data.size, 2)
        assertEqual(telemetryData.nonNullRootNode.unitLabel, "")
        assertEqual(telemetryData.nonNullRootNode.unitLevels, 0)
        val data = telemetryData.nonNullRootNode.calculateUnitScale(FrequencyUnit.PER_MINUTE).data.first { it.description == "normal" }
        checkDataPoints(data, expectedValue, expectedPoints, totalPoints)
        assertTrue(data.nonNullUnitScaledData.map { if (it != null) it * 2.toBigDecimal() else null } == telemetryData.nonNullRootNode.data.first {
            it.description == "double"
        }.unitScaledData)
    }

    private fun checkCustomGroupHours(telemetryData: TelemetryData, expectedValue: BigDecimal, totalPoints: Int) {
        assertEqual(telemetryData.nonNullRootNode.description, "vmNoGroup")
        assertEqual(telemetryData.nonNullRootNode.children.size, 0)
        assertEqual(telemetryData.nonNullRootNode.data.size, 2)
        assertEqual(telemetryData.nonNullRootNode.unitLabel, "")
        assertEqual(telemetryData.nonNullRootNode.unitLevels, 0)
        val data = telemetryData.nonNullRootNode.calculateUnitScale(FrequencyUnit.PER_MINUTE).data.first { it.description == "normal" }
        checkDataPointHours(data, expectedValue, totalPoints)
        assertTrue(data.nonNullUnitScaledData.map { if (it != null) it * 2.toBigDecimal() else null } == telemetryData.nonNullRootNode.data.first {
            it.description == "double"
        }.unitScaledData)
    }

    private fun checkDataPoints(
        data: TelemetryNode.Data,
        expectedValue: BigDecimal,
        expectedPoints: Int,
        totalPoints: Int,
        allPointsAvailable: Boolean = false
    ) {
        var dataPointCount = 0
        var correctPointCount = 0
        assertEqual(data.nonNullUnitScaledData.size, totalPoints)
        data.nonNullUnitScaledData.forEach {
            if (it != null) {
                dataPointCount++
                if (it equalTo expectedValue) {
                    correctPointCount++
                }
            }
        }
        if (allPointsAvailable) {
            assertEqual(dataPointCount, data.nonNullUnitScaledData.size) {
                println(data.unitScaledData)
            }
        } else {
            assertBetween(dataPointCount, expectedPoints - 2, expectedPoints + 1)
            assertBetween(correctPointCount, expectedPoints - 3, expectedPoints + 1)
        }
    }

    private fun checkDataPointHours(data: TelemetryNode.Data, expectedValue: BigDecimal, totalPoints: Int) {
        var dataPointCount = 0
        var correctPointCount = 0
        assertEqual(data.nonNullUnitScaledData.size, totalPoints)
        data.nonNullUnitScaledData.forEach {
            if (it != null) {
                dataPointCount++
                if (it equalTo expectedValue) {
                    correctPointCount++
                }
            }
        }
        assertBetween(dataPointCount, 1, 3)
        assertBetween(correctPointCount, 1, 3)
    }

    private fun checkDataPointRange(data: TelemetryNode.Data, minValue: BigDecimal, maxValue: BigDecimal, expectedPoints: Int, totalPoints: Int) {
        var dataPointCount = 0
        var correctPointCount = 0
        assertEqual(data.nonNullUnitScaledData.size, totalPoints)
        data.nonNullUnitScaledData.forEach {
            if (it != null) {
                dataPointCount++
                if (it in minValue..maxValue) {
                    correctPointCount++
                }
            }
        }
        assertBetween(dataPointCount, expectedPoints - 2, expectedPoints + 1)
        assertBetween(correctPointCount, expectedPoints - 3, expectedPoints + 1)
    }

    private fun checkDataPointRangeHours(data: TelemetryNode.Data, minValue: BigDecimal, maxValue: BigDecimal, totalPoints: Int) {
        var dataPointCount = 0
        var correctPointCount = 0
        assertEqual(data.nonNullUnitScaledData.size, totalPoints)
        data.nonNullUnitScaledData.forEach {
            if (it != null) {
                dataPointCount++
                if (it in minValue..maxValue) {
                    correctPointCount++
                }
            }
        }
        assertBetween(dataPointCount, 1, 3)
        assertBetween(correctPointCount, 1, 3)
    }
}
