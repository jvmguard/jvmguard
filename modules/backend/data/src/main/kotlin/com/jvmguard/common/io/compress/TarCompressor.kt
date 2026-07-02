package com.jvmguard.common.io.compress

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.GZIPOutputStream

class TarCompressor(outputFile: File, private val modeProvider: ModeProvider) : Compressor() {

    private val gzipOut = GZIPOutputStream(BufferedOutputStream(FileOutputStream(outputFile)))
    private val tarOut = TarArchiveOutputStream(gzipOut).apply {
        setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU)
    }

    override fun writeEntry(file: File, archivePath: String) {
        val tarEntry = TarArchiveEntry(file).apply {
            name = archivePath
            userName = ""
            mode = modeProvider.getMode(archivePath, file.isDirectory)
        }
        tarOut.putArchiveEntry(tarEntry)
        writeData(file, tarOut)
        tarOut.closeArchiveEntry()
    }

    override fun finish() {
        tarOut.close()
        gzipOut.close()
    }

    fun interface ModeProvider {
        fun getMode(archivePath: String, directory: Boolean): Int
    }
}
