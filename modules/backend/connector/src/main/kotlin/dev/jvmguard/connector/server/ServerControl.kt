package dev.jvmguard.connector.server

import dev.jvmguard.connector.api.Server

interface ServerControl {
    val isShuttingDown: Boolean
    val server: Server
    fun shutdown()
}
