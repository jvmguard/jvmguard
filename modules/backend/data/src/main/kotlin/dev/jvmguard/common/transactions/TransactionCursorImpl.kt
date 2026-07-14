package dev.jvmguard.common.transactions

import dev.jvmguard.common.helper.Direction
import dev.jvmguard.data.transactions.TransactionCursor
import dev.jvmguard.data.transactions.TransactionDataType
import dev.jvmguard.data.transactions.TransactionTreeInterval
import dev.jvmguard.data.vmdata.VM

enum class DataAvailability(val isAvailable: Boolean) {
    FALSE(false),
    TRUE(true),
}

open class TransactionCursorImpl : TransactionCursor, Cloneable {

    override var availability = DataAvailability.TRUE
    override var isLatest = false
    override var startTime: Long = 0
    private var intervalValue: TransactionTreeInterval? = null
    lateinit var transactionDataType: TransactionDataType
    override var vm: VM? = null
    var vmNodes = VmNodes.NONE

    var isGroupData = false
        set(value) {
            field = value
            if (value) {
                isGroupNaming = true
            }
        }
    var isGroupNaming = false

    override var gap: Long = 0

    override var interval: TransactionTreeInterval
        get() = intervalValue!!
        set(value) {
            intervalValue = value
        }

    public override fun clone(): TransactionCursorImpl =
        super.clone() as TransactionCursorImpl

    override fun toString(): String =
        "TransactionCursorImpl{" +
                "dataAvailable=" + availability.isAvailable +
                ", latest=" + isLatest +
                ", startTime=" + startTime +
                ", interval=" + intervalValue +
                ", vm=" + vm +
                '}'

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }

        val that = other as TransactionCursorImpl

        if (availability.isAvailable != that.availability.isAvailable) {
            return false
        }
        if (isLatest != that.isLatest) {
            return false
        }
        if (startTime != that.startTime) {
            return false
        }
        if (intervalValue != that.intervalValue) {
            return false
        }
        if (transactionDataType != that.transactionDataType) {
            return false
        }
        return vm == that.vm
    }

    override fun hashCode(): Int {
        var result = if (availability.isAvailable) 1 else 0
        result = 31 * result + (if (isLatest) 1 else 0)
        result = 31 * result + java.lang.Long.hashCode(startTime)
        result = 31 * result + intervalValue!!.hashCode()
        result = 31 * result + transactionDataType.hashCode()
        result = 31 * result + (vm?.hashCode() ?: 0)
        return result
    }

    fun updateGap(direction: Direction, previousCursor: TransactionCursorImpl): TransactionCursor {
        gap = 0
        if (availability.isAvailable && previousCursor.availability.isAvailable) {
            val distance = if (direction == Direction.NEXT) {
                startTime - previousCursor.startTime
            } else {
                previousCursor.startTime - startTime
            }

            if (distance > intervalValue!!.timeExtent) {
                gap = distance - intervalValue!!.timeExtent
            }
        }

        return this
    }

    enum class VmNodes(val isComplete: Boolean) {
        NONE(false),
        GROUP(true),
    }
}
