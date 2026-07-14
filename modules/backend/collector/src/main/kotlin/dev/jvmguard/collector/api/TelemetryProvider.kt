package dev.jvmguard.collector.api

import dev.jvmguard.data.vmdata.CustomTelemetryInfo
import dev.jvmguard.data.vmdata.CustomTelemetryNodeIdentifier
import dev.jvmguard.data.vmdata.TelemetryData
import dev.jvmguard.data.vmdata.TelemetryInterval
import dev.jvmguard.data.vmdata.TelemetryType
import dev.jvmguard.data.vmdata.VM

interface TelemetryProvider {
    val idToTelemetryType: Map<String, TelemetryType>

    val customTelemetryInfo: CustomTelemetryInfo
    val hiddenDeclaredTelemetryNodes: Collection<String>

    fun setDeclaredTelemetryNodeVisibility(nodeName: String, visible: Boolean): Boolean

    fun getTelemetryData(vm: VM?, mainId: String, interval: TelemetryInterval, endTime: Long, plainHeap: Boolean): TelemetryData
    fun getCustomTelemetryData(vm: VM?, nodeIdentifier: CustomTelemetryNodeIdentifier, interval: TelemetryInterval, endTime: Long): TelemetryData
}
