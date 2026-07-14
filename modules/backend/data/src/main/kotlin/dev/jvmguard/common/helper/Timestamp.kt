package dev.jvmguard.common.helper

class Timestamp(val time: Long) {

    constructor() : this(System.currentTimeMillis())

    fun floor(interval: Interval): Long = floor(time, interval)

    /*
       telemetry data intervals:
       TEN_SECONDS
       MINUTE
       TWO_MINUTES
       TEN_MINUTES
       HOUR
       TWELVE_HOURS
       DAY
     */
    enum class Interval(seconds: Int) {
        TEN_SECONDS(10),
        MINUTE(60),
        TWO_MINUTES(60 * 2),
        TEN_MINUTES(60 * 10),
        HOUR(60 * 60),
        TWELVE_HOURS(60 * 60 * 12),
        DAY(60 * 60 * 24);

        val millis: Long = (seconds * 1000).toLong()

        val seconds: Int
            get() = (millis / 1000).toInt()

    }

    companion object {
        fun floor(time: Long, interval: Interval): Long =
            time / interval.millis * interval.millis
    }
}
