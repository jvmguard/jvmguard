package com.jvmguard.integration.util

fun interface TimeComparator {

    fun isEqual(time1: Long, count1: Long, time2: Long, count2: Long): Boolean

    companion object {
        val NONE = TimeComparator { _, _, _, _ -> true }

        val THIRTY_PERCENT = TimeComparator { time1, _, time2, _ ->
            time1 == -1L || time2 == -2L || (time1 <= time2 * 1.3 && time1 >= time2 * 0.7)
        }

        val FIFTY_PERCENT = TimeComparator { time1, _, time2, _ ->
            time1 == -1L || time2 == -2L || (time1 <= time2 * 1.5 && time1 >= time2 * 0.5)
        }
    }
}
