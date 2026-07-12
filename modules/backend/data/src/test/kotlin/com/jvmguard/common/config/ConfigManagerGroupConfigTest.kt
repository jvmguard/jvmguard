package com.jvmguard.common.config

import com.jvmguard.agent.comm.CodecTypes
import com.jvmguard.agent.config.VmType
import com.jvmguard.data.vmdata.VmIdentifier
import org.h2.jdbcx.JdbcDataSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import javax.sql.DataSource

class ConfigManagerGroupConfigTest {

    private val storefront = VmIdentifier("Demo/Storefront", VmType.POOL)

    @Test
    fun poolConfigDoesNotAccumulateAcrossRestarts() {
        val dataSource = freshStore()
        repeat(5) {
            // Each iteration is a server start: a new manager reloads the store, then the pool connects.
            ConfigManager(ConfigStorage(dataSource)).groupConnected(storefront)
        }

        val configs = ConfigManager(ConfigStorage(dataSource)).getGroupConfigs()
            .filter { it.hierarchyPath == "Demo/Storefront" }
        assertEquals(1, configs.size, "the pool must not accumulate a config per server start")
        assertEquals(VmType.POOL, configs.single().groupType, "the pool must stay a pool across restarts")
    }

    private fun freshStore(): DataSource {
        val dataSource = JdbcDataSource()
        dataSource.setURL("jdbc:h2:mem:cfg-${DB_COUNTER.getAndIncrement()};DB_CLOSE_DELAY=-1")
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
