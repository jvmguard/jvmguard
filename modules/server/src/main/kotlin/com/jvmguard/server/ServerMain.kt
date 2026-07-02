package com.jvmguard.server

import com.install4j.runtime.installer.helper.InstallerUtil
import com.jvmguard.agent.comm.CodecTypes
import com.jvmguard.agent.util.LoggingHandler
import com.jvmguard.collector.util.BackupHandler
import com.jvmguard.common.Loggers
import com.jvmguard.common.JvmGuardConfig
import com.jvmguard.common.JvmGuardDirectories
import com.jvmguard.common.JvmGuardProperties
import com.jvmguard.common.config.ImportManager
import com.jvmguard.database.Database
import com.jvmguard.mbean.data.MBeanManager
import com.jvmguard.mbean.data.MBeanManager.LogAdapter
import com.jvmguard.connector.api.Server
import com.jvmguard.connector.client.ServerFactory
import com.jvmguard.connector.server.ServerControl
import org.h2.engine.Constants
import org.h2.tools.Restore
import org.slf4j.Logger
import org.slf4j.bridge.SLF4JBridgeHandler
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.SpringApplication
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.SmartLifecycle
import org.springframework.stereotype.Component
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.sql.SQLException
import java.util.*
import java.util.logging.LogManager
import kotlin.system.exitProcess

