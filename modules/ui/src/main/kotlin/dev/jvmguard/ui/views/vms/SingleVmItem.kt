package dev.jvmguard.ui.views.vms

import dev.jvmguard.agent.config.VmType
import dev.jvmguard.data.vmdata.*

class SingleVmItem(private val dataHolder: VmDataHolder) : VmTreeItem() {

    override val key: String get() = "vm/" + dataHolder.vm.id
    override val name: String get() = dataHolder.vm.name
    override val selectionId: VmIdentifier get() = dataHolder.vm.qualifiedIdentifier

    val vm: VM get() = dataHolder.vm
    val vmType: VmType get() = dataHolder.vm.type
    val isConnected: Boolean get() = dataHolder.isConnected
    val isOutdatedAgent: Boolean get() = dataHolder.isOutdatedAgent
    val statusChangeTime: Long get() = dataHolder.statusChangeTime

    val secondaryText: String?
        get() {
            if (dataHolder.vm.type == VmType.POOLED) {
                return dataHolder.vm.formattedInstanceId
            }
            val hostName = dataHolder.hostName
            return if (hostName.isEmpty()) null else "$hostName:${dataHolder.port}"
        }

    override fun statusSortValue(): Long =
        if (dataHolder.isConnected) -dataHolder.statusChangeTime else dataHolder.statusChangeTime

    override fun sparkLineData(telemetryType: TelemetryType): SparkLineData =
        dataHolder.getSparkLineData(telemetryType)
}
