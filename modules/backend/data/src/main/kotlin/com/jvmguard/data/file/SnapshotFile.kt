package com.jvmguard.data.file

import com.jvmguard.agent.config.base.AbstractEntity
import com.jvmguard.data.vmdata.VM
import java.io.File
import java.time.Instant

open class SnapshotFile(
    id: Long?,
    var vm: VM,
    var type: SnapshotFileType,
    snapshotTime: Long,
    name: String,
) : AbstractEntity(id) {

    var dateCreated: Instant = Instant.ofEpochMilli(snapshotTime)
        private set

    var name: String = name
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var uncompressedLength: Long = UNKNOWN_LENGTH.toLong()
        private set

    fun updateUncompressedLength(uncompressedLength: Long) {
        this.uncompressedLength = uncompressedLength
    }

    val file: File
        get() = File(snapshotDirectory, id.toString())

    val targetFileName: String
        get() = name.trim() + "." + type.extension

    override fun toString(): String =
        "SnapshotFile{" +
                "vm=" + vm +
                ", dateCreated=" + dateCreated +
                ", type=" + type +
                ", name='" + name + '\'' +
                '}'

    companion object {
        const val UNKNOWN_LENGTH: Int = -1

        var snapshotDirectory: File? = null
    }
}
