package dev.jvmguard.collector.main

import dev.jvmguard.common.DatabaseWriter
import dev.jvmguard.common.Loggers
import dev.jvmguard.common.notification.InboxManager
import dev.jvmguard.common.notification.ModificationEvent
import dev.jvmguard.common.notification.ModificationType
import dev.jvmguard.data.file.SnapshotFile
import dev.jvmguard.data.file.SnapshotFileType
import dev.jvmguard.data.user.InboxItem
import dev.jvmguard.data.user.User
import dev.jvmguard.data.vmdata.VM
import jakarta.annotation.PostConstruct
import org.springframework.context.ApplicationEventPublisher
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Component
import java.sql.Connection

@Component
class InboxManagerImpl(
    private val vmStorage: VmStorage,
    private val snapshotFileStorage: SnapshotFileStorage,
    private val eventPublisher: ApplicationEventPublisher,
    private val databaseWriter: DatabaseWriter,
    private val jdbcClient: JdbcClient,
) : InboxManager, SnapshotFileStorage.SnapshotFileDeletionListener {

    @PostConstruct
    fun postConstruct() {
        snapshotFileStorage.addDeletionListener(this)
    }

    override fun snapshotFileDeleted(connection: Connection, snapshotFileId: Long) {
        deleteSnapshotFile(connection, snapshotFileId)
    }

    override fun createInboxItem(user: User, snapshotFile: SnapshotFile?, vm: VM?, name: String, message: String) {
        val usedVm = snapshotFile?.vm ?: vm

        databaseWriter.executeInWriter {
            try {
                jdbcClient
                    .sql("insert into $INBOX (userId, inboxTime, snapshotFileId, snapshotFileType, vmId, name, message, itemRead) values (?,?,?,?,?,?,?,FALSE)")
                    .param(user.id)
                    .param(System.currentTimeMillis())
                    .param(if (snapshotFile != null) snapshotFile.id else 0L)
                    .param(snapshotFile?.type?.databaseId ?: 0)
                    .param(usedVm?.id ?: 0L)
                    .param(name)
                    .param(message)
                    .update()
                eventPublisher.publishEvent(ModificationEvent(this, user, ModificationType.INBOX))
            } catch (e: Exception) {
                SERVER_LOGGER.error("could not create snapshot $INBOX item for {}", user, e)
            }
        }
    }

    override fun deleteVMs(connection: Connection, vms: List<VM>) {
        val statement = connection.prepareStatement("delete from $INBOX where vmId=?")
        for (vm in vms) {
            statement.setLong(1, vm.id)
            statement.execute()
        }
    }

    override fun deleteSnapshotFile(connection: Connection, id: Long) {
        val statement = connection.prepareStatement("delete from $INBOX where snapshotFileId=?")
        statement.setLong(1, id)
        statement.execute()
    }

    override fun modifyItemRead(inboxItem: InboxItem) {
        if (inboxItem.id != null) {
            try {
                jdbcClient
                    .sql("update $INBOX set itemRead=? where id=?")
                    .param(inboxItem.isItemRead).param(inboxItem.id)
                    .update()
            } catch (e: Exception) {
                SERVER_LOGGER.error("could not update $INBOX item", e)
            }
        }
    }

    override fun delete(inboxItem: InboxItem) {
        if (inboxItem.id != null) {
            val snapshotFileId = inboxItem.snapshotFileId
            var orphaned = false
            try {
                jdbcClient.sql("delete from $INBOX where id=?").param(inboxItem.id).update()
                // Snapshot artifacts are shared across users via per-user inbox rows: when the last
                // referencing inbox row is removed the file is pruned.
                if (snapshotFileId != null) {
                    val count = jdbcClient.sql("select count(*) from $INBOX where snapshotFileId=?")
                        .param(snapshotFileId).query(Long::class.java).single()
                    orphaned = count == 0L
                }
            } catch (e: Exception) {
                SERVER_LOGGER.error("could not delete $INBOX item", e)
            }
            if (orphaned && snapshotFileId != null) {
                val snapshotFile = snapshotFileStorage.load(snapshotFileId)
                if (snapshotFile != null) {
                    snapshotFileStorage.delete(snapshotFile)
                }
            }
        }
    }

    override fun getInboxItems(user: User): Collection<InboxItem> {
        if (user.id != null) {
            try {
                return jdbcClient
                    .sql("select * from $INBOX where userId=?")
                    .param(user.id)
                    .query { resultSet, _ ->
                        val snapshotFileType = SnapshotFileType.fromDatabaseId(resultSet.getInt("snapshotFileType"))
                        val snapshotFileId = resultSet.getLong("snapshotFileId")
                        InboxItem(
                            resultSet.getLong("id"),
                            resultSet.getLong("inboxTime"),
                            if (snapshotFileId == 0L) null else snapshotFileId,
                            snapshotFileType,
                            vmStorage.getVmById(resultSet.getLong("vmId")),
                            resultSet.getString("name"),
                            resultSet.getString("message"),
                            resultSet.getBoolean("itemRead"),
                        )
                    }
                    .list()
            } catch (e: Exception) {
                SERVER_LOGGER.error("could not list $INBOX items", e)
            }
        }
        return ArrayList()
    }

    companion object {
        const val INBOX = "inbox"

        private val SERVER_LOGGER = Loggers.SERVER
    }
}
