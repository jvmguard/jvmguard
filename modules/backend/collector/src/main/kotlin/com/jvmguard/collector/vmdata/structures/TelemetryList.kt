package com.jvmguard.collector.vmdata.structures

import com.jvmguard.agent.util.Util

class TelemetryList(capacity: Int) : RevolvingLongList(capacity, Long.MIN_VALUE) {

    private var currentSum: Long = 0
    private var count: Int = 0

    var fillCount: Int = 0
        private set

    fun getAggregatedValue(dataPointCount: Int): Long {
        var sum: Long = 0
        var count = 0
        for (i in size - dataPointCount until size) {
            val value = getLong(i)
            if (value > Long.MIN_VALUE) {
                sum += value
                count++
            }
        }
        return if (count == 0) {
            Long.MIN_VALUE
        } else {
            Util.divideAndRoundUpToOne(sum, count.toLong())
        }
    }

    fun setNextValue(value: Long) {
        if (value > Long.MIN_VALUE) {
            currentSum += value
            count++
        }
    }

    fun commitValue(averaged: Boolean): Long {
        if (fillCount < size) {
            fillCount++
        }
        val committedValue = getCommittedValue(averaged, currentSum, count)
        add(committedValue)
        currentSum = 0
        count = 0
        return committedValue
    }

    companion object {
        fun getCommittedValue(averaged: Boolean, currentSum: Long, count: Int): Long = when {
            count == 0 -> Long.MIN_VALUE
            averaged -> Util.divideAndRoundUpToOne(currentSum, count.toLong())
            else -> currentSum
        }
    }
}
