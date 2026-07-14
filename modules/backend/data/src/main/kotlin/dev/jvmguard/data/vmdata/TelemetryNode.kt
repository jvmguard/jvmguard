package dev.jvmguard.data.vmdata

import dev.jvmguard.agent.config.telemetry.TelemetryUnit
import dev.jvmguard.data.config.FrequencyUnit
import java.math.BigDecimal
import java.math.RoundingMode

class TelemetryNode {

    var description: String = ""
    var isStackedData: Boolean = false
        private set

    var telemetryUnit: TelemetryUnit = TelemetryUnit.PLAIN
        private set
    var scale: Int = 0
        private set

    val data: MutableList<Data> = ArrayList()
    val children: MutableList<TelemetryNode> = ArrayList()

    var unitLabel: String = ""
        private set
    var unitLevels: Int = 0
        private set
    private var multiplier: Int = 1

    constructor()

    constructor(description: String, stackedData: Boolean) {
        this.description = description
        this.isStackedData = stackedData
    }

    fun setTelemetryUnit(telemetryUnit: TelemetryUnit, scale: Int) {
        this.telemetryUnit = telemetryUnit
        this.scale = scale
    }

    fun calculateUnitScale(frequencyUnit: FrequencyUnit): TelemetryNode {
        val maxValue = getMaxDisplayValue()
        if (telemetryUnit == TelemetryUnit.PER_SECOND) {
            unitLabel = frequencyUnit.label
            multiplier = frequencyUnit.multiplier
        } else {
            unitLevels = telemetryUnit.getUnitLevels(maxValue.toDouble())
            unitLabel = telemetryUnit.getLabel(unitLevels)
            multiplier = 1
        }
        return this
    }

    private fun getMaxDisplayValue(): BigDecimal {
        var maxValue = BigDecimal.valueOf(10)
        if (data.isNotEmpty()) {
            val dataPointCount = data.first().data?.size ?: 0
            for (i in 0 until dataPointCount) {
                var totalValue = 0L
                for (singleData in data) {
                    val values = singleData.data
                    if (values != null && i < values.size) {
                        totalValue += values[i]
                        if (!isStackedData) {
                            val currentValue = plainScale(values[i])
                            if (currentValue != null && currentValue > maxValue) {
                                maxValue = currentValue
                            }
                        }
                    }
                }
                if (isStackedData) {
                    val currentValue = plainScale(totalValue)
                    if (currentValue != null && currentValue > maxValue) {
                        maxValue = currentValue
                    }
                }
            }
        }
        return maxValue
    }

    private fun unitScale(base: Long): BigDecimal? {
        if (base == Long.MIN_VALUE) {
            return null
        }
        return scaleInt(base * multiplier, unitLevels * 3)
    }

    private fun plainScale(base: Long): BigDecimal? = scaleInt(base, 0)

    private fun scaleInt(base: Long, unitScale: Int): BigDecimal? {
        if (base == Long.MIN_VALUE) {
            return null
        }
        var ret = BigDecimal.valueOf(base, scale + unitScale)
        if (scale + unitScale > 1) {
            ret = ret.setScale(1, RoundingMode.HALF_UP)
        }
        return ret
    }

    override fun toString(): String =
        "TelemetryNode{description='$description', data=$data, children=$children}"

    fun addData(description: String, subId: String, values: LongArray): Data {
        val ret = Data(description, subId, values)
        data.add(ret)
        return ret
    }

    fun addData(description: String, subId: String): Data {
        val ret = Data(description, subId)
        data.add(ret)
        return ret
    }

    inner class Data {
        val description: String
        val subId: String
        var data: LongArray?

        constructor(description: String, subId: String, data: LongArray?) {
            this.description = description
            this.subId = subId
            this.data = data
        }

        constructor(description: String, subId: String) {
            this.description = description
            this.subId = subId
            this.data = null
        }

        override fun toString(): String = "$description: ${data.contentToString()}"

        val unitScaledData: List<BigDecimal?>?
            get() {
                val values = data ?: return null
                return values.map { unitScale(it) }
            }

        val plainScaledData: List<BigDecimal?>?
            get() {
                val values = data ?: return null
                return values.map { plainScale(it) }
            }
    }

    companion object {
        const val CUSTOM_TELEMETRIES_TEXT: String = "Custom Telemetries"
    }
}
