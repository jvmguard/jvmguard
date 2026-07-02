package com.jvmguard.ui.views.vms

import com.jvmguard.data.vmdata.SparkLineData
import com.jvmguard.data.vmdata.TelemetryType
import com.jvmguard.data.vmdata.VmIdentifier
import com.jvmguard.ui.components.sparkline.SparklineState

abstract class VmTreeItem {

    private val _children = mutableListOf<VmTreeItem>()
    val children: List<VmTreeItem> get() = _children
    private val graphMax = mutableMapOf<TelemetryType, Double>()

    abstract val key: String
    abstract val name: String

    abstract val selectionId: VmIdentifier

    abstract fun statusSortValue(): Long
    protected abstract fun sparkLineData(telemetryType: TelemetryType): SparkLineData?

    fun currentValue(telemetryType: TelemetryType): Long =
        sparkLineData(telemetryType)?.currentValue ?: Long.MIN_VALUE

    fun addChild(child: VmTreeItem) {
        _children.add(child)
    }

    fun getScaledMax(telemetryType: TelemetryType): Double =
        sparkLineData(telemetryType)?.scaledMaxDisplayValue ?: 0.0

    fun setGraphMax(telemetryType: TelemetryType, max: Double) {
        graphMax[telemetryType] = max
    }

    fun sparklineState(telemetryType: TelemetryType): SparklineState {
        val data = sparkLineData(telemetryType) ?: return SparklineState.empty()
        return SparklineState.forVm(data, graphMax[telemetryType] ?: data.scaledMaxDisplayValue)
    }

    override fun equals(other: Any?): Boolean = other is VmTreeItem && key == other.key

    override fun hashCode(): Int = key.hashCode()
}
