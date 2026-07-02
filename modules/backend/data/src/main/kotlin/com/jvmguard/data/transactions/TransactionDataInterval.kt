package com.jvmguard.data.transactions

import com.jvmguard.common.helper.Timestamp
import com.jvmguard.common.helper.Timestamp.Interval
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

enum class TransactionDataInterval(val timestampInterval: Interval) {
    MINUTE(Interval.MINUTE),
    HOUR(Interval.HOUR) {
        override fun getFloorStartTime(time: ZonedDateTime): Long {
            return if (time.toInstant().toEpochMilli() == 0L) {
                super.getFloorStartTime(time)
            } else {
                time.truncatedTo(ChronoUnit.HOURS).toInstant().toEpochMilli()
            }
        }
    },
    DAY(Interval.DAY) {
        override fun getFloorStartTime(time: ZonedDateTime): Long {
            return if (time.toInstant().toEpochMilli() == 0L) {
                super.getFloorStartTime(time)
            } else {
                time.toLocalDate().atStartOfDay(time.zone).toInstant().toEpochMilli()
            }
        }
    };

    var previousInterval: TransactionDataInterval? = null
        private set

    val seconds: Int
        get() = timestampInterval.seconds

    val millis: Long
        get() = timestampInterval.millis

    open fun getFloorStartTime(time: ZonedDateTime): Long =
        Timestamp.floor(time.toInstant().toEpochMilli(), timestampInterval)

    fun getFloorStartTime(time: Long): Long = getFloorStartTime(atDefaultZone(time))

    fun getPossibleStartTime(previous: ZonedDateTime, current: ZonedDateTime): Long {
        val previousStartTime = getFloorStartTime(previous)
        return if (getFloorStartTime(current) != previousStartTime) {
            previousStartTime
        } else {
            0
        }
    }

    fun getTableName(transactionDataType: TransactionDataType): String =
        (transactionDataType.name + "_" + name).lowercase()

    fun getNextStartTime(lastTime: Long): Long {
        if (lastTime == 0L || lastTime == Long.MAX_VALUE || lastTime == Long.MIN_VALUE) {
            return 0
        }
        var time = lastTime + millis
        var nextTime = getFloorStartTime(time)
        while (nextTime == lastTime) {
            time += millis / 10
            nextTime = getFloorStartTime(time)
        }
        return nextTime
    }

    fun getPreviousStartTime(lastTime: Long): Long {
        if (lastTime == 0L || lastTime == Long.MAX_VALUE || lastTime == Long.MIN_VALUE) {
            return 0
        }
        val previousTime = getFloorStartTime(lastTime)
        var usedTime = previousTime
        var result = previousTime
        while (result == lastTime) {
            usedTime -= millis / 10
            result = getFloorStartTime(usedTime)
        }
        return result
    }

    fun isSameLength(displayInterval: TransactionTreeInterval): Boolean =
        timestampInterval == displayInterval.timestampInterval

    fun getNanos(): Long = millis * 1000 * 1000

    class IntervalWithTimestamp(
        val interval: TransactionDataInterval,
        val time: Long,
    )

    class UsedIntervals(
        val largeInterval: TransactionDataInterval,
        val smallInterval: TransactionDataInterval?,
    ) {
        val smallestInterval: TransactionDataInterval
            get() = smallInterval ?: largeInterval
    }

    companion object {
        private val myValues = entries.toTypedArray()
        private val calculatedIntervals: Array<TransactionDataInterval> = myValues.copyOfRange(1, myValues.size)

        init {
            for (i in 1 until myValues.size) {
                myValues[i].previousInterval = myValues[i - 1]
            }
        }

        fun getUsedIntervals(
            displayInterval: TransactionTreeInterval,
            group: Boolean,
            latest: Boolean,
        ): UsedIntervals =
            when (displayInterval) {
                TransactionTreeInterval.MINUTE, TransactionTreeInterval.TEN_MINUTE -> UsedIntervals(MINUTE, null)
                TransactionTreeInterval.HOUR -> UsedIntervals(if (latest) MINUTE else HOUR, null)
                TransactionTreeInterval.DAY ->
                    if (latest) UsedIntervals(HOUR, if (group) MINUTE else null) else UsedIntervals(DAY, null)
            }

        fun getCalculatedIntervals(): Array<TransactionDataInterval> = calculatedIntervals

        fun getRecordingInterval(): TransactionDataInterval = myValues[0]

        fun getPossibleDatabaseStartTimes(
            previous: ZonedDateTime,
            current: ZonedDateTime,
        ): List<IntervalWithTimestamp>? {
            var ret: MutableList<IntervalWithTimestamp>? = null
            for (databaseInterval in calculatedIntervals) {
                val timestamp = databaseInterval.getPossibleStartTime(previous, current)
                if (timestamp > 0) {
                    if (ret == null) {
                        ret = ArrayList()
                    }
                    ret.add(IntervalWithTimestamp(databaseInterval, timestamp))
                }
            }
            return ret
        }

        private fun atDefaultZone(millis: Long): ZonedDateTime =
            Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
    }
}
