package dev.jvmguard.integration.tests.jvmguard.mbean

import dev.jvmguard.agent.AgentConstants
import dev.jvmguard.agent.config.telemetry.MBeanLineConfig
import dev.jvmguard.agent.config.telemetry.MBeanTelemetryConfig
import dev.jvmguard.agent.config.telemetry.TelemetryUnit
import dev.jvmguard.data.config.FrequencyUnit
import dev.jvmguard.data.config.GroupConfig
import dev.jvmguard.data.vmdata.CustomTelemetryNodeIdentifier
import dev.jvmguard.data.vmdata.SparkLineRange
import dev.jvmguard.data.vmdata.TelemetryInterval
import dev.jvmguard.data.vmdata.TelemetryType
import dev.jvmguard.integration.Controller
import dev.jvmguard.integration.JvmGuardTest
import dev.jvmguard.integration.TestServerConnection
import dev.jvmguard.integration.TestVmManager
import dev.jvmguard.integration.config.VMConfig
import dev.jvmguard.integration.util.nonNullRootNode
import dev.jvmguard.integration.util.nonNullUnitScaledData
import java.math.BigDecimal

class MBeanTelemetryTest : JvmGuardTest() {

    override fun getVmCount(vmConfig: VMConfig, runNo: Int) = 2

    override fun modifyInitialRootConfig(rootConfig: GroupConfig) {
        val mbeanTelemetries = rootConfig.telemetrySettings.mbeanTelemetries
        mbeanTelemetries.add(MBeanTelemetryConfig("mbean tel1", TelemetryUnit.PER_SECOND, 0, true, false).apply {
            lines.add(MBeanLineConfig(MBeanTelemetryWorkload.TEST_BOTH_BEAN_NAME, "Val2", "val 2"))
            lines.add(MBeanLineConfig(MBeanTelemetryWorkload.TEST_BOTH_BEAN_NAME, "Val3", "val 3"))
        })

        mbeanTelemetries.add(MBeanTelemetryConfig("mbean tel1 non", TelemetryUnit.MICROSECONDS, -1, false, true).apply {
            lines.add(MBeanLineConfig(MBeanTelemetryWorkload.TEST_BOTH_BEAN_NAME, "Val2", "val 2"))
            lines.add(MBeanLineConfig(MBeanTelemetryWorkload.TEST_BOTH_BEAN_NAME, "Val3", "val 3"))
        })

        mbeanTelemetries.add(MBeanTelemetryConfig("mbean tel2", TelemetryUnit.BYTES, 2, true, false).apply {
            lines.add(MBeanLineConfig(MBeanTelemetryWorkload.TEST1_BEAN_NAME, "Val3", "val 3"))
            lines.add(MBeanLineConfig(MBeanTelemetryWorkload.TEST1_BEAN_NAME, "Val4", "val 4"))
            lines.add(MBeanLineConfig(MBeanTelemetryWorkload.TEST1_BEAN_NAME, "Val5", "val 5"))
        })

        mbeanTelemetries.add(MBeanTelemetryConfig("mbean tel3", TelemetryUnit.PERCENT, 0, true, false).apply {
            lines.add(MBeanLineConfig(MBeanTelemetryWorkload.TEST1_BEAN_NAME, "Val", "val"))
        })

        mbeanTelemetries.add(MBeanTelemetryConfig("mbean tel4", TelemetryUnit.NANOSECONDS, 0, true, false).apply {
            lines.add(MBeanLineConfig(MBeanTelemetryWorkload.TEST1_BEAN_NAME, "Val3", "val 3"))
        })

        mbeanTelemetries.add(MBeanTelemetryConfig("mbean ms tel4", TelemetryUnit.MILLISECONDS, 0, true, false).apply {
            lines.add(MBeanLineConfig(MBeanTelemetryWorkload.TEST1_BEAN_NAME, "Val3", "val 3"))
        })

        mbeanTelemetries.add(MBeanTelemetryConfig("mbean tel5", TelemetryUnit.PLAIN, 0, true, false).apply {
            lines.add(MBeanLineConfig(MBeanTelemetryWorkload.TEST_COMPLEX, "Complex/test1", "test1 (23)\\/"))
            lines.add(MBeanLineConfig(MBeanTelemetryWorkload.TEST_COMPLEX, "Complex/measure/value", "val1"))
            lines.add(MBeanLineConfig(MBeanTelemetryWorkload.TEST_COMPLEX, "Complex/measure/value2", "val2"))
            lines.add(MBeanLineConfig(MBeanTelemetryWorkload.TEST_COMPLEX, "Complex/singleMap/key\\\\a2\\//value/sub1", "val3"))
            lines.add(MBeanLineConfig(MBeanTelemetryWorkload.TEST_COMPLEX, "Complex/singleMap/key\\\\a20\\//value/sub1", "val4"))
        })
    }

