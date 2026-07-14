package dev.jvmguard.ui.server

import dev.jvmguard.connector.api.ServerConnection
import dev.jvmguard.data.user.AccessLevel

interface JvmGuardPrincipal {
    val loginName: String
    val accessLevel: AccessLevel
    val serverConnection: ServerConnection?
}
