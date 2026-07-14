package dev.jvmguard.collector.transactions.util

import kotlin.math.roundToInt
import kotlin.math.roundToLong

class Percentage {
    private var minIntervalPercentageValue = 100
    private var maxIntervalPercentageValue = 0

    var totalTime = 0L
        private set

    fun addPart(realInterval: Long, expectedInterval: Long) {
        totalTime += realInterval
        val currentPercentage = if (realInterval == 0L) 0.0 else realInterval.toDouble() / expectedInterval.toDouble()
        var intPercentage = ((currentPercentage * 10).roundToLong() * 10).toInt()
        if (currentPercentage > 0 && intPercentage == 0) {
            intPercentage = (currentPercentage * 100).roundToInt()
        }
        if (realInterval > 0 && intPercentage == 0) {
            intPercentage = 1
        }
        if (currentPercentage > maxIntervalPercentageValue) {
            maxIntervalPercentageValue = intPercentage
        }
        if (currentPercentage < minIntervalPercentageValue) {
            minIntervalPercentageValue = intPercentage
        }
    }

    val minIntervalPercentage: Int
        get() = if (minIntervalPercentageValue >= 95) 100 else minIntervalPercentageValue

    val maxIntervalPercentage: Int
        get() = if (maxIntervalPercentageValue >= 95) 100 else maxIntervalPercentageValue

    override fun toString(): String =
        "Percentage{minIntervalPercentage=$minIntervalPercentageValue, maxIntervalPercentage=$maxIntervalPercentageValue, totalTime=$totalTime}"
}
