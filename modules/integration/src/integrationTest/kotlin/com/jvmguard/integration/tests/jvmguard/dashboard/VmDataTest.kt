package com.jvmguard.integration.tests.jvmguard.dashboard

import com.jvmguard.agent.config.VmType
import com.jvmguard.integration.JvmGuardTest
import com.jvmguard.integration.Controller
import com.jvmguard.integration.TestServerConnection
import com.jvmguard.integration.TestVmManager
import com.jvmguard.integration.config.VMConfig
import com.jvmguard.integration.util.TimeComparator
import com.jvmguard.integration.util.TransactionTreeComparator
import com.jvmguard.data.config.FrequencyUnit
import com.jvmguard.data.transactions.TransactionDataType
import com.jvmguard.data.transactions.TransactionTreeInterval
import com.jvmguard.data.vmdata.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.TimeUnit

class VmDataTest : JvmGuardTest() {
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

        val startTime = System.nanoTime()
        waitForNextConfigRequest(serverConnection, vms)
        checkTree(
            serverConnection,
            TransactionTreeInterval.HOUR,
            TransactionDataType.TRANSACTION,
            1,
            false,
            TransactionTreeComparator(TimeComparator.THIRTY_PERCENT)
        )
        println("time: " + TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startTime))

        VmFilter.entries.forEach {
            compareCustomAll(serverConnection, it, SparkLineRange.LAST_HOUR, compareCustomHour)
            compareCustomAll(serverConnection, it, SparkLineRange.LAST_DAY, compareCustomDay)
        }
        compareCustom(serverConnection, VmIdentifier.ROOT_GROUP_IDENTIFIER, 2.5)
        compareCustom(serverConnection, VmIdentifier("pool/pool", VmType.POOL), 3.5)
        compareCustom(serverConnection, VmIdentifier("pool", VmType.GROUP), 3.5)
        compareCustom(serverConnection, VmIdentifier("mygroup", VmType.GROUP), 1.5)

        compareVmTelemetries(serverConnection)

        val completedTransactions = getTransactionTelemetryType(serverConnection)
        compareTransaction(serverConnection, completedTransactions, 1.toBigDecimal())

        terminate(vmManager, vms.find { it.name == "idle" }!!)
        sleep(1000)
        assertEqual(
            serverConnection.getVmDataHolders(
                VmFilter.CONNECTED,
                SparkLineRange.LAST_HOUR,
                listOf()
            ).groupChildren.filterKeys { it == VmIdentifier("mygroup", VmType.GROUP) }.values.first().vmDataMap.size, 1
        )
        assertEqual(
            serverConnection.getVmDataHolders(
                VmFilter.RECENT,
                SparkLineRange.LAST_HOUR,
                listOf()
            ).groupChildren.filterKeys { it == VmIdentifier("mygroup", VmType.GROUP) }.values.first().vmDataMap.size, 2
        )

        sleep(25 * 1000)
        checkTelemetryRecent(serverConnection)

        terminate(vmManager, vms.find { it.name == "default" }!!)
        sleep(1000)
        assertEqual(serverConnection.getVmDataHolders(VmFilter.CONNECTED, SparkLineRange.LAST_HOUR, listOf()).groupChildren.size, 1)
        assertEqual(serverConnection.getVmDataHolders(VmFilter.RECENT, SparkLineRange.LAST_HOUR, listOf()).groupChildren.size, 2)

        println("finished")
    }

    private fun checkTelemetryRecent(serverConnection: TestServerConnection) {
        val (connected, thread) = getVmTelemetryTypes(serverConnection)
        val rootDataHolders = serverConnection.getVmDataHolders(VmFilter.RECENT, SparkLineRange.LAST_HOUR, listOf(connected, thread))
        assertEqual(rootDataHolders.data!!.getSparkLineData(connected).scaledCurrent, 3)

        val childDataHolders = rootDataHolders.groupChildren.filterKeys { it == VmIdentifier("mygroup", VmType.GROUP) }.values.first()
        assertEqual(childDataHolders.data!!.getSparkLineData(connected).scaledCurrent, 1)

        val vmDataHolder = childDataHolders.vmDataMap.filterKeys { it.name == "default" }.values.first()
        assertTrue(vmDataHolder.isConnected)
        vmDataHolder.getSparkLineData(connected).apply {
            assertEqual(scaledCurrent, 1)
        }
        vmDataHolder.getSparkLineData(thread).apply {
            assertBetween(scaledCurrent, 4, 29)
        }

        val idleDataHolder = childDataHolders.vmDataMap.filterKeys { it.name == "idle" }.values.first()
        assertFalse(idleDataHolder.isConnected)
        idleDataHolder.getSparkLineData(connected).apply {
            assertEqual(scaledCurrent, 0)
            assertEqual(scaledMax, 1)
        }
        idleDataHolder.getSparkLineData(thread).apply {
            assertEqual(scaledCurrent, 0)
            assertTrue(scaledMax.toBigDecimal() > 3.toBigDecimal() && scaledMax.toBigDecimal() < 30.toBigDecimal()) {
                println(scaledMax)
            }
            assertTrue(scaledMin.toBigDecimal() > 3.toBigDecimal() && scaledMin.toBigDecimal() < 30.toBigDecimal()) {
                println(scaledMin)
            }
        }

    }

    private fun compareVmTelemetries(serverConnection: TestServerConnection) {
        val (connected, thread, usedHeap) = getVmTelemetryTypes(serverConnection)
        val vmDataHolder =
            serverConnection.getGroupVmDataHolder(VmIdentifier.ROOT_GROUP_IDENTIFIER, SparkLineRange.LAST_HOUR, listOf(connected, thread, usedHeap))
        checkConnected(vmDataHolder.getSparkLineData(connected))
        checkThread(vmDataHolder.getSparkLineData(thread))
        checkHeap(vmDataHolder.getSparkLineData(usedHeap))
    }

    private fun checkConnected(sparkLineData: SparkLineData) {
        var dataPointCount = 0
        var dataPoint4Count = 0
        sparkLineData.scaledData.forEach { value ->
            assertTrue(value.toBigDecimal() <= 4.toBigDecimal()) { println(value) }
            if (value.toBigDecimal() > 0.toBigDecimal()) {
                dataPointCount++
            }
            if (value.toBigDecimal() equalTo 4.toBigDecimal()) {
                dataPoint4Count++
            }
        }
        assertBetween(dataPointCount, 3, 8)
        assertBetween(dataPoint4Count, 3, 8)
        assertEqual(sparkLineData.label, "")
        assertEqual(sparkLineData.scaledCurrent.toInt(), 4)
        assertEqual(sparkLineData.scaledMax.toInt(), 4)
        assertEqual(sparkLineData.scaledMin.toInt(), 0)
    }

    private fun checkThread(sparkLineData: SparkLineData) {
        var dataPointCount = 0
        sparkLineData.scaledData.forEach {
            assertTrue(it.toBigDecimal() <= 30.toBigDecimal())
            if (it.toBigDecimal() > 3.toBigDecimal()) {
                dataPointCount++
            }
        }
        assertBetween(dataPointCount, 3, 7)
        assertEqual(sparkLineData.label, "")
        assertTrue(sparkLineData.scaledCurrent.toBigDecimal() > 3.toBigDecimal() && sparkLineData.scaledCurrent.toBigDecimal() < 30.toBigDecimal()) {
            println(sparkLineData.scaledCurrent)
        }
        assertTrue(sparkLineData.scaledMax.toBigDecimal() > 3.toBigDecimal() && sparkLineData.scaledMax.toBigDecimal() < 30.toBigDecimal()) {
            println(sparkLineData.scaledMax)
        }
        assertTrue(sparkLineData.scaledMin.toBigDecimal() > 3.toBigDecimal() && sparkLineData.scaledMin.toBigDecimal() < 30.toBigDecimal()) {
            println(sparkLineData.scaledMin)
        }
    }

    private fun checkHeap(sparkLineData: SparkLineData) {
        var dataPointCount = 0
        if (sparkLineData.label == "MB") {
            sparkLineData.scaledData.forEach { value ->
                assertTrue(value.toBigDecimal() <= 50.toBigDecimal()) {
                    println(value)
                }
                if (value.toBigDecimal() > 1.toBigDecimal()) {
                    dataPointCount++
                }
            }
            assertBetween(dataPointCount, 3, 7)
            assertTrue(sparkLineData.scaledCurrent.toBigDecimal() > 0.7.toBigDecimal() && sparkLineData.scaledCurrent.toBigDecimal() < 50.toBigDecimal()) {
                println(sparkLineData.scaledCurrent)
            }
            assertTrue(sparkLineData.scaledMax.toBigDecimal() > 0.7.toBigDecimal() && sparkLineData.scaledMax.toBigDecimal() < 50.toBigDecimal()) {
                println(sparkLineData.scaledMax)
            }
            assertTrue(sparkLineData.scaledMin.toBigDecimal() > 0.7.toBigDecimal() && sparkLineData.scaledMin.toBigDecimal() < 50.toBigDecimal()) {
                println(sparkLineData.scaledMin)
            }
        } else {
            assertEqual(sparkLineData.label, "KB") {
                println(sparkLineData.label)
            }
            sparkLineData.scaledData.forEach { value ->
                assertTrue(value.toBigDecimal() <= 15000.toBigDecimal()) {
                    println(value)
                }
                if (value.toBigDecimal() > 1.toBigDecimal()) {
                    dataPointCount++
                }
            }
            assertBetween(dataPointCount, 3, 7)
            assertTrue(sparkLineData.scaledCurrent.toBigDecimal() > 700.toBigDecimal() && sparkLineData.scaledCurrent.toBigDecimal() < 15000.toBigDecimal()) {
                println(sparkLineData.scaledCurrent)
            }
            assertTrue(sparkLineData.scaledMax.toBigDecimal() > 700.toBigDecimal() && sparkLineData.scaledMax.toBigDecimal() < 15000.toBigDecimal()) {
                println(sparkLineData.scaledMax)
            }
            assertTrue(sparkLineData.scaledMin.toBigDecimal() > 700.toBigDecimal() && sparkLineData.scaledMin.toBigDecimal() < 15000.toBigDecimal()) {
                println(sparkLineData.scaledMin)
            }
        }
    }

    private fun setFrequency(serverConnection: TestServerConnection, value: FrequencyUnit) {
        serverConnection.setGlobalConfig(serverConnection.getGlobalConfig(false).apply {
            frequencyUnit = value
        })
    }

    private fun checkTransactionBase(sparkLineData: SparkLineData, frequencyUnit: FrequencyUnit, totalMultiplier: BigDecimal) {
        assertTrue(sparkLineData.scaledMax.toBigDecimal() > totalMultiplier * frequencyUnit.multiplier * 100 && sparkLineData.scaledMax.toBigDecimal() < 250L.toBigDecimal() * frequencyUnit.multiplier) {
            println("$sparkLineData.scaledMax with $frequencyUnit")
        }
        assertSimilar(sparkLineData.scaledMin, 0)
        assertEqual(sparkLineData.label, frequencyUnit.label)
    }

    private fun compareTransaction(serverConnection: TestServerConnection, telemetryType: TelemetryType, totalMultiplier: BigDecimal) {
        FrequencyUnit.entries.forEach { frequencyUnit ->
            setFrequency(serverConnection, frequencyUnit)

            val vmDataHolder = serverConnection.getGroupVmDataHolder(VmIdentifier.ROOT_GROUP_IDENTIFIER, SparkLineRange.LAST_HOUR, listOf(telemetryType))
            val sparkLineData = vmDataHolder.getSparkLineData(telemetryType)
            checkTransactionBase(sparkLineData, frequencyUnit, totalMultiplier)

            var dataPointCount = 0
            var totalValue = BigDecimal(0)
            sparkLineData.scaledData.forEach {
                if (it.toBigDecimal() > BigDecimal(0)) {
                    dataPointCount++
                    totalValue += it.toBigDecimal()
                }
            }
            if (telemetryType.name.startsWith("Slow")) {
                assertBetween(dataPointCount, 1, 2)
            } else {
                assertBetween(dataPointCount, 3, 7)
            }
            val calculatedTransactionCountPerVm = totalValue * FrequencyUnit.PER_MINUTE.multiplier / sparkLineData.frequencyUnit.multiplier
            assertSimilar(calculatedTransactionCountPerVm, 33600 * totalMultiplier) {
                println(sparkLineData.scaledData)
            }
        }
        setFrequency(serverConnection, FrequencyUnit.PER_MINUTE)
    }

    private fun getVmTelemetryTypes(serverConnection: TestServerConnection): List<TelemetryType> {
        val connected = serverConnection.idToTelemetryType.values.find {
            it.name == "Connected VMs" && it.categoryName == "VM Telemetries"
        }!!
        val thread = serverConnection.idToTelemetryType.values.find {
            it.name == "Thread Count" && it.categoryName == "VM Telemetries"
        }!!
        val usedHeap = serverConnection.idToTelemetryType.values.find {
            it.name == "Used Heap" && it.categoryName == "VM Telemetries"
        }!!
        return listOf(connected, thread, usedHeap)
    }

    private fun getTransactionTelemetryType(serverConnection: TestServerConnection): TelemetryType =
        serverConnection.idToTelemetryType.values.find {
            it.name == "Completed Transactions" && it.categoryName == "Transactions"
        }!!

    private fun compareCustomAll(
        serverConnection: TestServerConnection,
        vmFilter: VmFilter,
        sparkLineRange: SparkLineRange,
        compare: (VmDataHolder, TelemetryType, TelemetryType, TelemetryType, Number) -> Unit
    ) {
        val (vmNoSingle, vmNoGroupNormal, vmNoGroupDouble) = getCustomTelemetryTypes(serverConnection)
        val rootDataHolders = serverConnection.getVmDataHolders(vmFilter, sparkLineRange, listOf(vmNoSingle, vmNoGroupNormal, vmNoGroupDouble))
        assertEqual(rootDataHolders.vmDataMap.size, 0)
        assertEqual(rootDataHolders.groupChildren.size, 2)

        compare(rootDataHolders.data!!, vmNoSingle, vmNoGroupNormal, vmNoGroupDouble, 2.5)

        rootDataHolders.groupChildren.filterKeys { it == VmIdentifier("mygroup", VmType.GROUP) }.values.first().apply {
            compare(data!!, vmNoSingle, vmNoGroupNormal, vmNoGroupDouble, 1.5)
            assertEqual(vmDataMap.size, 2)
            compare(vmDataMap.filterKeys { it.name == "default" }.values.first(), vmNoSingle, vmNoGroupNormal, vmNoGroupDouble, 2)
            compare(vmDataMap.filterKeys { it.name == "idle" }.values.first(), vmNoSingle, vmNoGroupNormal, vmNoGroupDouble, 1)
        }

        rootDataHolders.groupChildren.filterKeys { it == VmIdentifier("pool", VmType.GROUP) }.values.first().apply {
            compare(data!!, vmNoSingle, vmNoGroupNormal, vmNoGroupDouble, 3.5)
            assertEqual(vmDataMap.size, 0)
            assertEqual(groupChildren.size, 1)

            groupChildren.filterKeys { it == VmIdentifier("pool/pool", VmType.POOL) }.values.first().apply {
                assertEqual(vmDataMap.size, 2)
                assertEqual(groupChildren.size, 0)

                val poolDataHolder = vmDataMap.values.first()
                assertTrue(poolDataHolder.vm.verbose.startsWith("VM \"pool/pool/127.0.0.1:")) {
                    println(poolDataHolder.vm)
                }
                assertTrue(poolDataHolder.vm.type == VmType.POOLED) {
                    println(poolDataHolder.vm)
                }
            }
        }

    }

    private fun compareCustom(serverConnection: TestServerConnection, vmIdentifier: VmIdentifier, expectedCount: Number) {
        val (vmNoSingle, vmNoGroupNormal, vmNoGroupDouble) = getCustomTelemetryTypes(serverConnection)

        val vmDataHolder = serverConnection.getGroupVmDataHolder(vmIdentifier, SparkLineRange.LAST_HOUR, listOf(vmNoSingle, vmNoGroupNormal, vmNoGroupDouble))
        compareCustomHour(vmDataHolder, vmNoSingle, vmNoGroupNormal, vmNoGroupDouble, expectedCount)

        val singleDataHolder = serverConnection.getGroupVmDataHolder(vmIdentifier, SparkLineRange.LAST_DAY, listOf(vmNoSingle))
        compareCustomDay(singleDataHolder, vmNoSingle, vmNoGroupNormal, vmNoGroupDouble, expectedCount)
    }

    private val compareCustomDay = { vmDataHolder: VmDataHolder, vmNoSingle: TelemetryType, _: TelemetryType, _: TelemetryType, expectedCount: Number ->
        val sparkLineData = vmDataHolder.getSparkLineData(vmNoSingle)
        val singleData = sparkLineData.scaledData

        assertBetween(singleData.count { it.toBigDecimal() equalTo expectedCount.toBigDecimal() }, 1, 10)
        assertEqual(sparkLineData.scaledCurrent, expectedCount)
        assertEqual(sparkLineData.scaledMin, expectedCount)
        assertEqual(sparkLineData.scaledMax, expectedCount)
        assertEqual(sparkLineData.label, "")
        assertEqual(sparkLineData.divisor, 1)
        assertEqual(sparkLineData.multiplier, 1)
    }

    @Suppress("USELESS_CAST")
    private val compareCustomHour =
        { vmDataHolder: VmDataHolder, vmNoSingle: TelemetryType, vmNoGroupNormal: TelemetryType, vmNoGroupDouble: TelemetryType, expectedCount: Number ->
            val sparkLineData = vmDataHolder.getSparkLineData(vmNoSingle)
            val singleData = sparkLineData.scaledData
            assertTrue(singleData == vmDataHolder.getSparkLineData(vmNoGroupNormal).scaledData)
            assertTrue(singleData.map { if (it is Long) (it * 2) else (it * BigDecimal(2)) as Number } == vmDataHolder.getSparkLineData(vmNoGroupDouble).scaledData)

            assertBetween(singleData.count { it.toBigDecimal() equalTo expectedCount.toBigDecimal() }, 5, 7) {
                println(expectedCount)
                println(singleData)
            }
            assertEqual(sparkLineData.scaledCurrent, expectedCount)
            assertEqual(sparkLineData.scaledMin, expectedCount)
            assertEqual(sparkLineData.scaledMax, expectedCount)
            assertEqual(sparkLineData.label, "")
            assertEqual(sparkLineData.divisor, 1)
            assertEqual(sparkLineData.multiplier, 1)
        }

    private fun getCustomTelemetryTypes(serverConnection: TestServerConnection): List<TelemetryType> {
        val vmNoSingle = serverConnection.idToTelemetryType.values.find {
            it.name == "vmNoSingle" && it.categoryName == "Custom Telemetries"
        }!!
        val vmNoGroupNormal = serverConnection.idToTelemetryType.values.find {
            it.name == "vmNoGroup (normal)" && it.categoryName == "Custom Telemetries"
        }!!
        val vmNoGroupDouble = serverConnection.idToTelemetryType.values.find {
            it.name == "vmNoGroup (double)" && it.categoryName == "Custom Telemetries"
        }!!
        return listOf(vmNoSingle, vmNoGroupNormal, vmNoGroupDouble)
    }
}

fun Number.toBigDecimal() = when (this) {
    is BigDecimal -> this
    is Double -> BigDecimal(this)
    else -> BigDecimal(this.toString())
}

operator fun BigDecimal.times(other: Number): BigDecimal = this.multiply(other.toBigDecimal())
operator fun BigDecimal.div(other: Number): BigDecimal = this.divide(other.toBigDecimal(), RoundingMode.HALF_EVEN)
operator fun Number.times(other: BigDecimal): BigDecimal = this.toBigDecimal().multiply(other)
operator fun Number.div(other: BigDecimal): BigDecimal = this.toBigDecimal().divide(other, RoundingMode.HALF_EVEN)
infix fun BigDecimal.equalTo(other: BigDecimal) = this.compareTo(other) == 0
