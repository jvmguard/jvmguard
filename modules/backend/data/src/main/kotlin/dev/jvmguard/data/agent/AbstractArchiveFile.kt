package dev.jvmguard.data.agent

import dev.jvmguard.common.io.compress.Compressor
import java.io.File

abstract class AbstractArchiveFile(
    private val archiveFileType: ArchiveFileType,
    private val agentDirectory: File,
) : ArchiveFile {

    private var archiveFile: File? = cache[archiveFileType]

    init {
        @Suppress("LeakingThis")
        if (isAutoInit) {
            init()
        }
    }

    protected open val isAutoInit: Boolean
        get() = true

    protected abstract val cache: MutableMap<ArchiveFileType, File>
    protected abstract val archivePrefix: String

    protected fun init() {
        var file = archiveFile
        if (file == null || !file.exists()) {
            file = File.createTempFile("jvmguard_agent", "archive")

            val compressor = archiveFileType.getCompressor(file)
            writeFiles(getStartDirectory(agentDirectory), archivePrefix, compressor)
            compressor.finish()

            cache[archiveFileType] = file
            file.deleteOnExit()
            archiveFile = file
        }
    }

    private fun writeFiles(directory: File, archivePrefix: String, compressor: Compressor) {
        val files = directory.listFiles() ?: return
        if (archivePrefix != this.archivePrefix) {
            compressor.writeEntry(directory, archivePrefix)
        }
        for (file in files) {
            if (file.isDirectory) {
                writeFiles(file, archivePrefix + file.name + "/", compressor)
            } else {
                compressor.writeEntry(file, archivePrefix + file.name)
                if (file.name == "jvmguard.jar") {
                    writeAdditionalTopLevelFiles(compressor, archivePrefix)
                }
            }
        }
    }

    protected open fun writeAdditionalTopLevelFiles(compressor: Compressor, archivePrefix: String) {
    }

    protected open fun getStartDirectory(agentDirectory: File): File = agentDirectory

    override val file: File
        get() = archiveFile!!

    override val fileSize: Long
        get() = archiveFile!!.length()
}
