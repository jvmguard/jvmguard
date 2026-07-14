package dev.jvmguard.common.io.compress

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream

abstract class Compressor {

    abstract fun writeEntry(file: File, archivePath: String)

    abstract fun finish()

    protected fun writeData(file: File, out: OutputStream) {
        if (file.isDirectory) {
            return
        }

        BufferedInputStream(FileInputStream(file)).use { fileIn ->
            var bytesToBeRead = file.length()
            val buffer = ByteArray(16384)
            while (bytesToBeRead > 0) {
                val readNow = fileIn.read(buffer, 0, minOf(bytesToBeRead, buffer.size.toLong()).toInt())
                out.write(buffer, 0, readNow)
                bytesToBeRead -= readNow
            }
        }
    }
}
