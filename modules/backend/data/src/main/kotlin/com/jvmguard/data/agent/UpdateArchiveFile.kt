package com.jvmguard.data.agent

import java.io.File
import java.util.concurrent.ConcurrentHashMap

class UpdateArchiveFile(agentDirectory: File) :
    AbstractArchiveFile(ArchiveFileType.ZIP, agentDirectory) {

    override val cache: MutableMap<ArchiveFileType, File>
        get() = Companion.cache

    override val archivePrefix: String
        get() = ""

    override fun getStartDirectory(agentDirectory: File): File = File(agentDirectory, "lib")

    companion object {
        private val cache: MutableMap<ArchiveFileType, File> = ConcurrentHashMap()

        @Synchronized
        fun create(agentDirectory: File): UpdateArchiveFile = UpdateArchiveFile(agentDirectory)
    }
}
