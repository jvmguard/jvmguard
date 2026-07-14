package dev.jvmguard.data.vmdata

import dev.jvmguard.agent.config.telemetry.TelemetryUnit
import dev.jvmguard.data.config.FrequencyUnit
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import kotlin.math.pow

class SparkLineData : Comparable<SparkLineData> {

    val telemetryType: TelemetryType
    val frequencyUnit: FrequencyUnit

    private lateinit var values: LongArray
    var min: Long = Long.MAX_VALUE
        private set
    var max: Long = Long.MIN_VALUE
        private set

    private var scaleCalculated: Boolean = false
    private var maxDisplayValue: Long = Long.MIN_VALUE
    private var labelValue: String? = null
    private var divisorValue: Int = 1
    private var multiplierValue: Int = 1

    constructor(telemetryType: TelemetryType, frequencyUnit: FrequencyUnit, sparkLineRange: SparkLineRange) {
        this.telemetryType = telemetryType
        this.frequencyUnit = frequencyUnit
        init(sparkLineRange)
    }

    constructor(
        telemetryType: TelemetryType,
        frequencyUnit: FrequencyUnit,
        sparkLineRange: SparkLineRange,
        data: LongArray?,
        min: Long,
        max: Long,
    ) {
        this.telemetryType = telemetryType
        this.frequencyUnit = frequencyUnit
        this.min = min
        this.max = max
        val connectionsTelemetry = Telemetry.CONNECTIONS.mainId == telemetryType.telemetryIdentifier.mainId
        if (data == null) {
            init(sparkLineRange)
            if (connectionsTelemetry) {
                this.min = 0
            }
        } else {
            this.values = data
            if (connectionsTelemetry) {
                var i = 0
                while (i < data.size && this.min > 0) {
                    if (data[i] == Long.MIN_VALUE) {
                        this.min = 0
                        break
                    }
                    i++
                }
            }
        }
    }

    val scaledMin: Number
        get() {
            checkScaleCalculated()
            return scaleValue(min)
        }

    val scaledMax: Number
        get() {
            checkScaleCalculated()
            return scaleValue(max)
        }

    val scaledCurrent: Number
        get() {
            checkScaleCalculated()
            return scaleValue(currentValue)
        }

    val currentValue: Long
        get() = values[values.size - 1]

    val data: LongArray
        get() = values

    val scaledData: List<Number>
        get() {
            checkScaleCalculated()
            return values.map { scaleValue(it) }
        }

    private fun scaleValue(value: Long): Number = scaleValue(value, RoundingMode.HALF_UP)

    private fun scaleValue(value: Long, roundingMode: RoundingMode): Number {
        val unscaledValue =
            if (value > Long.MIN_VALUE && value < Long.MAX_VALUE) {
                divideAndRound(value * multiplierValue, divisorValue.toLong(), roundingMode)
            } else {
                0
            }
        return if (unscaledValue == 0L) {
            BigDecimal.valueOf(0, 0)
        } else if (scale != 0) {
            var scaledValue = BigDecimal.valueOf(unscaledValue, scale)
            if (scale > 1) {
                scaledValue = scaledValue.setScale(1, roundingMode)
            }
            scaledValue
        } else {
            unscaledValue
        }
    }

    private val scale: Int
        get() = telemetryType.scale

    val scaledMaxDisplayValue: Double
        get() {
            checkScaleCalculated()
            return scaleValue(maxDisplayValue, RoundingMode.UP).toDouble()
        }

    val multiplier: Int
        get() {
            checkScaleCalculated()
            return multiplierValue
        }

    val divisor: Int
        get() {
            checkScaleCalculated()
            return divisorValue
        }

    val label: String?
        get() {
            checkScaleCalculated()
            return labelValue
        }

    private fun checkScaleCalculated() {
        if (scaleCalculated) {
            return
        }
        maxDisplayValue = 0
        for (value in values) {
            maxDisplayValue = maxOf(maxDisplayValue, value)
        }
        val telemetryUnit = telemetryType.unit
        if (telemetryUnit == TelemetryUnit.PER_SECOND) {
            multiplierValue = frequencyUnit.multiplier
            labelValue = frequencyUnit.label
        } else {
            val unitLevels =
                telemetryUnit.getUnitLevels(maxOf(maxDisplayValue, max).toDouble() * 10.0.pow(-telemetryType.scale.toDouble()))
            divisorValue = 10.0.pow((3 * unitLevels).toDouble()).toInt()
            labelValue = telemetryUnit.getLabel(unitLevels)
        }
        scaleCalculated = true
    }

    private fun init(sparkLineRange: SparkLineRange) {
        values = LongArray(sparkLineRange.numberOfPoints)
        Arrays.fill(values, Long.MIN_VALUE)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val that = other as SparkLineData
        return values.contentEquals(that.values)
    }

    override fun hashCode(): Int = values.contentHashCode()

    override fun compareTo(other: SparkLineData): Int =
        currentValue.compareTo(other.currentValue)

    override fun toString(): String =
        "SparkLineData{telemetryType=$telemetryType, frequencyUnit=$frequencyUnit, " +
                "data=${values.contentToString()}, min=$min, max=$max, scaleCalculated=$scaleCalculated, " +
                "maxDisplayValue=$maxDisplayValue, label='$labelValue', divisor=$divisorValue, multiplier=$multiplierValue}"

    companion object {
        private fun divideAndRound(value: Long, divisor: Long, roundingMode: RoundingMode): Long {
            if (divisor == 1L) {
                return value
            }
            val remainder = value % divisor
            return if (roundingMode == RoundingMode.UP) {
                value / divisor + (if (remainder > 0) 1 else 0)
            } else {
                value / divisor + (if (remainder >= divisor / 2) 1 else 0)
            }
        }
    }
}
