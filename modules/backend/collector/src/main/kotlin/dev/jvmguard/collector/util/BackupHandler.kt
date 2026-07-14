package dev.jvmguard.collector.util

import dev.jvmguard.common.Loggers
import dev.jvmguard.common.JvmGuardDirectories
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.task.TaskExecutor
import org.springframework.stereotype.Component
import java.io.File
import java.io.IOException
import java.io.PrintWriter
import javax.sql.DataSource

@Component
class BackupHandler(
    @param:Qualifier("backupExecutor") private val executor: TaskExecutor,
    private val directories: JvmGuardDirectories,
    private val dataSource: DataSource,
) {

    fun checkBackupFile(files: Array<File>) {
        for (file in files) {
            if (file.name == TRIGGER_BACKUP) {
                executor.execute {
                    if (file.isFile) {
                        try {
                            backup()
                        } catch (t: Throwable) {
                            SERVER_LOGGER.error("during backup", t)
                            try {
                                PrintWriter(file.parentFile, TRIGGER_BACKUP_ERROR).use { writer ->
                                    t.printStackTrace(writer)
                                }
                            } catch (e: IOException) {
                                SERVER_LOGGER.error("during backup", e)
                            }
                        } finally {
                            file.delete()
                        }
                    }
                }
            }
        }
    }

    fun backup() {
        SERVER_LOGGER.info("Backup requested")
        directories.backupDirectory.mkdirs()
        backupDatabase(File(directories.backupDirectory, JVMGUARD_BAK).absolutePath)
        SERVER_LOGGER.info("Backup succeed")
    }

    private fun backupDatabase(path: String) {
        try {
            dataSource.connection.use { connection ->
                connection.prepareStatement("BACKUP TO '$path'").use { statement ->
                    statement.execute()
                }
            }
        } catch (t: Throwable) {
            throw BackupException("Error creating $path: $t", t)
        }
    }

    class BackupException(message: String, cause: Throwable) : Exception(message, cause)

    companion object {
        const val JVMGUARD_BAK = "jvmguard.bak"

        private val SERVER_LOGGER = Loggers.SERVER

        private const val TRIGGER_BACKUP = "trigger_backup"
        private const val TRIGGER_BACKUP_ERROR = "$TRIGGER_BACKUP.error"
    }
}
