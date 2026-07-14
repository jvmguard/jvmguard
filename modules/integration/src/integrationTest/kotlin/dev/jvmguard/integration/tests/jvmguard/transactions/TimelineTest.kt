package dev.jvmguard.integration.tests.jvmguard.transactions

import dev.jvmguard.agent.config.transactions.DeclaredTransactionDef
import dev.jvmguard.agent.config.transactions.TransactionType
import dev.jvmguard.data.config.FrequencyUnit
import dev.jvmguard.data.config.GroupConfig
import dev.jvmguard.data.transactions.TransactionInfo
import dev.jvmguard.data.transactions.TransactionTreeInterval
import dev.jvmguard.data.transactions.TransactionTreeValueType
import dev.jvmguard.data.vmdata.TelemetryData
import dev.jvmguard.data.vmdata.VM
import dev.jvmguard.integration.Controller
import dev.jvmguard.integration.JvmGuardTest
import dev.jvmguard.integration.TestServerConnection
import dev.jvmguard.integration.TestVmManager
import dev.jvmguard.integration.config.VMConfig
import dev.jvmguard.integration.util.nonNullRootNode
import dev.jvmguard.integration.util.nonNullTimestamps
import dev.jvmguard.integration.util.nonNullUnitScaledData

class TimelineTest : JvmGuardTest() {
    override fun getJvmGuardOptions(runNo: Int, vmNo: Int, libraryNo: Int) = super.getJvmGuardOptions(runNo, vmNo, libraryNo) + " -Xmx64m"
    override fun getVmCount(vmConfig: VMConfig, runNo: Int) = 2

    override fun modifyInitialRootConfig(rootConfig: GroupConfig) {
        rootConfig.transactionSettings.transactionDefs.add(DeclaredTransactionDef().apply {
            initDefault()
            group.usedValue = "sample"
        })
    }

    override fun connect(vmManager: TestVmManager, serverConnection: TestServerConnection, controller: Controller) {
        val vm = waitForConnections(serverConnection).first()

        sleep(1000 * 60 * 4)

        check(serverConnection, null, 1.0)
        check(serverConnection, vm, 0.5)
    }

    private fun check(serverConnection: TestServerConnection, vm: VM?, countMultiplier: Double) {
        val time = serverConnection.currentTime
        var telemetryData = serverConnection.getHotspotsTimeLine(
            vm,
            time - 1000 * 60 * 10,
            time,
            TransactionInfo("TimelineWorkload.prep2", TransactionType.DECLARED),
            TransactionTreeValueType.AVERAGE,
            TransactionTreeInterval.MINUTE
        )
        assertTrue(getAndCheckData(telemetryData, time, "ms").filter { it > 150 && it < 250 }.size > 1) {
            println(getAndCheckData(telemetryData, time))
        }

        telemetryData = serverConnection.getHotspotsTimeLine(
            vm,
            time - 1000 * 60 * 10,
            time,
            TransactionInfo("TimelineWorkload.prep2", TransactionType.DECLARED),
            TransactionTreeValueType.COUNT,
            TransactionTreeInterval.MINUTE
        )
        assertTrue(getAndCheckData(telemetryData, time, "").filter { it > 250 * countMultiplier && it < 350 * countMultiplier }.size > 1) {
            println(getAndCheckData(telemetryData, time))
        }

        telemetryData = serverConnection.getHotspotsTimeLine(
            vm,
            time - 1000 * 60 * 10,
            time,
            TransactionInfo("TimelineWorkload.prep2", TransactionType.DECLARED),
            TransactionTreeValueType.TOTAL,
            TransactionTreeInterval.MINUTE
        )
        assertTrue(getAndCheckData(telemetryData, time, "s").filter { it > 40 * countMultiplier && it < 80 * countMultiplier }.size > 1) {
            println(getAndCheckData(telemetryData, time))
        }
    }

    private fun getAndCheckData(telemetryData: TelemetryData, time: Long, label: String? = null): List<Double> {
        assertEqual(telemetryData.nonNullTimestamps.size, 10)
        assertTrue(telemetryData.nonNullTimestamps[telemetryData.nonNullTimestamps.size - 1] < time + 5000 && telemetryData.nonNullTimestamps[telemetryData.nonNullTimestamps.size - 1] > time - 60 * 1000 * 3) {
            println(telemetryData.nonNullTimestamps[telemetryData.nonNullTimestamps.size - 1])
            println(time)
        }
        assertEqual(telemetryData.nonNullRootNode.children.size, 0)
        assertEqual(telemetryData.nonNullRootNode.data.size, 1)
        telemetryData.nonNullRootNode.calculateUnitScale(FrequencyUnit.PER_MINUTE)
        if (label != null) assertEqual(telemetryData.nonNullRootNode.unitLabel, label)
        return telemetryData.nonNullRootNode.data[0].nonNullUnitScaledData.filterNotNull().map { it.toDouble() }
    }
}
