package dev.jvmguard.collector.vmdata.structures

import dev.jvmguard.data.base.Interval

abstract class SparklineList protected constructor(dataInterval: Interval, protected val backingList: RevolvingLongList) {

    protected val dataInterval: Long = dataInterval.millis

    protected var currentSum: Long = 0
    protected var count: Int = 0
    protected var lastValue: Long = Long.MIN_VALUE

    protected var currentMinValue: Long = Long.MAX_VALUE
    protected var currentMaxValue: Long = Long.MIN_VALUE

    open fun addValue(value: Long, nanoTime: Long): Boolean {
        if (value > Long.MIN_VALUE) {
            currentSum += value
            count++

            if (value < currentMinValue) {
                currentMinValue = value
            }
            if (value > currentMaxValue) {
                currentMaxValue = value
            }
        }
        lastValue = value
        return false
    }

    protected fun clearCurrent() {
        currentSum = 0
        count = 0
        currentMinValue = Long.MAX_VALUE
        currentMaxValue = Long.MIN_VALUE
    }

    fun getCurrentUnscaledData(nanoTime: Long): CurrentData {
        val result = CurrentData(backingList.size, currentMinValue, currentMaxValue)
        val dataPointsPassed = getDataPointPassed(nanoTime)

        var index = 0
        for (i in dataPointsPassed until backingList.size) {
            result.data[index++] = backingList.getLong(i)
            val currentMin = backingList.getMin(i)
            if (currentMin != Long.MIN_VALUE && currentMin < result.min) {
                result.min = currentMin
            }
            val currentMax = backingList.getMax(i)
            if (currentMax != Long.MAX_VALUE && currentMax > result.max) {
                result.max = currentMax
            }
        }

        if (dataPointsPassed == 1) {
            result.data[index] = lastValue
        } else if (dataPointsPassed > 0) {
            var i = 0
            while (i < dataPointsPassed && index < result.data.size) {
                result.data[index++] = Long.MIN_VALUE
                i++
            }
        }
        return result
    }

    protected open fun getDataPointPassed(nanoTime: Long): Int {
        return 0
    }

    protected open fun initTime(nanoTime: Long) {
    }

    fun init(externalData: LongArray, nanoTime: Long) {
        initTime(nanoTime)
        val list = backingList
        if (list is RevolvingMinMaxLongList) {
            for (value in externalData) {
                list.add(value, value, value)
            }
        } else {
            for (value in externalData) {
                list.add(value)
            }
        }
    }

    class CurrentData(length: Int, var min: Long, var max: Long) {
        val data: LongArray = LongArray(length)
    }
}
