package dev.jvmguard.collector.vmdata.structures

class RevolvingMinMaxLongList(capacity: Int, initialValue: Long) : RevolvingLongList(capacity, initialValue) {

    private val minValues: LongArray = initArray(capacity, Long.MAX_VALUE)
    private val maxValues: LongArray = initArray(capacity, Long.MIN_VALUE)

    override fun add(v: Long): Boolean {
        return add(v, Long.MAX_VALUE, Long.MIN_VALUE)
    }

    fun add(value: Long, min: Long, max: Long): Boolean {
        minValues[getPosition()] = min
        maxValues[getPosition()] = max
        return super.add(value)
    }

    override fun getMin(index: Int): Long {
        return minValues[getIndex(index)]
    }

    override fun getMax(index: Int): Long {
        return maxValues[getIndex(index)]
    }
}
