package com.jvmguard.ui.server

import com.jvmguard.connector.api.ServerConnection
import com.jvmguard.data.user.AccessLevel

interface JvmGuardPrincipal {
    val loginName: String
    val accessLevel: AccessLevel
    val serverConnection: ServerConnection?
}
