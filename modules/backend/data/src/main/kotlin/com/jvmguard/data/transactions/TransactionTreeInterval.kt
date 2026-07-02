package com.jvmguard.data.transactions

import com.jvmguard.common.helper.Timestamp
import com.jvmguard.common.helper.Timestamp.Interval
import com.jvmguard.data.base.TimeRangeInterval
import com.jvmguard.data.vmdata.TelemetryInterval

enum class TransactionTreeInterval : TimeRangeInterval {
    MINUTE(TransactionDataInterval.MINUTE, "1 minute", true, TelemetryInterval.FORTY_MINUTES, "1min"),
    TEN_MINUTE(Interval.TEN_MINUTES, "10 minutes", true, TelemetryInterval.THREE_HOURS, "10min"),
    HOUR(TransactionDataInterval.HOUR, "1 hour", false, TelemetryInterval.ONE_DAY, "1h"),
    DAY(TransactionDataInterval.DAY, "1 day", false, TelemetryInterval.TWELVE_DAYS, "1d");

    val timestampInterval: Interval
    private val verbose: String
    private val autoFollowSupported: Boolean
    val minimumTimeLineInterval: TelemetryInterval?
    val exportId: String

    private var sameLengthDataInterval: TransactionDataInterval? = null

    constructor(
        sameLengthDataInterval: TransactionDataInterval,
        verbose: String,
        autoFollowSupported: Boolean,
        minimumTimeLineInterval: TelemetryInterval?,
        exportId: String,
    ) {
        this.sameLengthDataInterval = sameLengthDataInterval
        this.exportId = exportId
        this.timestampInterval = sameLengthDataInterval.timestampInterval
        this.verbose = verbose
        this.autoFollowSupported = autoFollowSupported
        this.minimumTimeLineInterval = minimumTimeLineInterval
    }

    constructor(
        timestampInterval: Interval,
        verbose: String,
        autoFollowSupported: Boolean,
        minimumTimeLineInterval: TelemetryInterval?,
        exportId: String,
    ) {
        this.timestampInterval = timestampInterval
        this.verbose = verbose
        this.autoFollowSupported = autoFollowSupported
        this.minimumTimeLineInterval = minimumTimeLineInterval
        this.exportId = exportId
    }

    fun getFloorStartTime(time: Long): Long {
        if (time == 0L || time == Long.MAX_VALUE || time == Long.MIN_VALUE) {
            return time
        }
        return sameLengthDataInterval?.getFloorStartTime(time)
            ?: Timestamp.floor(time, timestampInterval)
    }

    fun getNextStartTime(time: Long): Long {
        if (time == 0L || time == Long.MAX_VALUE || time == Long.MIN_VALUE) {
            return time
        }
        return sameLengthDataInterval?.getNextStartTime(time)
            ?: (Timestamp.floor(time, timestampInterval) + timestampInterval.millis)
    }

    fun getPreviousStartTime(time: Long): Long {
        if (time == 0L || time == Long.MAX_VALUE || time == Long.MIN_VALUE) {
            return time
        }
        return sameLengthDataInterval?.getPreviousStartTime(time)
            ?: (getFloorStartTime(time) - timestampInterval.millis)
    }

    override val timeExtent: Long
        get() = timestampInterval.millis

    override val isAutoUpdateSupported: Boolean
        get() = autoFollowSupported

    override fun toString(): String = verbose

    companion object {
        fun fromExportId(string: String): TransactionTreeInterval? =
            entries.firstOrNull { it.exportId == string }
    }
}
