package com.jvmguard.data.user

import com.jvmguard.agent.config.base.AbstractEntity
import com.jvmguard.data.file.SnapshotFileType
import com.jvmguard.data.vmdata.VM
import java.time.Instant

open class InboxItem(
    id: Long?,
    millis: Long,
    val snapshotFileId: Long?,
    val snapshotFileType: SnapshotFileType?,
    var vm: VM?,
    name: String,
    message: String,
    private var itemRead: Boolean,
) : AbstractEntity(id) {

    var date: Instant = Instant.ofEpochMilli(millis)
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var name: String = name
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var message: String = message
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var isItemRead: Boolean
        get() = itemRead
        set(value) {
            val old = itemRead
            itemRead = value
            fireChanged(old, value)
        }

    override fun toString(): String =
        "InboxItem{date=$date, snapshotFileId=$snapshotFileId, snapshotFileType=$snapshotFileType, " +
                "vm=$vm, name='$name', message='$message', itemRead=$itemRead}"
}
