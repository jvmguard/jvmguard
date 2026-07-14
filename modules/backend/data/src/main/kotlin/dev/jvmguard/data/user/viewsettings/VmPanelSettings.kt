package dev.jvmguard.data.user.viewsettings

import dev.jvmguard.data.vmdata.SparkLineRange
import dev.jvmguard.data.vmdata.Telemetry
import dev.jvmguard.data.vmdata.TelemetryType
import dev.jvmguard.data.vmdata.VmFilter
import java.io.Serializable

open class VmPanelSettings : Serializable {

    var sparkLineScaleMode: SparkLineScaleMode = SparkLineScaleMode.SEPARATE
    var sparkLineRange: SparkLineRange = SparkLineRange.LAST_HOUR
    var vmFilter: VmFilter = VmFilter.CONNECTED

    var sparkLineMementos: MutableList<SparkLineMemento> = ArrayList(
        listOf(
            SparkLineMemento(Telemetry.HEAP.mainId, TelemetryType.SUB_ID_USED_HEAP),
            SparkLineMemento(Telemetry.CPU.mainId, "")
        )
    )
}
