package dev.jvmguard.common.io

import java.io.PrintWriter
import java.io.Writer

class WinPrintWriter(out: Writer) : PrintWriter(out) {

    var numLines: Int = 0
        private set

    private var lineFeedOnly = false

    fun lineFeedOnly(value: Boolean): WinPrintWriter {
        lineFeedOnly = value
        return this
    }

    override fun println() {
        if (!lineFeedOnly) {
            write(13)
        }
        write(10)
        numLines++
    }
}