    override fun connect(vmManager: TestVmManager, serverConnection: TestServerConnection, controller: Controller) {
        val vm = waitForConnection(serverConnection, listOf("JVM")).first()
        sleep(1000 * 10)
        assertEqual(
            serverConnection.getMBeanNames(vm, false).toSortedSet(),
            sortedSetOf(
                MBeanTelemetryWorkload.TEST1_BEAN_NAME,
                MBeanTelemetryWorkload.TEST2_BEAN_NAME,
                MBeanTelemetryWorkload.TEST_BOTH_BEAN_NAME,
                MBeanTelemetryWorkload.TEST_COMPLEX
            )
        )

        sleep(1000 * 30)

        assertEqual(serverConnection.idToTelemetryType.values.count { it.telemetryIdentifier.additionalType == AgentConstants.TELEMETRY_TYPE_MBEAN }, 15)
        assertTrue(serverConnection.idToTelemetryType.values.find { it.name == "mbean tel5 (test1 (23)\\/)" } != null)

        assertEqual(serverConnection.customTelemetryNodes.count { it.type == CustomTelemetryNodeIdentifier.Type.MBEAN }, 7)

        var identifier = serverConnection.customTelemetryNodes.first { it.name == "mbean tel1" }
        var telemetryData = serverConnection.getCustomTelemetryData(null, identifier, TelemetryInterval.TEN_MINUTES, System.currentTimeMillis())
        var rootNode = telemetryData.nonNullRootNode
        assertEqual(rootNode.calculateUnitScale(FrequencyUnit.PER_MINUTE).description, identifier.name)
        assertEqual(rootNode.telemetryUnit, TelemetryUnit.PER_SECOND)
        assertEqual(rootNode.isStackedData, false)
        assertEqual(rootNode.unitLabel, " / m")
        assertEqual(rootNode.data.size, 2)
        assertSimilar(rootNode.data.first { it.description == "val 2" }.nonNullUnitScaledData.last()!!, 56.78 * 60, null, 10.0)
        assertSimilar(rootNode.data.first { it.description == "val 3" }.nonNullUnitScaledData.last()!!, 1300.33 * 60, null, 10.0)

        identifier = serverConnection.customTelemetryNodes.first { it.name == "mbean tel1 non" }
        telemetryData = serverConnection.getCustomTelemetryData(null, identifier, TelemetryInterval.TEN_MINUTES, System.currentTimeMillis())
        rootNode = telemetryData.nonNullRootNode
        assertEqual(rootNode.calculateUnitScale(FrequencyUnit.PER_MINUTE).description, identifier.name)
        assertEqual(rootNode.telemetryUnit, TelemetryUnit.MICROSECONDS)
        assertEqual(rootNode.isStackedData, true)
        assertEqual(rootNode.unitLabel, "ms")
        assertEqual(rootNode.data.size, 2)
        assertSimilar(rootNode.data.first { it.description == "val 2" }.nonNullUnitScaledData.last()!!, 56.78 * 2 * 10 / 1000, null, 10.0)
        assertSimilar(rootNode.data.first { it.description == "val 3" }.nonNullUnitScaledData.last()!!, 1300.33 * 2 * 10 / 1000, null, 10.0)

        telemetryData = serverConnection.getCustomTelemetryData(vm, identifier, TelemetryInterval.TEN_MINUTES, System.currentTimeMillis())
        rootNode = telemetryData.nonNullRootNode
        assertEqual(rootNode.calculateUnitScale(FrequencyUnit.PER_MINUTE).description, identifier.name)
        assertEqual(rootNode.telemetryUnit, TelemetryUnit.MICROSECONDS)
        assertEqual(rootNode.isStackedData, true)
        assertEqual(rootNode.unitLabel, "ms")
        assertEqual(rootNode.data.size, 2)
        assertSimilar(rootNode.data.first { it.description == "val 2" }.nonNullUnitScaledData.last()!!, 56.78 * 10 / 1000, null, 10.0)
        assertSimilar(rootNode.data.first { it.description == "val 3" }.nonNullUnitScaledData.last()!!, 1300.33 * 10 / 1000, null, 10.0)

        checkTel2(serverConnection)

        identifier = serverConnection.customTelemetryNodes.find { it.name == "mbean tel3" }!!
        telemetryData = serverConnection.getCustomTelemetryData(null, identifier, TelemetryInterval.TEN_MINUTES, System.currentTimeMillis())
        rootNode = telemetryData.nonNullRootNode
        assertEqual(rootNode.calculateUnitScale(FrequencyUnit.PER_MINUTE).description, identifier.name)
        assertEqual(rootNode.telemetryUnit, TelemetryUnit.PERCENT)
        assertEqual(rootNode.isStackedData, false)
        assertEqual(rootNode.unitLabel, "%")
        assertEqual(rootNode.data.size, 1)
        assertEqual(rootNode.data[0].description, "val")
        assertEqual(rootNode.data[0].nonNullUnitScaledData.last()!!, 100)

        checkTel4(serverConnection, serverConnection.customTelemetryNodes.first { it.name == "mbean tel4" }, TelemetryUnit.NANOSECONDS, "ns")
        checkTel4(serverConnection, serverConnection.customTelemetryNodes.first { it.name == "mbean ms tel4" }, TelemetryUnit.MILLISECONDS, "ms")

        identifier = serverConnection.customTelemetryNodes.find { it.name == "mbean tel5" }!!
        telemetryData = serverConnection.getCustomTelemetryData(null, identifier, TelemetryInterval.TEN_MINUTES, System.currentTimeMillis())
        rootNode = telemetryData.nonNullRootNode
        assertEqual(rootNode.calculateUnitScale(FrequencyUnit.PER_MINUTE).description, identifier.name)
        assertEqual(rootNode.telemetryUnit, TelemetryUnit.PLAIN)
        assertEqual(rootNode.isStackedData, false)
        assertEqual(rootNode.unitLabel, "")
        assertEqual(rootNode.data.size, 3)
        assertEqual(rootNode.data.first { it.description == "test1 (23)\\/" }.nonNullUnitScaledData.last()!!, 1)
        assertEqual(rootNode.data.first { it.description == "val1" }.nonNullUnitScaledData.last()!!, 33)
        assertEqual(rootNode.data.first { it.description == "val3" }.nonNullUnitScaledData.last()!!, 2)

        val allIdToTelemetryTypes = serverConnection.idToTelemetryType
        checkSparklines(serverConnection, allIdToTelemetryTypes)

        modifyCurrentRootConfig(serverConnection) { rootConfig ->
            rootConfig.telemetrySettings.mbeanTelemetries.removeAll { it.name == "mbean tel2" || it.name == "mbean tel3" }
        }

        sleep(1000 * 30)

        assertEqual(serverConnection.idToTelemetryType.values.count { it.telemetryIdentifier.additionalType == AgentConstants.TELEMETRY_TYPE_MBEAN }, 11)
        assertTrue(serverConnection.idToTelemetryType.values.find { it.name.startsWith("mbean tel2") } == null)

        assertEqual(serverConnection.customTelemetryNodes.count { it.type == CustomTelemetryNodeIdentifier.Type.MBEAN }, 5)

        checkSparklines(serverConnection, allIdToTelemetryTypes, false)

        modifyCurrentRootConfig(serverConnection) { rootConfig ->
            rootConfig.telemetrySettings.mbeanTelemetries.add(MBeanTelemetryConfig("mbean tel2", TelemetryUnit.BYTES, 2, true, false).apply {
                lines.add(MBeanLineConfig(MBeanTelemetryWorkload.TEST1_BEAN_NAME, "Val3", "val 3"))
                lines.add(MBeanLineConfig(MBeanTelemetryWorkload.TEST1_BEAN_NAME, "Val4", "val 4"))
                lines.add(MBeanLineConfig(MBeanTelemetryWorkload.TEST1_BEAN_NAME, "Val5", "val 5"))
            })

            rootConfig.telemetrySettings.mbeanTelemetries.add(MBeanTelemetryConfig("mbean tel3", TelemetryUnit.PERCENT, 0, true, false).apply {
                lines.add(MBeanLineConfig(MBeanTelemetryWorkload.TEST1_BEAN_NAME, "Val", "val"))
            })
        }

        sleep(1000 * 30)

        assertEqual(serverConnection.idToTelemetryType.values.count { it.telemetryIdentifier.additionalType == AgentConstants.TELEMETRY_TYPE_MBEAN }, 15)
        assertEqual(serverConnection.idToTelemetryType.values.count { it.name.startsWith("mbean tel2") }, 3)

        assertEqual(serverConnection.customTelemetryNodes.count { it.type == CustomTelemetryNodeIdentifier.Type.MBEAN }, 7)

        checkSparklines(serverConnection, allIdToTelemetryTypes)
        val tel2Data = checkTel2(serverConnection).reversed()
        var emptyFound = false
        var secondDataFound = false
        var secondEmptyFound = false
        tel2Data.forEachIndexed { index, value ->
            if (index == 0) {
                assertSimilar(value!!, 1300.33 / 100, null, 10.0)
            }
            if (value == null && !emptyFound) {
                emptyFound = true
            }
            if (emptyFound && value != null) {
                assertTrue(value > 1.toBigDecimal())
                secondDataFound = true
            }
            if (secondDataFound && value == null) {
                secondEmptyFound = true
            }
        }
        assertTrue(secondEmptyFound) {
            println(tel2Data)
        }
    }

