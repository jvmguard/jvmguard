package dev.jvmguard.ui.server

import dev.jvmguard.connector.api.ServerConnection
import dev.jvmguard.connector.api.SsoProviderInfo
import dev.jvmguard.connector.client.ServerFactory

class DefaultLoginService : LoginService {

    override fun login(userName: String, password: String, authenticatorCode: String?): ServerConnection =
        SecurityBridge.authenticate(userName, password, authenticatorCode, Sessions.mockMode())

    override fun isUse2fa(): Boolean = try {
        ServerFactory.lookup().isUse2fa
    } catch (_: Exception) {
        false
    }

    override fun enabledSsoProviders(): List<SsoProviderInfo> = try {
        ServerFactory.lookup().enabledSsoProviders
    } catch (_: Exception) {
        emptyList()
    }
}
