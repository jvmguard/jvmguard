package com.jvmguard.connector.server

import com.jvmguard.connector.api.Server

interface ServerControl {
    val isShuttingDown: Boolean
    val server: Server
    fun shutdown()
}
