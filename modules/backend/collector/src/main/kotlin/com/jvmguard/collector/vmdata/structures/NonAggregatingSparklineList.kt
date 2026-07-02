package com.jvmguard.collector.vmdata.structures

import com.jvmguard.agent.util.Util
import com.jvmguard.data.base.Interval

class NonAggregatingSparklineList(capacity: Int, dataInterval: Interval) :
    SparklineList(dataInterval, RevolvingLongList(capacity, Long.MIN_VALUE)) {

    override fun addValue(value: Long, nanoTime: Long): Boolean {
        super.addValue(value, nanoTime)
        commitValue()
        return true
    }

    fun commitValue(): Long {
        val committedValue = if (count == 0) {
            Long.MIN_VALUE
        } else {
            Util.divideAndRoundUpToOne(currentSum, count.toLong())
        }
        backingList.add(committedValue)
        clearCurrent()
        return committedValue
    }
}
