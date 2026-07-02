package com.jvmguard.data.agent

import com.jvmguard.agent.comm.JvmGuardKeyManager
import com.jvmguard.common.io.compress.Compressor
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class DownloadArchiveFile(
    archiveFileType: ArchiveFileType,
    agentDirectory: File,
    useSsl: Boolean,
    agentKeystore: File?,
) : AbstractArchiveFile(archiveFileType, agentDirectory) {

    private val agentKeystore: File? = if (useSsl) agentKeystore else null

    init {
        init()
    }

    override val isAutoInit: Boolean
        get() = false

    override val cache: MutableMap<ArchiveFileType, File>
        get() = Companion.cache

    override val archivePrefix: String
        get() = ARCHIVE_PREFIX

    override fun writeAdditionalTopLevelFiles(compressor: Compressor, archivePrefix: String) {
        agentKeystore?.let {
            compressor.writeEntry(it, archivePrefix + JvmGuardKeyManager.AGENT_STORE)
        }
    }

    companion object {
        const val ARCHIVE_PREFIX: String = "jvmguard/"

        private val cache: MutableMap<ArchiveFileType, File> = ConcurrentHashMap()
    }
}
