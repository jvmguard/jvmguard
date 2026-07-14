package dev.jvmguard.integration.tests.jvmguard.previous

import java.io.File
import java.nio.charset.StandardCharsets

object BootstrapFileUtil {

    fun emptyDirectory(dir: File, excludedFiles: Set<File>) {
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (!excludedFiles.contains(file.absoluteFile)) {
                if (file.isDirectory) {
                    deleteDirectory(file, excludedFiles)
                }
                file.delete()
            }
        }
    }

    fun deleteDirectory(dir: File, excludedFiles: Set<File>) {
        emptyDirectory(dir, excludedFiles)
        dir.delete()
    }

    fun getHashedPath(path: String): String {
        val crc64 = CRC64()
        crc64.update(path.toByteArray(StandardCharsets.UTF_8))
        return crc64.value.toString(Character.MAX_RADIX)
    }
}
