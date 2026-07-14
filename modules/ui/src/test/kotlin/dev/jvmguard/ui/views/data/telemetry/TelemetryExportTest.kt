package dev.jvmguard.ui.views.data.telemetry

import dev.jvmguard.agent.config.telemetry.TelemetryUnit
import dev.jvmguard.data.config.FrequencyUnit
import dev.jvmguard.data.vmdata.TelemetryData
import dev.jvmguard.data.vmdata.TelemetryInterval
import dev.jvmguard.data.vmdata.TelemetryNode
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TelemetryExportTest {

    @Test
    fun exportsMetadataAndScaledPoints() {
        val node = TelemetryNode("CPU Load", false).apply {
            setTelemetryUnit(TelemetryUnit.PERCENT, 0)
            addData("CPU Load", "", longArrayOf(5, 12))
        }
        // calculateUnitScale must run before scaled data is read
        TelemetryChartModels.build(
            telemetryData = data(node), node = node, frequencyUnit = FrequencyUnit.PER_MINUTE,
            interval = TelemetryInterval.TEN_MINUTES, endTime = 2000, isTransactions = false,
            logarithmic = false, frozen = null,
        )

        val json = String(TelemetryExport.toJson(data(node), node, TelemetryInterval.TEN_MINUTES, 2000))

        assertTrue(json.contains("\"telemetry\":\"CPU Load\""), json)
        assertTrue(json.contains("\"interval\":\"10 minutes\""), json)
        assertTrue(json.contains("\"points\":["), json)
        assertTrue(json.contains("\"time\":1000"), json)
        assertTrue(json.contains("\"CPU Load\":5"), json)
        assertTrue(json.contains("\"CPU Load\":12"), json)
    }

    private fun data(node: TelemetryNode): TelemetryData =
        TelemetryData().apply {
            rootNode = node
            timestamps = longArrayOf(1000, 2000)
        }
}
