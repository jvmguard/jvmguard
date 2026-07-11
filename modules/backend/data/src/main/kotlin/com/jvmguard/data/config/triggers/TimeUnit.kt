package com.jvmguard.data.config.triggers

import com.jvmguard.agent.config.base.ConfigDoc

enum class TimeUnit(
    private val verbose: String,
    private val singular: String,
    private val plural: String,
    private val secondMultiplier: Int,
) {
    @ConfigDoc("Seconds.")
    SECONDS("seconds", "second", "seconds", 1),
    @ConfigDoc("Minutes.")
    MINUTES("minutes", "minute", "minutes", 60),
    @ConfigDoc("Hours.")
    HOURS("hours", "hour", "hours", 60 * 60);

    fun getMillis(time: Int): Long = getSeconds(time).toLong() * 1000

    fun getNanos(time: Int): Long = getMillis(time) * 1000 * 1000

    fun getSeconds(time: Int): Int = time * secondMultiplier

    fun getName(number: Int): String = if (number > 1) plural else singular

    override fun toString(): String = verbose
}
