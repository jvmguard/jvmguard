package com.jvmguard.common.notification

import com.jvmguard.data.file.SnapshotFile
import com.jvmguard.data.user.InboxItem
import com.jvmguard.data.user.User
import com.jvmguard.data.vmdata.VM
import java.sql.Connection

interface InboxManager {
    fun createInboxItem(user: User, snapshotFile: SnapshotFile?, vm: VM?, name: String, message: String)

    fun deleteVMs(connection: Connection, vms: List<VM>)

    fun deleteSnapshotFile(connection: Connection, id: Long)

    fun modifyItemRead(inboxItem: InboxItem)
    fun delete(inboxItem: InboxItem)
    fun getInboxItems(user: User): Collection<InboxItem>
}