    private fun checkTel4(serverConnection: TestServerConnection, identifier: CustomTelemetryNodeIdentifier, telemetryUnit: TelemetryUnit, label: String) {
        val telemetryData = serverConnection.getCustomTelemetryData(null, identifier, TelemetryInterval.TEN_MINUTES, System.currentTimeMillis())
        val rootNode = telemetryData.nonNullRootNode
        assertEqual(rootNode.calculateUnitScale(FrequencyUnit.PER_MINUTE).description, identifier.name)
        assertEqual(rootNode.telemetryUnit, telemetryUnit)
        assertEqual(rootNode.isStackedData, false)
        assertEqual(rootNode.unitLabel, label)
        assertEqual(rootNode.data.size, 1)
        assertEqual(rootNode.data[0].description, "val 3")
        assertSimilar(rootNode.data[0].nonNullUnitScaledData.last()!!, 1300.33, null, 10.0)
    }

    private fun checkTel2(serverConnection: TestServerConnection): List<BigDecimal?> {
        val identifier = serverConnection.customTelemetryNodes.first { it.name == "mbean tel2" }
        val telemetryData = serverConnection.getCustomTelemetryData(null, identifier, TelemetryInterval.TEN_MINUTES, System.currentTimeMillis())
        val rootNode = telemetryData.nonNullRootNode
        assertEqual(rootNode.calculateUnitScale(FrequencyUnit.PER_MINUTE).description, identifier.name)
        assertEqual(rootNode.telemetryUnit, TelemetryUnit.BYTES)
        assertEqual(rootNode.isStackedData, false)
        assertEqual(rootNode.unitLabel, "b")
        assertEqual(rootNode.data.size, 1)
        assertEqual(rootNode.data[0].description, "val 3")
        assertSimilar(rootNode.data[0].nonNullUnitScaledData.last()!!, 1300.33 / 100, null, 10.0)
        return rootNode.data[0].nonNullUnitScaledData
    }

