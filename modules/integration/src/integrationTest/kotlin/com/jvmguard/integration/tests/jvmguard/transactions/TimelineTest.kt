package com.jvmguard.integration.tests.jvmguard.transactions

import com.jvmguard.agent.config.transactions.DevOpsAnnotatedTransactionDef
import com.jvmguard.agent.config.transactions.TransactionType
import com.jvmguard.data.config.FrequencyUnit
import com.jvmguard.data.config.GroupConfig
import com.jvmguard.data.transactions.TransactionInfo
import com.jvmguard.data.transactions.TransactionTreeInterval
import com.jvmguard.data.transactions.TransactionTreeValueType
import com.jvmguard.data.vmdata.TelemetryData
import com.jvmguard.data.vmdata.VM
import com.jvmguard.integration.Controller
import com.jvmguard.integration.JvmGuardTest
import com.jvmguard.integration.TestServerConnection
import com.jvmguard.integration.TestVmManager
import com.jvmguard.integration.config.VMConfig
import com.jvmguard.integration.util.nonNullRootNode
import com.jvmguard.integration.util.nonNullTimestamps
import com.jvmguard.integration.util.nonNullUnitScaledData

class TimelineTest : JvmGuardTest() {
    override fun getJvmGuardOptions(runNo: Int, vmNo: Int, libraryNo: Int) = super.getJvmGuardOptions(runNo, vmNo, libraryNo) + " -Xmx64m"
    override fun getVmCount(vmConfig: VMConfig, runNo: Int) = 2

    override fun modifyInitialRootConfig(rootConfig: GroupConfig) {
        rootConfig.transactionSettings.transactionDefs.add(DevOpsAnnotatedTransactionDef().apply {
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
            TransactionInfo("TimelineWorkload.prep2", TransactionType.DEVOPS),
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
            TransactionInfo("TimelineWorkload.prep2", TransactionType.DEVOPS),
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
            TransactionInfo("TimelineWorkload.prep2", TransactionType.DEVOPS),
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
