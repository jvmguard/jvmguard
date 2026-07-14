package dev.jvmguard.integration.tests.jvmguard.telemetry

import dev.jvmguard.agent.AgentConstants
import dev.jvmguard.agent.config.VmType
import dev.jvmguard.agent.config.telemetry.TelemetryUnit
import dev.jvmguard.agent.telemetry.TelemetryFormatImpl
import dev.jvmguard.annotation.TelemetryFormat
import dev.jvmguard.annotation.TelemetryFormat.Unit
import dev.jvmguard.data.config.FrequencyUnit
import dev.jvmguard.data.vmdata.*
import dev.jvmguard.integration.Controller
import dev.jvmguard.integration.JvmGuardTest
import dev.jvmguard.integration.TestServerConnection
import dev.jvmguard.integration.TestVmManager
import dev.jvmguard.integration.config.VMConfig
import dev.jvmguard.integration.util.nonNullPlainScaledData
import dev.jvmguard.integration.util.nonNullRootNode
import java.math.BigDecimal

class CustomTelemetryFormatTest : JvmGuardTest() {

    override fun getGroupName(vmNo: Int) = if (vmNo >= 3) "group2" else super.getGroupName(vmNo)
    override fun getVmCount(vmConfig: VMConfig, runNo: Int) = 4

    override fun connect(vmManager: TestVmManager, serverConnection: TestServerConnection, controller: Controller) {
        val vms = waitForConnections(serverConnection)

        sleep(1000 * 70)

        assertEqual(serverConnection.idToTelemetryType.values.count { it.isVisible && it.name.startsWith("stacked (") }, 2) {
            println(serverConnection.idToTelemetryType)
        }
        serverConnection.setDeclaredTelemetryNodeVisibility("stacked", false)
        assertEqual(serverConnection.idToTelemetryType.values.count { it.isVisible && it.name.startsWith("stacked (") }, 0) {
            println(serverConnection.idToTelemetryType)
        }
        assertEqual(serverConnection.customTelemetryNodes.size, 10)
        assertEqual(serverConnection.customTelemetryNodes.count { it.name == "stacked" }, 0)
        serverConnection.setDeclaredTelemetryNodeVisibility("micro", false)
        assertEqual(serverConnection.customTelemetryNodes.size, 9)
        assertEqual(serverConnection.customTelemetryNodes.count { it.name == "micro" }, 0)
        assertEqual(serverConnection.hiddenDeclaredTelemetryNodes.size, 2)
        assertEqual(serverConnection.hiddenDeclaredTelemetryNodes.count { it == "micro" }, 1)
        assertEqual(serverConnection.hiddenDeclaredTelemetryNodes.count { it == "stacked" }, 1)
        serverConnection.setDeclaredTelemetryNodeVisibility("stacked", true)
        serverConnection.setDeclaredTelemetryNodeVisibility("micro", true)
        assertEqual(serverConnection.customTelemetryNodes.size, 11)
        assertEqual(serverConnection.customTelemetryNodes.count { it.name == "stacked" }, 1)
        assertEqual(serverConnection.customTelemetryNodes.count { it.name == "micro" }, 1)
        assertEqual(serverConnection.idToTelemetryType.values.count { it.isVisible && it.name.startsWith("stacked (") }, 2) {
            println(serverConnection.idToTelemetryType)
        }

        val telemetryNodeIdentifiers = serverConnection.customTelemetryNodes
        assertEqual(telemetryNodeIdentifiers.size, 11)
        assertEqual(telemetryNodeIdentifiers.filter { it.type == CustomTelemetryNodeIdentifier.Type.DECLARED }.size, 11)
        assertTrue(telemetryNodeIdentifiers.find { it.name == "requestedConfigurationNumber" } != null)

        checkSingleFormat(
            serverConnection,
            telemetryNodeIdentifiers.first { it.name == "micro" },
            TelemetryFormatImpl(Unit.MICROSECONDS, false, true, 0),
            100.toBigDecimal()
        )
        checkSingleFormat(
            serverConnection,
            telemetryNodeIdentifiers.first { it.name == "nano" },
            TelemetryFormatImpl(Unit.NANOSECONDS, true, true, 0),
            1_000_000.toBigDecimal()
        )
        checkSingleFormat(
            serverConnection,
            telemetryNodeIdentifiers.first { it.name == "perSecond" },
            TelemetryFormatImpl(Unit.PER_SECOND, false, true, 1),
            10.toBigDecimal()
        )
        checkSingleFormat(
            serverConnection,
            telemetryNodeIdentifiers.first { it.name == "percent" },
            TelemetryFormatImpl(Unit.PERCENT, false, true, -2),
            35.toBigDecimal()
        )
        checkSingleFormat(
            serverConnection,
            telemetryNodeIdentifiers.first { it.name == "plain" },
            TelemetryFormatImpl(Unit.PLAIN, false, false, 0),
            100.toBigDecimal()
        )

        val dataHolder = serverConnection.getGroupVmDataHolder(null, SparkLineRange.LAST_HOUR, serverConnection.idToTelemetryType.values.filter {
            it.telemetryIdentifier.additionalType == AgentConstants.TELEMETRY_TYPE_DECLARED
        })
        var sparkLineData = dataHolder.getSparkLineData(serverConnection.idToTelemetryType.values.find { it.name == "micro" }!!)
        assertEqual(sparkLineData.scaledCurrent, 100)
        assertEqual(sparkLineData.label, "\u00b5s")
        sparkLineData = dataHolder.getSparkLineData(serverConnection.idToTelemetryType.values.find { it.name == "millis" }!!)
        assertEqual(sparkLineData.telemetryType.unit, TelemetryUnit.MILLISECONDS)
        assertEqual(sparkLineData.scaledCurrent, 30)
        assertEqual(sparkLineData.label, "s")
        sparkLineData = dataHolder.getSparkLineData(serverConnection.idToTelemetryType.values.find { it.name == "percent" }!!)
        assertEqual(sparkLineData.scaledCurrent, 35)
        assertEqual(sparkLineData.label, "%")

        val telemetryData = serverConnection.getCustomTelemetryData(
            null,
            telemetryNodeIdentifiers.first { it.name == "stacked" },
            TelemetryInterval.TEN_MINUTES,
            System.currentTimeMillis()
        )
        assertEqual(telemetryData.nonNullRootNode.calculateUnitScale(FrequencyUnit.PER_MINUTE).description, "stacked")
        assertEqual(telemetryData.nonNullRootNode.children.size, 0)
        assertEqual(telemetryData.nonNullRootNode.data.size, 2)
        assertEqual(telemetryData.nonNullRootNode.telemetryUnit, TelemetryUnit.PLAIN)
        assertEqual(telemetryData.nonNullRootNode.isStackedData, true)
        assertEqual(telemetryData.nonNullRootNode.data.first { it.description == "line 1" }.nonNullPlainScaledData.last(), 25)
        assertEqual(telemetryData.nonNullRootNode.data.first { it.description == "line 2" }.nonNullPlainScaledData.last(), 75)

        checkNonAveraged(serverConnection, telemetryNodeIdentifiers, null, 4)
        checkNonAveraged(serverConnection, telemetryNodeIdentifiers, vms.first(), 1)
        checkNonAveraged(serverConnection, telemetryNodeIdentifiers, vmManager.getGroupVM(VmIdentifier("default", VmType.GROUP)), 2)

        val formatFromOneId = telemetryNodeIdentifiers.first { it.name == "formatFromOne" }
        val formatFromTwoId = telemetryNodeIdentifiers.first { it.name == "formatFromTwo" }

        sleep(1000 * 20)
        val formatOneUnit = getFormatFromOne(serverConnection, formatFromOneId)
        repeat(5) {
            println("check conflict $formatOneUnit")
            checkFormatFromTwo(serverConnection, formatFromTwoId)
            checkFormatFromOne(
                serverConnection.getCustomTelemetryData(null, formatFromOneId, TelemetryInterval.TEN_MINUTES, System.currentTimeMillis()),
                formatOneUnit
            )
            sleep(1000 * 20)
        }
    }

