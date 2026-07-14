package dev.jvmguard.data.user

import dev.jvmguard.agent.config.base.AbstractEntity
import dev.jvmguard.data.file.SnapshotFileType
import dev.jvmguard.data.vmdata.VM
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
        set(value) { field = changed(field, value) }

    var name: String = name
        set(value) { field = changed(field, value) }

    var message: String = message
        set(value) { field = changed(field, value) }

    var isItemRead: Boolean
        get() = itemRead
        set(value) { itemRead = changed(itemRead, value) }

    override fun toString(): String =
        "InboxItem{date=$date, snapshotFileId=$snapshotFileId, snapshotFileType=$snapshotFileType, " +
                "vm=$vm, name='$name', message='$message', itemRead=$itemRead}"
}
