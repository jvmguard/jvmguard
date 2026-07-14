package dev.jvmguard.database

import dev.jvmguard.annotation.Telemetry
import dev.jvmguard.server.ServerMain
import com.zaxxer.hikari.HikariDataSource
import com.zaxxer.hikari.HikariPoolMXBean
import org.springframework.stereotype.Component
import java.sql.DriverManager
import java.sql.SQLException

@Component
class Database(private val dataSource: HikariDataSource) {

    init {
        telemetryDataSource = dataSource
    }

    val jdbcUrl: String get() = dataSource.jdbcUrl
    val driverClassName: String get() = dataSource.driverClassName
    val username: String get() = dataSource.username

    fun shutdown() {
        LOGGER.info("Shutting down database")
        telemetryDataSource = null
        // Close the pool first, then issue the H2 SHUTDOWN over a fresh connection: a pooled connection would
        // fail to return to the pool once SHUTDOWN has closed it. Read the url/user before closing the pool.
        val url = dataSource.jdbcUrl
        val username = dataSource.username
        dataSource.close()
        try {
            DriverManager.getConnection(url, username, "").use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute("SHUTDOWN")
                }
            }
        } catch (e: SQLException) {
            LOGGER.error("Error closing database", e)
        }
    }

    companion object {
        private val LOGGER = ServerMain.LOGGER

        @Volatile
        private var telemetryDataSource: HikariDataSource? = null

        @Telemetry(value = "DB Connections", line = "Busy")
        @JvmStatic
        private fun getBusyConnections(): Long = dataPool()?.activeConnections?.toLong() ?: 0L

        @Telemetry(value = "DB Connections", line = "Idle")
        @JvmStatic
        private fun getIdleConnections(): Long = dataPool()?.idleConnections?.toLong() ?: 0L

        @Telemetry(value = "DB Connections", line = "Total")
        @JvmStatic
        private fun getTotalConnections(): Long = dataPool()?.totalConnections?.toLong() ?: 0L

        private fun dataPool(): HikariPoolMXBean? {
            val dataSource = telemetryDataSource
            return if (dataSource != null && !dataSource.isClosed) dataSource.hikariPoolMXBean else null
        }
    }
}
