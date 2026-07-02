package com.jvmguard.collector.vmdata.structures

import com.jvmguard.agent.util.Util
import com.jvmguard.data.base.Interval

class TimeAggregatingSparklineList(capacity: Int, dataInterval: Interval, minMax: Boolean) :
    SparklineList(
        dataInterval,
        if (minMax) RevolvingMinMaxLongList(capacity, Long.MIN_VALUE) else RevolvingLongList(capacity, Long.MIN_VALUE),
    ) {

    private var lastDataPointNanoTime: Long = 0

    constructor(capacity: Int, dataInterval: Interval) : this(capacity, dataInterval, true)

    override fun addValue(value: Long, nanoTime: Long): Boolean {
        if (lastDataPointNanoTime == 0L) {
            lastDataPointNanoTime = nanoTime
        }
        val dataPointsPassed = ((nanoTime - lastDataPointNanoTime) / 1000 / 1000 / dataInterval).toInt()
        if (dataPointsPassed > 0) {
            val list = backingList
            if (count == 0) {
                list.add(Long.MIN_VALUE)
            } else if (list is RevolvingMinMaxLongList) {
                list.add(Util.divideAndRoundUpToOne(currentSum, count.toLong()), currentMinValue, currentMaxValue)
            } else {
                list.add(Util.divideAndRoundUpToOne(currentSum, count.toLong()))
            }
            (1 until dataPointsPassed).forEach { _ ->
                list.add(Long.MIN_VALUE)
            }
            clearCurrent()
            lastDataPointNanoTime += dataInterval * dataPointsPassed * 1000 * 1000
        }

        super.addValue(value, nanoTime)
        return dataPointsPassed > 0
    }

    override fun initTime(nanoTime: Long) {
        lastDataPointNanoTime = nanoTime
    }

    override fun getDataPointPassed(nanoTime: Long): Int {
        var dataPointsPassed = ((nanoTime - lastDataPointNanoTime) / 1000 / 1000 / dataInterval).toInt()
        if (dataPointsPassed == 0 && nanoTime != lastDataPointNanoTime) {
            dataPointsPassed = 1
        }
        return dataPointsPassed
    }

    fun init(dayValues: LongArray, dayMin: LongArray, dayMax: LongArray, nanoTime: Long) {
        initTime(nanoTime)
        for (i in dayValues.indices) {
            (backingList as RevolvingMinMaxLongList).add(dayValues[i], dayMin[i], dayMax[i])
        }
    }
}
