package com.jvmguard.server

import com.jvmguard.common.JvmGuardDirectories
import com.jvmguard.data.file.SnapshotFile
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import java.io.File

@Component
class SnapshotDirectoryInitializer(private val directories: JvmGuardDirectories) {

    @PostConstruct
    fun init() {
        val snapshotDirectory = File(directories.dataDirectory, "snapshots")
        if (!snapshotDirectory.isDirectory && !snapshotDirectory.mkdirs()) {
            error("Could not create snapshot directory $snapshotDirectory")
        }
        SnapshotFile.snapshotDirectory = snapshotDirectory
    }
}
