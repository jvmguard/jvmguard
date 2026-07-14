package dev.jvmguard.connector.client

import dev.jvmguard.connector.api.Server

object ServerFactory {

    var localServer: Server? = null

    fun lookup(): Server = localServer ?: error("No jvmguard server available")
}
