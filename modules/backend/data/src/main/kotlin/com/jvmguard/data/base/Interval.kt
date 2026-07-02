package com.jvmguard.data.base

enum class Interval(seconds: Int) {
    MINUTE(60),
    HALF_AN_HOUR(60 * 30),
    HOUR(60 * 60),
    DAY(60 * 60 * 24);

    val millis: Long = seconds * 1000L

    val nanos: Long
        get() = millis * 1000 * 1000

}
