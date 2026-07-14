package dev.jvmguard.ui.views.vms

import dev.jvmguard.agent.config.VmType
import dev.jvmguard.data.vmdata.SparkLineData
import dev.jvmguard.data.vmdata.TelemetryType
import dev.jvmguard.data.vmdata.VmDataHolder
import dev.jvmguard.data.vmdata.VmIdentifier

class VmGroupItem(
    override val key: String,
    override val name: String,
    val vmType: VmType,
    private val dataHolder: VmDataHolder?
) : VmTreeItem() {

    override val selectionId: VmIdentifier get() = VmIdentifier(key, vmType)

    val vmCount: Int
        get() = children.sumOf { if (it is VmGroupItem) it.vmCount else 1 }

    override fun statusSortValue(): Long = vmCount.toLong()

    override fun sparkLineData(telemetryType: TelemetryType): SparkLineData? =
        dataHolder?.getSparkLineData(telemetryType)
}
