package dev.jvmguard.ui.server

import dev.jvmguard.connector.api.ServerConnection
import dev.jvmguard.connector.api.SsoProviderInfo

interface LoginService {

    fun login(userName: String, password: String, authenticatorCode: String?): ServerConnection
    fun isUse2fa(): Boolean
    fun enabledSsoProviders(): List<SsoProviderInfo>
}
