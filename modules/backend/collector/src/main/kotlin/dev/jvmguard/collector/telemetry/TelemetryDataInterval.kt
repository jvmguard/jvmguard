package dev.jvmguard.collector.telemetry

import dev.jvmguard.common.helper.Timestamp.Interval
import dev.jvmguard.data.vmdata.Telemetry
import dev.jvmguard.data.vmdata.TelemetryInterval

enum class TelemetryDataInterval(
    val timestampInterval: Interval,
    storageHours: Int,
    private val database: Boolean,
) {
    TEN_SECONDS(Interval.TEN_SECONDS, 6, false),
    MINUTE(Interval.MINUTE, 48, false),
    TWO_MINUTES(Interval.TWO_MINUTES, 10 * 24, true),
    HOUR(Interval.HOUR, 366 * 24, true),
    TWELVE_HOURS(Interval.TWELVE_HOURS, Integer.MAX_VALUE, true);

    private val defaultStorageHours: Int = storageHours

    // The built-in default, optionally overridden at startup
    @Volatile
    var storageMillis: Long = if (storageHours == Integer.MAX_VALUE) Long.MAX_VALUE else storageHours.toLong() * 60 * 60 * 1000
        private set

    val seconds: Int
        get() = timestampInterval.seconds

    val millis: Long
        get() = timestampInterval.millis

    val storedPoints: Int
        get() = (storageMillis / millis).toInt()

    val tableName: String
        get() = "telemetry_data_$seconds"

    fun getDisplayedPoints(displayInterval: TelemetryInterval): Int = (displayInterval.timeExtent / millis).toInt()

    companion object {
        private val myValues = entries.toTypedArray()

        private val databaseIntervals: Array<TelemetryDataInterval> = myValues.filter { it.database }.toTypedArray()

        private fun toMillis(hours: Int): Long =
            if (hours == Integer.MAX_VALUE) Long.MAX_VALUE else hours.toLong() * 60 * 60 * 1000

        fun applyStorageOverrides(hoursBySeconds: Map<Int, Int>) {
            for (interval in myValues) {
                val overrideHours = hoursBySeconds[interval.seconds]
                interval.storageMillis = toMillis(overrideHours ?: interval.defaultStorageHours)
            }
        }

        fun fromDisplayInterval(displayInterval: TelemetryInterval, mainId: String): TelemetryDataInterval =
            when (displayInterval) {
                TelemetryInterval.TEN_MINUTES, TelemetryInterval.TWENTY_MINUTES, TelemetryInterval.FORTY_MINUTES, TelemetryInterval.EIGHTY_MINUTES -> getRecordingInterval(
                    mainId
                )

                TelemetryInterval.THREE_HOURS, TelemetryInterval.SIX_HOURS, TelemetryInterval.TWELVE_HOURS, TelemetryInterval.ONE_DAY -> TWO_MINUTES
                TelemetryInterval.THREE_DAYS, TelemetryInterval.SIX_DAYS, TelemetryInterval.TWELVE_DAYS -> HOUR
                TelemetryInterval.THIRTY_DAYS, TelemetryInterval.SIXTY_DAYS, TelemetryInterval.ONE_HUNDRED_EIGHTY_DAYS -> TWELVE_HOURS
            }

        fun getRecordingInterval(mainId: String): TelemetryDataInterval =
            if (Telemetry.TRANSACTIONS.mainId == mainId) MINUTE else TEN_SECONDS

        fun getDatabaseIntervals(): Array<TelemetryDataInterval> = databaseIntervals
    }
}
