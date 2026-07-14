package dev.jvmguard.data.vmdata

object TimeHelper {
    const val MILLISECONDS_TO_SECOND: Long = 1000
    const val MILLISECONDS_TO_MINUTE: Long = MILLISECONDS_TO_SECOND * 60
    const val MILLISECONDS_TO_HOUR: Long = MILLISECONDS_TO_MINUTE * 60
    const val HOURS_IN_DAY: Int = 24
    const val DAYS_IN_WEEK: Int = 7
    const val HOURS_IN_WEEK: Int = HOURS_IN_DAY * DAYS_IN_WEEK
    const val MILLISECONDS_TO_DAY: Long = MILLISECONDS_TO_HOUR * HOURS_IN_DAY
}
