package com.jvmguard.ui.server

import com.jvmguard.data.file.SnapshotFile
import com.jvmguard.data.user.AccessLevel
import com.jvmguard.data.user.User
import com.jvmguard.connector.api.ServerConnection
import com.jvmguard.connector.api.ServerConnectionRegistry
import com.jvmguard.connector.server.mock.MockServerConnectionImpl
import java.nio.file.Files

object MockConnections {

    init {
        val snapshotDir = Files.createTempDirectory("jvmguard-web-mock-snapshots").toFile()
        snapshotDir.deleteOnExit()
        SnapshotFile.snapshotDirectory = snapshotDir
    }

    fun create(accessLevel: AccessLevel = AccessLevel.ADMIN): MockServerConnectionImpl {
        val user = User("admin", "Administrator", "", "admin@example.com", accessLevel)

        val registry = object : ServerConnectionRegistry {
            override fun add(serverConnection: ServerConnection) {}
            override fun remove(serverConnection: ServerConnection) {}
            override fun getServerConnections(): Collection<ServerConnection> = emptyList()
            override fun getLoggedInUsers(): Collection<User> = emptyList()
            override fun clearConnections() {}
        }

        return MockServerConnectionImpl(user, registry)
    }
}
