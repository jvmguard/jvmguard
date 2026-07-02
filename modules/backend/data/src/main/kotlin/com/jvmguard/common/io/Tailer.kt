package com.jvmguard.common.io

import java.io.*

class Tailer(
    val file: File,
    private val listener: TailerListener,
    private val delay: Long,
    private val end: Boolean,
) : Runnable {

    @Volatile
    private var run = true

    init {
        listener.init(this)
    }

    override fun run() {
        var reader: RandomAccessFile? = null
        try {
            var position = if (end) file.length() else readFirstBatch()

            reader = RandomAccessFile(file, "r")

            var last = System.currentTimeMillis()
            reader.seek(position)

            while (run) {
                val length = file.length()
                if (length < position) {
                    listener.fileRotated()
                    try {
                        val save = reader
                        reader = RandomAccessFile(file, "r")
                        position = 0
                        closeQuietly(save)
                    } catch (e: FileNotFoundException) {
                        listener.handle(e)
                    }
                    continue
                } else {
                    if (length > position) {
                        last = System.currentTimeMillis()
                        position = readLines(reader!!)
                    } else if (isFileNewer(file, last)) {
                        position = 0
                        reader!!.seek(position)

                        last = System.currentTimeMillis()
                        position = readLines(reader)
                    }
                    listener.batchFinished()
                }
                try {
                    Thread.sleep(delay)
                } catch (_: InterruptedException) {
                }
            }
        } catch (e: Exception) {
            listener.handle(e)
        } finally {
            closeQuietly(reader)
        }
    }

    private fun readFirstBatch(): Long {
        val countingInputStream = CountingInputStream(BufferedInputStream(FileInputStream(file)))
        var position: Long = 0
        var line: String?
        while (readLine(countingInputStream).also { line = it } != null) {
            position = countingInputStream.count
            listener.handle(line!!)
        }
        listener.batchFinished()
        return position
    }

    private fun closeQuietly(closeable: Closeable?) {
        try {
            closeable?.close()
        } catch (_: IOException) {
        }
    }

    fun isFileNewer(file: File?, timeMillis: Long): Boolean {
        if (file == null) {
            throw IllegalArgumentException("No specified file")
        }
        if (!file.exists()) {
            return false
        }
        return file.lastModified() > timeMillis
    }

    fun stop() {
        run = false
    }

    private fun readLines(reader: RandomAccessFile): Long {
        var pos = reader.filePointer
        var line = readLine(reader)
        while (line != null) {
            pos = reader.filePointer
            listener.handle(line)
            line = readLine(reader)
        }
        reader.seek(pos)
        return pos
    }

    private fun readLine(reader: RandomAccessFile): String? = readLine { reader.read() }

    private fun readLine(inputStream: InputStream): String? = readLine { inputStream.read() }

    private fun interface ReadSource {
        fun read(): Int
    }

    companion object {
        fun create(file: File, listener: TailerListener, delay: Long = 1000, end: Boolean = false): Tailer {
            val tailer = Tailer(file, listener, delay, end)
            Thread(tailer).apply {
                isDaemon = true
                start()
            }
            return tailer
        }

        // Version of readline() that returns null on EOF rather than a partial line.
        private fun readLine(readSource: ReadSource): String? {
            val buffer = StringBuilder()
            var ch: Int
            var seenCR = false
            while (readSource.read().also { ch = it } != -1) {
                when (ch) {
                    '\n'.code -> return buffer.toString()
                    '\r'.code -> seenCR = true
                    else -> {
                        if (seenCR) {
                            buffer.append('\r')
                            seenCR = false
                        }
                        buffer.append(ch.toChar())
                    }
                }
            }
            return null
        }
    }
}
