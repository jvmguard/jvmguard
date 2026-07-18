package dev.jvmguard.collector.main

import dev.jvmguard.agent.data.FileMover
import dev.jvmguard.collector.util.Consolidator
import java.io.File
import dev.jvmguard.common.config.ConfigManager
import dev.jvmguard.data.file.SnapshotFile
import dev.jvmguard.data.file.SnapshotFileType
import dev.jvmguard.data.vmdata.VM
import jakarta.annotation.PostConstruct
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization
import org.springframework.stereotype.Component
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement
import java.util.concurrent.CopyOnWriteArrayList
import javax.sql.DataSource

@Component
@DependsOnDatabaseInitialization
class SnapshotFileStorage(
    private val vmStorage: VmStorage,
    private val configManager: ConfigManager,
    private val consolidator: Consolidator,
    private val dataSource: DataSource,
) {
    private val deletionListeners = CopyOnWriteArrayList<SnapshotFileDeletionListener>()

    fun addDeletionListener(listener: SnapshotFileDeletionListener) {
        deletionListeners.add(listener)
    }

    interface SnapshotFileDeletionListener {
        fun snapshotFileDeleted(connection: Connection, snapshotFileId: Long)
    }

    @PostConstruct
    fun postConstruct() {
        consolidator.register(3) { deleteExpired() }
    }

    private fun deleteExpired() {
        val days = configManager.getGlobalConfig(false).snapshotFileDays
        if (days <= 0) {
            return
        }
        val cutoff = System.currentTimeMillis() - days.toLong() * 24 * 60 * 60 * 1000
        val expired = ArrayList<SnapshotFile>()
        try {
            dataSource.connection.use { connection ->
                connection.prepareStatement("select * from $SNAPSHOT_FILE where snapshotTime < ?").use { statement ->
                    statement.setLong(1, cutoff)
                    statement.executeQuery().use { resultSet ->
                        while (resultSet.next()) {
                            val vm = vmStorage.getVmById(resultSet.getLong("vmId"))
                            val type = SnapshotFileType.fromDatabaseId(resultSet.getInt("type"))
                            if (vm != null && type != null) {
                                expired.add(createSnapshotFile(resultSet, vm, type))
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            VmManagerImpl.SERVER_LOGGER.error("error querying expired snapshot files", e)
        }
        for (snapshotFile in expired) {
            delete(snapshotFile)
        }
    }

    fun createSnapshotFile(vm: VM, type: SnapshotFileType, millis: Long, name: String, fileMover: FileMover): SnapshotFile? {
        var snapshotFile: SnapshotFile? = null
        var movedFile: File? = null
        try {
            dataSource.connection.use { connection ->
                connection.autoCommit = false
                try {
                    connection.prepareStatement(
                        "insert into $SNAPSHOT_FILE (vmId, type, snapshotTime, name, uncompressedLength) values (?,?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS
                    ).use { statement ->
                        statement.setLong(1, vm.id)
                        statement.setInt(2, type.databaseId)
                        statement.setLong(3, millis)
                        statement.setString(4, name)
                        statement.setLong(5, fileMover.uncompressedLength)
                        statement.execute()
                        statement.generatedKeys.use { resultSet ->
                            if (resultSet.next()) {
                                val id = resultSet.getLong(1)
                                val created = SnapshotFile(id, vm, type, millis, name)
                                fileMover.moveToFile(created.file)
                                movedFile = created.file
                                created.updateUncompressedLength(fileMover.uncompressedLength)
                                snapshotFile = created
                            }
                        }
                    }
                    connection.commit()
                } catch (e: Exception) {
                    connection.rollback()
                    throw e
                } finally {
                    connection.autoCommit = true
                }
            }
        } catch (e: Exception) {
            snapshotFile = null
            movedFile?.delete()
            VmManagerImpl.SERVER_LOGGER.error("could not create snapshot file for {}", vm, e)
        }
        return snapshotFile
    }

    fun deleteVMs(connection: Connection, vms: List<VM>) {
        val statement = connection.prepareStatement("delete from $SNAPSHOT_FILE where vmId=?")
        for (vm in vms) {
            statement.setLong(1, vm.id)
            statement.execute()
        }
    }

    fun getSnapshotFiles(type: SnapshotFileType?, selectedVm: VM): Collection<SnapshotFile> {
        val result = ArrayList<SnapshotFile>()
        try {
            dataSource.connection.use { connection ->
                if (selectedVm.isGroupNode) {
                    val selectedIdentifier = selectedVm.qualifiedIdentifier
                    connection.prepareStatement("select * from $SNAPSHOT_FILE" + (if (type == null) "" else " where type=?")).use { statement ->
                        if (type != null) {
                            statement.setInt(1, type.databaseId)
                        }
                        statement.executeQuery().use { resultSet ->
                            while (resultSet.next()) {
                                val vm = vmStorage.getVmById(resultSet.getLong("vmId"))
                                val snapshotFileType = SnapshotFileType.fromDatabaseId(resultSet.getInt("type"))
                                if (snapshotFileType != null && vm != null && vm.isIncluded(selectedIdentifier)) {
                                    result.add(createSnapshotFile(resultSet, vm, snapshotFileType))
                                }
                            }
                        }
                    }
                } else {
                    connection.prepareStatement("select * from $SNAPSHOT_FILE where vmId=?" + (if (type == null) "" else " and type=?")).use { statement ->
                        statement.setLong(1, selectedVm.id)
                        if (type != null) {
                            statement.setInt(2, type.databaseId)
                        }
                        statement.executeQuery().use { resultSet ->
                            while (resultSet.next()) {
                                val snapshotFileType = SnapshotFileType.fromDatabaseId(resultSet.getInt("type"))
                                if (snapshotFileType != null) {
                                    result.add(createSnapshotFile(resultSet, selectedVm, snapshotFileType))
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            VmManagerImpl.SERVER_LOGGER.error("could not read snapshot files", e)
        }
        return result
    }

    private fun createSnapshotFile(resultSet: ResultSet, vm: VM, snapshotFileType: SnapshotFileType): SnapshotFile {
        val snapshotFile = SnapshotFile(resultSet.getLong("id"), vm, snapshotFileType, resultSet.getLong("snapshotTime"), resultSet.getString("name"))
        val uncompressedLength = resultSet.getLong("uncompressedLength")
        snapshotFile.updateUncompressedLength(if (resultSet.wasNull()) SnapshotFile.UNKNOWN_LENGTH.toLong() else uncompressedLength)
        return snapshotFile
    }

    fun delete(snapshotFile: SnapshotFile?) {
        if (snapshotFile?.id != null) {
            try {
                dataSource.connection.use { connection ->
                    connection.prepareStatement("delete from $SNAPSHOT_FILE where id=?").use { statement ->
                        statement.setLong(1, snapshotFile.id)
                        statement.execute()
                    }
                    snapshotFile.file.delete()
                    for (listener in deletionListeners) {
                        listener.snapshotFileDeleted(connection, snapshotFile.id)
                    }
                }
            } catch (e: Exception) {
                VmManagerImpl.SERVER_LOGGER.error("could not delete snapshot file", e)
            }
        }
    }

    fun load(id: Long): SnapshotFile? {
        var snapshotFile: SnapshotFile? = null
        try {
            dataSource.connection.use { connection ->
                connection.prepareStatement("select * from $SNAPSHOT_FILE where id=?").use { statement ->
                    statement.setLong(1, id)
                    statement.executeQuery().use { resultSet ->
                        while (resultSet.next()) {
                            val snapshotFileType = SnapshotFileType.fromDatabaseId(resultSet.getInt("type"))
                            val vm = vmStorage.getVmById(resultSet.getLong("vmId"))
                            if (snapshotFileType != null && vm != null) {
                                snapshotFile = createSnapshotFile(resultSet, vm, snapshotFileType)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            VmManagerImpl.SERVER_LOGGER.error("could not load snapshot file {}", id, e)
        }
        return snapshotFile
    }

    companion object {
        const val SNAPSHOT_FILE = "snapshot_file"
    }
}
