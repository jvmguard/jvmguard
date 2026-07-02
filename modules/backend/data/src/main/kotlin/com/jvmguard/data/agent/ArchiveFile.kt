package com.jvmguard.data.agent

import java.io.File

interface ArchiveFile {
    val file: File
    val fileSize: Long
}
