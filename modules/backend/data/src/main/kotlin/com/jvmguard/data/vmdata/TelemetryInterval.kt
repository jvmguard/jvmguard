package com.jvmguard.data.vmdata

import com.jvmguard.data.base.TimeRangeInterval
import com.jvmguard.data.transactions.TransactionTreeInterval

enum class TelemetryInterval(
    private val verbose: String,
    override val timeExtent: Long,
    val exportId: String,
) : TimeRangeInterval {
    TEN_MINUTES("10 minutes", 10 * TimeHelper.MILLISECONDS_TO_MINUTE, "10min"),
    TWENTY_MINUTES("20 minutes", 20 * TimeHelper.MILLISECONDS_TO_MINUTE, "20min"),
    FORTY_MINUTES("40 minutes", 40 * TimeHelper.MILLISECONDS_TO_MINUTE, "40min"),
    EIGHTY_MINUTES("80 minutes", 80 * TimeHelper.MILLISECONDS_TO_MINUTE, "80min"),
    THREE_HOURS("3 hours", 3 * TimeHelper.MILLISECONDS_TO_HOUR, "3h"),
    SIX_HOURS("6 hours", 6 * TimeHelper.MILLISECONDS_TO_HOUR, "6h"),
    TWELVE_HOURS("12 hours", 12 * TimeHelper.MILLISECONDS_TO_HOUR, "12h"),
    ONE_DAY("1 day", 1 * TimeHelper.MILLISECONDS_TO_DAY, "1d"),
    THREE_DAYS("3 days", 3 * TimeHelper.MILLISECONDS_TO_DAY, "3d"),
    SIX_DAYS("6 days", 6 * TimeHelper.MILLISECONDS_TO_DAY, "6d"),
    TWELVE_DAYS("12 days", 12 * TimeHelper.MILLISECONDS_TO_DAY, "12d"),
    THIRTY_DAYS("30 days", 30 * TimeHelper.MILLISECONDS_TO_DAY, "30d"),
    SIXTY_DAYS("60 days", 60 * TimeHelper.MILLISECONDS_TO_DAY, "60d"),
    ONE_HUNDRED_EIGHTY_DAYS("180 days", 180 * TimeHelper.MILLISECONDS_TO_DAY, "180d");

    override val isAutoUpdateSupported: Boolean
        get() = true

    override fun toString(): String = verbose

    fun getTransactionTreeTimeLineInterval(): TransactionTreeInterval? {
        val transactionTreeIntervals = TransactionTreeInterval.entries
        for (i in transactionTreeIntervals.indices.reversed()) {
            val transactionTreeInterval = transactionTreeIntervals[i]
            val timeLineInterval = transactionTreeInterval.minimumTimeLineInterval
            if (timeLineInterval != null && timeLineInterval.ordinal <= ordinal) {
                return transactionTreeInterval
            }
        }
        return null
    }

    companion object {
        fun fromExportId(exportId: String): TelemetryInterval? =
            entries.firstOrNull { it.exportId == exportId }
    }
}