    private fun checkSparklines(serverConnection: TestServerConnection, allIdToTelemetryTypes: Map<String, TelemetryType>, tel2Available: Boolean = true) {
        val dataHolder = serverConnection.getGroupVmDataHolder(null, SparkLineRange.LAST_HOUR, allIdToTelemetryTypes.values.filter {
            it.telemetryIdentifier.additionalType == AgentConstants.TELEMETRY_TYPE_MBEAN
        })
        var sparkLineData = dataHolder.getSparkLineData(allIdToTelemetryTypes.values.find {
            it.name == "mbean tel5 (test1 (23)\\/)"
        }!!)
        assertEqual(sparkLineData.scaledCurrent, 1)
        sparkLineData = dataHolder.getSparkLineData(allIdToTelemetryTypes.values.find {
            it.name == "mbean tel2 (val 3)"
        }!!)
        assertEqual(sparkLineData.label, "b")
        assertSimilar(sparkLineData.scaledCurrent, if (tel2Available) 1300.33 / 100 else 0, null, 10.0)
        sparkLineData = dataHolder.getSparkLineData(allIdToTelemetryTypes.values.find {
            it.name == "mbean tel2 (val 4)"
        }!!)
        assertEqual(sparkLineData.label, "b")
        assertEqual(sparkLineData.scaledCurrent, 0)
        sparkLineData = dataHolder.getSparkLineData(allIdToTelemetryTypes.values.find {
            it.name == "mbean tel1 (val 2)"
        }!!)
        assertSimilar(sparkLineData.scaledCurrent, 56.78 * 60, null, 10.0)
        sparkLineData = dataHolder.getSparkLineData(allIdToTelemetryTypes.values.find {
            it.name == "mbean tel1 non (val 2)"
        }!!)
        assertSimilar(sparkLineData.scaledCurrent, 56.78 * 2 * 10, null, 10.0)
    }
}
