package dev.jvmguard.data.vmdata

import dev.jvmguard.agent.config.telemetry.TelemetryUnit
import java.math.BigDecimal

class TelemetryType private constructor(
    val telemetryIdentifier: PersistentTelemetryIdentifier,
    val categoryName: String,
    val name: String,
    val unit: TelemetryUnit,
    val scale: Int,
) : Cloneable {

    var isVisible: Boolean = true
        private set

    // main and sub id must be unique together
    constructor(
        mainId: String,
        subId: String?,
        additionalType: Int,
        additionalName: String,
        categoryName: String,
        name: String,
        unit: TelemetryUnit,
        scale: Int,
    ) : this(
        PersistentTelemetryIdentifier(mainId, subId, additionalType, additionalName),
        categoryName,
        name,
        unit,
        scale,
    )

    constructor(
        mainId: String,
        subId: String?,
        categoryName: String,
        name: String,
        unit: TelemetryUnit,
        scale: Int,
    ) : this(
        mainId,
        subId,
        PersistentTelemetryIdentifier.DEFAULT_ADDITIONAL_TYPE,
        PersistentTelemetryIdentifier.DEFAULT_ADDITIONAL_NAME,
        categoryName,
        name,
        unit,
        scale,
    )

    val searchSubIdForTelemetry: String
        get() {
            val subId = telemetryIdentifier.subId
            val mainId = telemetryIdentifier.mainId
            return if (Telemetry.TRANSACTIONS.mainId == mainId && SUB_ID_COMPLETED == subId) {
                // "Completed" is not present in telemetry
                SUB_ID_NORMAL
            } else if (Telemetry.HEAP.mainId == mainId && SUB_ID_USED_HEAP_PERCENTAGE == subId) {
                // "Percentage" is not present in telemetry
                SUB_ID_USED_HEAP
            } else {
                subId
            }
        }

    fun visible(visible: Boolean): TelemetryType {
        this.isVisible = visible
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val that = other as TelemetryType
        return telemetryIdentifier == that.telemetryIdentifier
    }

    override fun hashCode(): Int = telemetryIdentifier.hashCode()

    override fun toString(): String =
        "TelemetryType{id='$telemetryIdentifier', name='$name'}"

    fun scaleThreshold(bound: Long, unitLevel: Int): Long {
        var result = bound
        if (scale != 0) {
            result = BigDecimal.valueOf(result, -scale).toLong()
        }
        if (unit == TelemetryUnit.PER_SECOND) {
            if (unitLevel == 1) { // per minute
                result /= 60
            } else if (unitLevel == 2) { // per hour
                result /= 3600
            }
        } else {
            if (unitLevel > 0 && unitLevel <= UNIT_FACTORS.size) {
                result *= UNIT_FACTORS[unitLevel]
            }
        }
        return result
    }

    public override fun clone(): TelemetryType = super.clone() as TelemetryType

    companion object {
        const val SUB_ID_USED_HEAP: String = "u"
        const val SUB_ID_FREE_HEAP: String = "f"
        const val SUB_ID_ADDITIONAL_PREFIX: String = "a" // telemetry only
        const val SUB_ID_USED_HEAP_PERCENTAGE: String = "p" // sparkline only

        const val SUB_ID_AVERAGE: String = "a"
        const val SUB_ID_NORMAL: String = "n"
        const val SUB_ID_SLOW: String = "s"
        const val SUB_ID_VERY_SLOW: String = "v"
        const val SUB_ID_ERROR: String = "e"
        const val SUB_ID_COMPLETED: String = "t" // sparkline only, excludes overdue

        private val UNIT_FACTORS = LongArray(6).also { factors ->
            for (i in factors.indices) {
                factors[i] = if (i > 0) factors[i - 1] * 1000 else 1
            }
        }
    }
}
