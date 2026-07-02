package com.jvmguard.connector.api.log

import com.jvmguard.common.Loggers
import com.jvmguard.common.io.Tailer
import com.jvmguard.common.io.TailerListener
import java.io.File
import java.io.IOException
import java.util.concurrent.CountDownLatch

data class ContentDelta(val lines: List<String>, val rotated: Boolean)

interface LogFile {
    val file: File
    val fileSize: Long
    fun componentDelta(): ContentDelta
    fun close()
}

class LogFileImpl(fileName: String) : LogFile, TailerListener {

    private val firstBatch = CountDownLatch(1)
    private val lineBuffer = ArrayList<String>(3000)
    private val tailer: Tailer
    private var rotated = true

    init {
        var file = File(fileName)
        try {
            file = file.canonicalFile
        } catch (e: IOException) {
            Loggers.SERVER.warn("Could not canonicalize log file {}", fileName, e)
        }
        tailer = Tailer(file, this, 2000, false)
        Thread(tailer, "log-tailer").start()
        try {
            firstBatch.await()
        } catch (e: InterruptedException) {
            Loggers.SERVER.warn("Interrupted while waiting for first log batch", e)
            Thread.currentThread().interrupt()
        }
    }

    override val file: File get() = tailer.file

    override val fileSize: Long get() = tailer.file.length()

    override fun componentDelta(): ContentDelta {
        synchronized(lineBuffer) {
            val copy = ArrayList(lineBuffer)
            val oldRotated = rotated
            lineBuffer.clear()
            rotated = false
            return ContentDelta(copy, oldRotated)
        }
    }

    override fun close() {
        tailer.stop()
    }

    override fun handle(line: String) {
        synchronized(lineBuffer) {
            if (lineBuffer.size >= MAX_LINE_BUFFER_SIZE) {
                lineBuffer.subList(0, MAX_LINE_BUFFER_SIZE / 3).clear()
            }
            lineBuffer.add(line)
        }
    }

    override fun init(tailer: Tailer) {
    }

    override fun fileRotated() {
        synchronized(lineBuffer) {
            lineBuffer.clear()
            rotated = true
        }
    }

    override fun batchFinished() {
        firstBatch.countDown()
    }

    override fun handle(ex: Exception) {
        Loggers.SERVER.warn("Error tailing log file", ex)
    }

    companion object {
        private const val MAX_LINE_BUFFER_SIZE = 15000
    }
}
