package com.jvmguard.connector.api

import com.jvmguard.data.user.User

interface ServerConnectionRegistry {
    fun add(serverConnection: ServerConnection)
    fun remove(serverConnection: ServerConnection)
    fun getServerConnections(): Collection<ServerConnection>
    fun getLoggedInUsers(): Collection<User>
    fun clearConnections()
}