    private fun checkFormatFromTwo(serverConnection: TestServerConnection, formatFromTwoId: CustomTelemetryNodeIdentifier) {
        val telemetryData = serverConnection.getCustomTelemetryData(null, formatFromTwoId, TelemetryInterval.TEN_MINUTES, System.currentTimeMillis())
        assertEqual(telemetryData.nonNullRootNode.calculateUnitScale(FrequencyUnit.PER_MINUTE).description, formatFromTwoId.name)
        assertEqual(telemetryData.nonNullRootNode.children.size, 0)
        assertEqual(telemetryData.nonNullRootNode.data.size, 1)
        assertEqual(telemetryData.nonNullRootNode.telemetryUnit, TelemetryUnit.BYTES)
        assertEqual(telemetryData.nonNullRootNode.isStackedData, true)
        assertEqual(telemetryData.nonNullRootNode.data[0].nonNullPlainScaledData.last(), 20 * 4)
    }

    private fun getFormatFromOne(serverConnection: TestServerConnection, formatFromOneId: CustomTelemetryNodeIdentifier): TelemetryUnit {
        val telemetryData = serverConnection.getCustomTelemetryData(null, formatFromOneId, TelemetryInterval.TEN_MINUTES, System.currentTimeMillis())
        val expectedUnit = telemetryData.nonNullRootNode.telemetryUnit
        assertTrue(expectedUnit == TelemetryUnit.BYTES || expectedUnit == TelemetryUnit.MICROSECONDS) {
            println(expectedUnit)
        }
        checkFormatFromOne(telemetryData, expectedUnit)
        return expectedUnit
    }

