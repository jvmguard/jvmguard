package com.jvmguard.collector.vmdata.structures

import it.unimi.dsi.fastutil.longs.AbstractLongList
import java.util.Arrays

open class RevolvingLongList(capacity: Int, initialValue: Long) : AbstractLongList() {

    private val data: LongArray = initArray(capacity, initialValue)
    private var position = 0

    protected fun getPosition(): Int {
        return position
    }

    override fun add(v: Long): Boolean {
        data[position] = v
        position = getIndex(1)
        return true
    }

    protected fun getIndex(index: Int): Int {
        if (index >= data.size || index <= -data.size) {
            throw IndexOutOfBoundsException()
        }
        return (position + index) % data.size
    }

    override val size: Int get() = data.size

    override fun getLong(index: Int): Long {
        return data[getIndex(index)]
    }

    open fun getMin(index: Int): Long {
        return getLong(index)
    }

    open fun getMax(index: Int): Long {
        return getLong(index)
    }

    override fun set(index: Int, v: Long): Long {
        throw UnsupportedOperationException()
    }

    companion object {
        @JvmStatic
        protected fun initArray(capacity: Int, initialValue: Long): LongArray {
            val data = LongArray(capacity)
            if (initialValue != 0L) {
                Arrays.fill(data, initialValue)
            }
            return data
        }
    }
}
