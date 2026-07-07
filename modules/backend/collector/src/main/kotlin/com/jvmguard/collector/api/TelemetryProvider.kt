package com.jvmguard.collector.api

import com.jvmguard.data.vmdata.CustomTelemetryInfo
import com.jvmguard.data.vmdata.CustomTelemetryNodeIdentifier
import com.jvmguard.data.vmdata.TelemetryData
import com.jvmguard.data.vmdata.TelemetryInterval
import com.jvmguard.data.vmdata.TelemetryType
import com.jvmguard.data.vmdata.VM

interface TelemetryProvider {
    val idToTelemetryType: Map<String, TelemetryType>

    val customTelemetryInfo: CustomTelemetryInfo
    val hiddenDeclaredTelemetryNodes: Collection<String>

    fun setDeclaredTelemetryNodeVisibility(nodeName: String, visible: Boolean): Boolean

    fun getTelemetryData(vm: VM?, mainId: String, interval: TelemetryInterval, endTime: Long, plainHeap: Boolean): TelemetryData
    fun getCustomTelemetryData(vm: VM?, nodeIdentifier: CustomTelemetryNodeIdentifier, interval: TelemetryInterval, endTime: Long): TelemetryData
}
