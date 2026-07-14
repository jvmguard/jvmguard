package dev.jvmguard.data.agent

import dev.jvmguard.common.io.compress.Compressor
import dev.jvmguard.common.io.compress.TarCompressor
import dev.jvmguard.common.io.compress.ZipCompressor
import java.io.File

enum class ArchiveFileType(private val verbose: String, val fileName: String) {
    TAR_GZ("as a .tar.gz archive", "agent.tar.gz") {
        override fun getCompressor(file: File): Compressor =
            TarCompressor(file) { archivePath, directory ->
                if (directory || archivePath.endsWith(".sl") || archivePath.endsWith(".so")) {
                    MODE_EXECUTABLE
                } else {
                    MODE_REGULAR
                }
            }
    },
    ZIP("as a .zip archive", "agent.zip") {
        override fun getCompressor(file: File): Compressor = ZipCompressor(file)
    };

    abstract fun getCompressor(file: File): Compressor

    override fun toString(): String = verbose

    companion object {
        private val MODE_EXECUTABLE = "755".toInt(8)
        private val MODE_REGULAR = "644".toInt(8)
    }
}
