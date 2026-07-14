package dev.jvmguard.connector.api

import dev.jvmguard.data.user.User

interface ServerConnectionRegistry {
    fun add(serverConnection: ServerConnection)
    fun remove(serverConnection: ServerConnection)
    fun getServerConnections(): Collection<ServerConnection>
    fun getLoggedInUsers(): Collection<User>
    fun clearConnections()
}
