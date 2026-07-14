package export

import dev.jvmguard.agent.config.telemetry.TelemetryUnit
import dev.jvmguard.common.export.TelemetryExport
import dev.jvmguard.data.vmdata.TelemetryNode
import org.junit.jupiter.api.Test

class TelemetryExportTest {

    @Test
    fun telemetry() {
        val node = TelemetryNode("test description", false)
        node.setTelemetryUnit(TelemetryUnit.BYTES, 2)
        node.addData("used", "u1", longArrayOf(320, 5000, 10, 0, Long.MIN_VALUE))
        node.addData("free", "u1", longArrayOf(720, 2000, 10, 0, Long.MIN_VALUE))

        val export = TelemetryExport(longArrayOf(0, 1000, 2000, 3000, 4000), node)
        ExportTestHelper.exportAndCompare(export, "telemetry")
    }

    @Test
    fun empty() {
        val node = TelemetryNode("test description", false)
        node.setTelemetryUnit(TelemetryUnit.BYTES, 2)
        val export = TelemetryExport(null, node)
        ExportTestHelper.exportAndCompare(export, "telemetry_empty")
    }
}
