package com.jvmguard.ui.server

import com.jvmguard.connector.api.ServerConnection
import com.jvmguard.connector.api.SsoProviderInfo

interface LoginService {

    fun login(userName: String, password: String, authenticatorCode: String?): ServerConnection
    fun isUse2fa(): Boolean
    fun enabledSsoProviders(): List<SsoProviderInfo>
}
