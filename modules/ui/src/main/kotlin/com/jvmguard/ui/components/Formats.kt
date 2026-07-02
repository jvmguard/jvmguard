package com.jvmguard.ui.components

import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.abs

object Formats {

    private val DATE_TIME = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss")
    private val DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun dateTime(instant: Instant?, fallback: String = "—"): String =
        instant?.let { DATE_TIME.format(it.atZone(ZoneId.systemDefault())) } ?: fallback

    fun date(instant: Instant?, fallback: String = ""): String =
        instant?.let { DATE.format(it.atZone(ZoneId.systemDefault())) } ?: fallback

    private const val MICROSECOND = 1_000L
    private const val MILLISECOND = 1_000 * MICROSECOND
    private const val SECOND = 1_000 * MILLISECOND
    private const val MINUTE = 60 * SECOND
    private const val HOUR = 60 * MINUTE

    private const val SCALE_THRESHOLD = 100

    private val grouping = NumberFormat.getIntegerInstance(Locale.US)

    /** Formats a nanosecond duration, scaling to the largest fitting unit (µs / ms / s / m / h). */
    fun time(nanos: Long): String {
        val abs = abs(nanos)
        return when {
            abs > SCALE_THRESHOLD * HOUR -> "${nanos / HOUR} h"
            abs > SCALE_THRESHOLD * MINUTE -> "${nanos / MINUTE} m"
            abs > SCALE_THRESHOLD * SECOND -> "${nanos / SECOND} s"
            abs > SCALE_THRESHOLD * MILLISECOND -> "${nanos / MILLISECOND} ms"
            else -> "${nanos / MICROSECOND} µs"
        }
    }

    /** Formats an invocation count with grouping separators. */
    fun count(value: Long): String = grouping.format(value)
}
