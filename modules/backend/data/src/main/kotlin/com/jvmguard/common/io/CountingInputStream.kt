package com.jvmguard.common.io

import java.io.FilterInputStream
import java.io.InputStream

class CountingInputStream(input: InputStream) : FilterInputStream(input) {

    var count: Long = 0
        private set

    override fun read(): Int {
        val value = `in`.read()
        if (value > -1) {
            count++
        }
        return value
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val bytesRead = `in`.read(b, off, len)
        if (bytesRead > 0) {
            count += bytesRead
        }
        return bytesRead
    }

    override fun skip(n: Long): Long {
        val skipped = super.skip(n)
        count += skipped
        return skipped
    }
}