@Component
class ServerMain(
    private val context: ConfigurableApplicationContext,
    private val database: Database,
    private val serverProvider: ObjectProvider<Server>,
    private val importManager: ImportManager,
    private val jvmguardProperties: JvmGuardProperties,
    private val directories: JvmGuardDirectories,
) : ServerControl, SmartLifecycle {

    @Volatile
    private var running = false
    @Volatile
    private var shuttingDown = false

    override fun start() {
        startH2Console()
        createDemoConfig()
        importStartupConfig(ImportManager.SERVER_CONFIG_FILE_NAME)
        importStartupConfig(ImportManager.RECORDING_CONFIG_FILE_NAME)
        startWebConnector()
        running = true
        LOGGER.info("Server started")
    }

    override fun stop() {
        if (running && !shuttingDown) {
            shuttingDown = true
            database.shutdown()
            running = false
            LOGGER.info("Server stopped")
        }
    }

    override fun isRunning(): Boolean = running

    // Import the startup config and publish the local server before VmManagerImpl
    // starts one phase later. On shutdown stop after it, so the database is
    // closed after collection is terminated.
    override fun getPhase(): Int = Int.MAX_VALUE - 1

    override fun shutdown() {
        exitProcess(SpringApplication.exit(context))
    }

    override val isShuttingDown: Boolean get() = shuttingDown

    override val server: Server get() = serverProvider.getObject()

    private fun importStartupConfig(fileName: String) {
        val startupConfigFile = File(directories.dataDirectory, fileName)
        if (startupConfigFile.exists()) {
            if (importManager.importConfig(startupConfigFile)) {
                LOGGER.info("Imported {}", startupConfigFile)
            } else {
                LOGGER.info("Could not import {}", startupConfigFile)
            }
            startupConfigFile.delete()
        }
    }

    private fun createDemoConfig() {
        val demoLoadFile = File(System.getProperty("java.io.tmpdir"), "jvmguard_load_demo_config")
        if (demoLoadFile.exists()) {
            demoLoadFile.delete()
            val demoConfigFile = File(directories.demoDirectory, "jvmguard_demo.json")
            if (importManager.importConfig(demoConfigFile)) {
                LOGGER.info("Created demo configuration")
            }
        }
    }

    private fun startWebConnector() {
        ServerFactory.localServer = serverProvider.getObject()
    }

    private fun startH2Console() {
        if (!jvmguardProperties.isStartH2Console) {
            return
        }
        val databaseDirectory = directories.databaseDirectory.absolutePath.replace('\\', '/')
        try {
            val h2Properties = Properties().apply {
                val driver = database.driverClassName
                if (driver.isNotEmpty()) {
                    setProperty("0", "jvmguard db|$driver|${database.jdbcUrl}|${database.username}")
                }
                setProperty("webAllowOthers", jvmguardProperties.h2ConsoleAllowOthers)
                setProperty("webPort", jvmguardProperties.h2ConsolePort.toString())
            }
            File(databaseDirectory).mkdirs()
            FileOutputStream(File(databaseDirectory, Constants.SERVER_PROPERTIES_NAME)).use { out ->
                h2Properties.store(out, "H2 Server Properties")
            }
        } catch (e: IOException) {
            LOGGER.warn("could not store embedded database viewer properties.", e)
        }

        try {
            // H2 console at http://localhost:8082
            org.h2.tools.Server.createWebServer("-properties", databaseDirectory).start()
        } catch (e: SQLException) {
            LOGGER.error("Could not create H2 database console", e)
        }
    }

    companion object {
        init {
            LogManager.getLogManager().reset() // mute JUL console output
            SLF4JBridgeHandler.install() // redirect JUL to slf4j
        }

        val LOGGER: Logger = Loggers.SERVER

        private lateinit var applicationContext: ConfigurableApplicationContext

        fun <T : Any> getBean(type: Class<T>): T = applicationContext.getBean(type)

        /** Closes the in-process context. Used by integration tests. */
        fun closeContext() {
            if (::applicationContext.isInitialized) {
                applicationContext.close()
            }
        }

        @JvmStatic
        fun main(args: Array<String>) {
            InstallerUtil.setStartupVmParameters()
            System.setProperty("jvmguard.server", "true")
            CodecTypes.registerAll()

            val application = SpringApplication(JvmGuardApplication::class.java)
            // The external application.yaml is located on the config directory next to the launcher and is loaded
            // by Spring Boot as the config source with the highest precedence.
            val externalConfigDir = JvmGuardDirectories.resolveExternalConfigDir().absolutePath.replace('\\', '/')
            application.setDefaultProperties(mapOf("spring.config.additional-location" to "optional:file:$externalConfigDir/"))
            if (JvmGuardConfig.isIntegrationTest) {
                application.setAdditionalProfiles("integrationTest")
            }
            // Runs before the context is refreshed. A pending restore (a "jvmguard.bak" dropped into the database
            // directory) must be applied before the auto-configured DataSource opens the H2 files during refresh.
            application.addInitializers(ApplicationContextInitializer<ConfigurableApplicationContext> { bootstrap() })
            applicationContext = application.run()
        }

        private fun bootstrap() {
            initAdditionalLogging()
            LOGGER.info("Starting server")
            restorePendingBackup()
        }

        // The backup must come from the same schema version as this server: a restored database carries its own
        // flyway_schema_history, so Flyway will not re-run migrations against it. Cross-version restore is not
        // supported (the clean-break single-database layout has no migration from the old two-database backups).
        private fun restorePendingBackup() {
            val databaseDirectory = JvmGuardDirectories.getInstance().databaseDirectory
            val backup = File(databaseDirectory, BackupHandler.JVMGUARD_BAK)
            if (backup.isFile) {
                LOGGER.info("Restoring database from {}", BackupHandler.JVMGUARD_BAK)
                try {
                    Restore.execute(backup.absolutePath, databaseDirectory.absolutePath, null)
                    backup.delete()
                    LOGGER.info("Restoring succeeded")
                } catch (t: Throwable) {
                    LOGGER.error("Error restoring {}", BackupHandler.JVMGUARD_BAK, t)
                    exitProcess(1)
                }
            }
        }

        fun initAdditionalLogging() {
            if (LoggingHandler.getLogFile() == null) {
                val logDir = File(JvmGuardDirectories.getInstance().dataDirectory, "log")
                if (!logDir.isDirectory) {
                    logDir.mkdirs()
                }
                LoggingHandler.setLogFile(File(logDir, "agent.log"))
            }

            MBeanManager.setLogAdapter(object : LogAdapter {
                override fun error(message: String) {
                    LOGGER.warn(message)
                }

                override fun error(t: Throwable) {
                    LOGGER.warn("in mbean transfer: ", t)
                }

                override fun isLogNotification(): Boolean = true
            })
        }
    }
}
