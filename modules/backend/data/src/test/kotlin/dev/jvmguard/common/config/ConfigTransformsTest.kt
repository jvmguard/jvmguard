package dev.jvmguard.common.config

import dev.jvmguard.agent.comm.CodecTypes
import dev.jvmguard.agent.config.transactions.OtelTransactionDef
import dev.jvmguard.data.config.GlobalConfig
import dev.jvmguard.data.config.GroupConfig
import dev.jvmguard.data.vmdata.VmIdentifier
import org.h2.jdbcx.JdbcDataSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import javax.sql.DataSource

class ConfigTransformsTest {

    @Test
    fun freshInstallHasCurrentVersionAndOtelDef() {
        val configManager = ConfigManager(ConfigStorage(freshStore()))

        assertEquals(GlobalConfig.CURRENT_CONFIG_VERSION, configManager.getGlobalConfig(false).configVersion)
        assertEquals(1, rootOtelDefCount(configManager))
    }

    @Test
    fun legacyInstallGetsOtelDefOnce() {
        val dataSource = freshStore()
        val storage = ConfigStorage(dataSource)
        // a pre-versioning install: no config version, no OTel def
        storage.store(GlobalConfig::class.java, GlobalConfig())
        storage.store(GroupConfig::class.java, GroupConfig())

        val configManager = ConfigManager(ConfigStorage(dataSource))
        assertEquals(GlobalConfig.CURRENT_CONFIG_VERSION, configManager.getGlobalConfig(false).configVersion)
        assertEquals(1, rootOtelDefCount(configManager))

        // the transform must not run again on the next server start
        val restarted = ConfigManager(ConfigStorage(dataSource))
        assertEquals(1, rootOtelDefCount(restarted))
    }

    @Test
    fun deletedOtelDefIsNotRestored() {
        val dataSource = freshStore()
        val storage = ConfigStorage(dataSource)
        // the installation is current, but the user deleted the OTel def
        storage.store(GlobalConfig::class.java, GlobalConfig().apply { configVersion = GlobalConfig.CURRENT_CONFIG_VERSION })
        storage.store(GroupConfig::class.java, GroupConfig())

        val configManager = ConfigManager(ConfigStorage(dataSource))
        assertEquals(0, rootOtelDefCount(configManager))
    }

    private fun rootOtelDefCount(configManager: ConfigManager): Int =
        configManager.getGroupConfig(VmIdentifier.ROOT_GROUP_IDENTIFIER)
            .transactionSettings.transactionDefs.count { it is OtelTransactionDef }

    private fun freshStore(): DataSource {
        val dataSource = JdbcDataSource()
        dataSource.setURL("jdbc:h2:mem:cfg-transforms-${DB_COUNTER.getAndIncrement()};DB_CLOSE_DELAY=-1")
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.execute(
                    "CREATE TABLE config_storage (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                        "bean_type VARCHAR(255) NOT NULL, " +
                        "content MEDIUMTEXT NOT NULL)"
                )
            }
        }
        return dataSource
    }

    companion object {
        private val DB_COUNTER = AtomicInteger()

        @BeforeAll
        @JvmStatic
        fun registerCodecTypes() = CodecTypes.registerAll()
    }
}
