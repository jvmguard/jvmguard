package com.jvmguard.connector.client

import com.jvmguard.connector.api.Server

object ServerFactory {

    var localServer: Server? = null

    fun lookup(): Server = localServer ?: error("No jvmguard server available")
}
