package dev.jvmguard.common.io.compress

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipCompressor(outputFile: File) : Compressor() {

    private val zipOut = ZipOutputStream(BufferedOutputStream(FileOutputStream(outputFile)))

    override fun writeEntry(file: File, archivePath: String) {
        zipOut.putNextEntry(ZipEntry(archivePath))
        writeData(file, zipOut)
        zipOut.closeEntry()
    }

    override fun finish() {
        zipOut.close()
    }
}
