package dev.jvmguard.data.vmdata

import dev.jvmguard.data.config.FrequencyUnit

class VmDataHolder(
    val vm: VM,
    var isConnected: Boolean,
    val isOutdatedAgent: Boolean,
    val statusChangeTime: Long,
    private val sparkLineRange: SparkLineRange,
    val frequencyUnit: FrequencyUnit,
    val hostName: String,
    val port: Int,
) : Comparable<Any?> {

    private val data = HashMap<TelemetryType, SparkLineData>()

    fun addSparkLineData(telemetryType: TelemetryType, sparkLineData: SparkLineData) {
        data[telemetryType] = sparkLineData
    }

    fun getSparkLineData(telemetryType: TelemetryType): SparkLineData =
        data.computeIfAbsent(telemetryType) { SparkLineData(it, frequencyUnit, sparkLineRange) }

    override fun compareTo(other: Any?): Int {
        if (other !is VmDataHolder) {
            return -1
        }
        return vm.compareTo(other.vm)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is VmDataHolder) {
            return false
        }
        return vm == other.vm
    }

    override fun hashCode(): Int = vm.hashCode()

    override fun toString(): String =
        "VmDataHolder{vm=$vm, connected=$isConnected, statusChangeTime=$statusChangeTime}"
}
