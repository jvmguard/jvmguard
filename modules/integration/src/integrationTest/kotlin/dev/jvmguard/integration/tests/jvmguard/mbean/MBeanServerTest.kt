package dev.jvmguard.integration.tests.jvmguard.mbean

import dev.jvmguard.agent.config.telemetry.MBeanLineConfig
import dev.jvmguard.agent.config.telemetry.MBeanTelemetryConfig
import dev.jvmguard.agent.config.telemetry.TelemetryUnit
import dev.jvmguard.data.config.FrequencyUnit
import dev.jvmguard.data.config.GroupConfig
import dev.jvmguard.data.vmdata.TelemetryInterval
import dev.jvmguard.integration.Controller
import dev.jvmguard.integration.JvmGuardTest
import dev.jvmguard.integration.TestServerConnection
import dev.jvmguard.integration.TestVmManager
import dev.jvmguard.integration.util.nonNullPlainScaledData
import dev.jvmguard.integration.util.nonNullRootNode
import dev.jvmguard.integration.util.nonNullTimestamps
import dev.jvmguard.integration.util.nonNullUnitScaledData

class MBeanServerTest : JvmGuardTest() {

    override fun modifyInitialRootConfig(rootConfig: GroupConfig) {
        rootConfig.telemetrySettings.mbeanTelemetries.add(MBeanTelemetryConfig("mbean tel1", TelemetryUnit.PLAIN, 0, true, false).apply {
            lines.add(MBeanLineConfig("java.lang:type=ClassLoading", "TotalLoadedClassCount", "class"))
            lines.add(MBeanLineConfig(MBeanServerWorkload.TEST_BOTH_BEAN_NAME, "Val", "custom"))
        })

        rootConfig.telemetrySettings.mbeanTelemetries.add(MBeanTelemetryConfig("mbean tel2", TelemetryUnit.BYTES, 0, true, false).apply {
            lines.add(MBeanLineConfig("java.lang:type=ClassLoading", "TotalLoadedClassCount", "class"))
        })
    }


    override fun connect(vmManager: TestVmManager, serverConnection: TestServerConnection, controller: Controller) {
        val vm = waitForConnection(serverConnection, listOf("JVM")).first()
        sleep(1000 * 10)
        assertEqual(
            serverConnection.getMBeanNames(vm, false).toSet(),
            listOf(MBeanServerWorkload.TEST1_BEAN_NAME, MBeanServerWorkload.TEST2_BEAN_NAME, MBeanServerWorkload.TEST_BOTH_BEAN_NAME).toSet()
        )

        var mBeanData = serverConnection.getMBeanData(vm, MBeanServerWorkload.TEST1_BEAN_NAME, true, true)
        assertEqual(mBeanData.values[findIndex(mBeanData, "Val")], 1)
        mBeanData = serverConnection.getMBeanData(vm, MBeanServerWorkload.TEST2_BEAN_NAME, true, true)
        assertEqual(mBeanData.values[findIndex(mBeanData, "Val")], 2)
        mBeanData = serverConnection.getMBeanData(vm, MBeanServerWorkload.TEST_BOTH_BEAN_NAME, true, true)
        assertEqual(mBeanData.values[findIndex(mBeanData, "Val")], 10)

        sleep(1000 * 30)

        val tel1 = serverConnection.customTelemetryNodes.find { it.name == "mbean tel1" }!!
        val tel2 = serverConnection.customTelemetryNodes.find { it.name == "mbean tel2" }!!

        var telemetryData = serverConnection.getCustomTelemetryData(vm, tel2, TelemetryInterval.TEN_MINUTES, System.currentTimeMillis())
        var rootNode = telemetryData.nonNullRootNode
        assertEqual(rootNode.data.size, 0)
        assertEqual(telemetryData.nonNullTimestamps.size, 60)

        rootNode = serverConnection.getCustomTelemetryData(vm, tel1, TelemetryInterval.TEN_MINUTES, System.currentTimeMillis()).nonNullRootNode
        assertEqual(rootNode.calculateUnitScale(FrequencyUnit.PER_SECOND).description, "mbean tel1")
        assertEqual(rootNode.data.size, 1)
        assertEqual(rootNode.data[0].description, "custom")
        assertEqual(rootNode.data[0].nonNullUnitScaledData.last(), 10.toBigDecimal())

        val mBeanNames = serverConnection.getMBeanNames(vm, true).toSet()
        assertTrue(mBeanNames.size > 8)
        assertTrue(mBeanNames.contains("java.lang:type=ClassLoading"))

        sleep(1000 * 40)

        rootNode = serverConnection.getCustomTelemetryData(vm, tel1, TelemetryInterval.TEN_MINUTES, System.currentTimeMillis()).nonNullRootNode
        assertEqual(rootNode.calculateUnitScale(FrequencyUnit.PER_SECOND).description, "mbean tel1")
        assertEqual(rootNode.data.size, 2)
        assertEqual(rootNode.data[0].description, "class")
        assertTrue(rootNode.data[0].nonNullUnitScaledData.last()!! > 500.toBigDecimal()) {
            println(rootNode.data[0].nonNullUnitScaledData.last())
        }
        assertEqual(rootNode.data[1].description, "custom")
        assertEqual(rootNode.data[1].nonNullUnitScaledData.last(), 10)
        assertEqual(rootNode.telemetryUnit, TelemetryUnit.PLAIN)

        telemetryData = serverConnection.getCustomTelemetryData(vm, tel2, TelemetryInterval.TEN_MINUTES, System.currentTimeMillis())
        rootNode = telemetryData.nonNullRootNode
        assertEqual(rootNode.calculateUnitScale(FrequencyUnit.PER_SECOND).description, "mbean tel2")
        assertEqual(rootNode.data.size, 1)
        assertEqual(telemetryData.nonNullTimestamps.size, 60)
        assertEqual(rootNode.data[0].description, "class")
        assertEqual(rootNode.telemetryUnit, TelemetryUnit.BYTES)
        assertTrue(rootNode.data[0].nonNullPlainScaledData.last()!! > 500.toBigDecimal()) {
            println(rootNode.data[0].nonNullPlainScaledData.last())
        }
    }
}
