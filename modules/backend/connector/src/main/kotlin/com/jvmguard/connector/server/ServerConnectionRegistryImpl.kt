package com.jvmguard.connector.server

import com.jvmguard.annotation.Telemetry
import com.jvmguard.annotation.TelemetryFormat
import com.jvmguard.common.notification.ModificationEvent
import com.jvmguard.data.user.User
import com.jvmguard.connector.api.ServerConnection
import com.jvmguard.connector.api.ServerConnectionRegistry
import jakarta.annotation.PostConstruct
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.util.Collections

@Component
class ServerConnectionRegistryImpl : ServerConnectionRegistry {

    private val serverConnections = HashSet<ServerConnection>()
    private var totalConnectionCount: Long = 0

    @Synchronized
    override fun add(serverConnection: ServerConnection) {
        serverConnections.add(serverConnection)
        totalConnectionCount++
    }

    @Synchronized
    override fun remove(serverConnection: ServerConnection) {
        serverConnections.remove(serverConnection)
    }

    @Synchronized
    override fun getServerConnections(): Collection<ServerConnection> = Collections.unmodifiableCollection(serverConnections)

    @Synchronized
    override fun getLoggedInUsers(): Collection<User> =
        serverConnections.mapTo(HashSet()) { it.user }

    @EventListener
    fun onModification(event: ModificationEvent) {
        for (serverConnection in getServerConnections()) {
            if (serverConnection is AbstractServerConnectionImpl &&
                serverConnection !== event.source &&
                (event.user == null || serverConnection.user == event.user)
            ) {
                serverConnection.modified(event.modificationType)
            }
        }
    }

    @Synchronized
    override fun clearConnections() {
        serverConnections.clear()
    }

    @PostConstruct
    fun postConstruct() {
        singleInstance = this
    }

    @Synchronized
    private fun getTotalConnectionCountInternal(): Long = totalConnectionCount

    companion object {
        @Volatile
        private var singleInstance: ServerConnectionRegistryImpl? = null

        @Telemetry(value = "Server Connections", format = TelemetryFormat(groupAverage = false))
        @JvmStatic
        private fun getConnectionCount(): Long = singleInstance?.getServerConnections()?.size?.toLong() ?: 0

        @Telemetry(value = "Total Server Connections", format = TelemetryFormat(groupAverage = false))
        @JvmStatic
        private fun getTotalConnectionCount(): Long = singleInstance?.getTotalConnectionCountInternal() ?: 0
    }
}