    private fun checkFormatFromOne(telemetryData: TelemetryData, expectedUnit: TelemetryUnit) {
        assertEqual(telemetryData.nonNullRootNode.calculateUnitScale(FrequencyUnit.PER_MINUTE).description, "formatFromOne")
        assertEqual(telemetryData.nonNullRootNode.children.size, 0)
        assertEqual(telemetryData.nonNullRootNode.data.size, 2)
        assertEqual(telemetryData.nonNullRootNode.telemetryUnit, expectedUnit)
        if (expectedUnit == TelemetryUnit.BYTES) {
            assertEqual(telemetryData.nonNullRootNode.isStackedData, true)
            assertEqual(telemetryData.nonNullRootNode.data.first { it.description == "line 1" }.nonNullPlainScaledData.last(), 10 * 4)
            assertEqual(telemetryData.nonNullRootNode.data.first { it.description == "line 2" }.nonNullPlainScaledData.last(), 20 * 4)
        } else if (expectedUnit == TelemetryUnit.MICROSECONDS) {
            assertEqual(telemetryData.nonNullRootNode.isStackedData, false)
            assertEqual(telemetryData.nonNullRootNode.data.first { it.description == "line 1" }.nonNullPlainScaledData.last(), 100)
            assertEqual(telemetryData.nonNullRootNode.data.first { it.description == "line 2" }.nonNullPlainScaledData.last(), 200)
        }
    }

    private fun checkNonAveraged(
        serverConnection: TestServerConnection,
        telemetryNodeIdentifiers: Collection<CustomTelemetryNodeIdentifier>,
        vm: VM?,
        multiplier: Int
    ) {
        val telemetryData = serverConnection.getCustomTelemetryData(
            vm,
            telemetryNodeIdentifiers.first { it.name == "nonAveraged" },
            TelemetryInterval.TEN_MINUTES,
            System.currentTimeMillis()
        )
        assertEqual(telemetryData.nonNullRootNode.calculateUnitScale(FrequencyUnit.PER_MINUTE).description, "nonAveraged")
        assertEqual(telemetryData.nonNullRootNode.children.size, 0)
        assertEqual(telemetryData.nonNullRootNode.data.size, 2)
        assertEqual(telemetryData.nonNullRootNode.telemetryUnit, TelemetryUnit.PLAIN)
        assertEqual(telemetryData.nonNullRootNode.isStackedData, false)
        assertEqual(telemetryData.nonNullRootNode.data.first { it.description == "line 1" }.nonNullPlainScaledData.last(), 25 * multiplier)
        assertEqual(telemetryData.nonNullRootNode.data.first { it.description == "line 2" }.nonNullPlainScaledData.last(), 75 * multiplier)

        if (vm == null || vm.isGroupNode) {
            val dataHolder =
                serverConnection.getGroupVmDataHolder(vm?.qualifiedIdentifier, SparkLineRange.LAST_HOUR, serverConnection.idToTelemetryType.values.filter {
                    it.telemetryIdentifier.additionalType == AgentConstants.TELEMETRY_TYPE_DECLARED
                })
            val sparkLineData = dataHolder.getSparkLineData(serverConnection.idToTelemetryType.values.first {
                it.name == "nonAveraged (line 1)"
            })
            assertEqual(sparkLineData.scaledCurrent, 25 * multiplier)
            assertEqual(sparkLineData.label, "")
        }
    }

    private fun checkSingleFormat(
        serverConnection: TestServerConnection,
        identifier: CustomTelemetryNodeIdentifier,
        expectedFormat: TelemetryFormat,
        expectedValue: BigDecimal
    ) {
        val telemetryData = serverConnection.getCustomTelemetryData(null, identifier, TelemetryInterval.TEN_MINUTES, System.currentTimeMillis())
        assertEqual(telemetryData.nonNullRootNode.calculateUnitScale(FrequencyUnit.PER_MINUTE).description, identifier.name)
        assertEqual(telemetryData.nonNullRootNode.children.size, 0)
        assertEqual(telemetryData.nonNullRootNode.data.size, 1)
        assertEqual(telemetryData.nonNullRootNode.telemetryUnit, TelemetryUnit.fromAnnotationUnit(expectedFormat.value))
        assertEqual(telemetryData.nonNullRootNode.scale, 2 + expectedFormat.scale)
        assertEqual(telemetryData.nonNullRootNode.isStackedData, expectedFormat.stacked)
        val data = telemetryData.nonNullRootNode.data[0]
        assertEqual(data.nonNullPlainScaledData.last(), expectedValue)
    }
}
