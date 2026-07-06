package com.jvmguard.connector.api

import com.jvmguard.annotation.Inheritance
import com.jvmguard.annotation.Inheritance.Mode
import com.jvmguard.annotation.MethodTransaction
import com.jvmguard.annotation.Part
import com.jvmguard.data.config.DefaultTheme
import com.jvmguard.data.config.GroupConfig
import com.jvmguard.data.config.SmtpConfig
import com.jvmguard.data.user.User
import com.jvmguard.data.vmdata.TelemetryType

interface Server {

    val isNewInstallation: Boolean
    val windowTitle: String
    val defaultTheme: DefaultTheme
    val isUse2fa: Boolean

    val enabledSsoProviders: List<SsoProviderInfo>

    val idToTelemetryType: Map<String, TelemetryType>

    fun createInitialUser(
        userName: String,
        fullName: String,
        email: String,
        passwordHash: String,
        use2fA: Boolean,
        smtpConfig: SmtpConfig?,
        groupConfig: GroupConfig
    )

    @MethodTransaction(naming = [Part(text = "Login")], group = "serverConnection", inheritance = Inheritance(Mode.WITH_SUPERCLASS_NAME))
    fun authenticate(loginName: String, password: String, authenticatorCode: String?): User

    fun authenticateSso(issuer: String, subject: String, email: String, name: String?, groups: List<String>): User

    fun connect(user: User, mode: MockMode = MockMode.NONE): ServerConnection

    fun login(loginName: String, password: String, authenticatorCode: String?, mode: MockMode = MockMode.NONE): ServerConnection {
        return connect(authenticate(loginName, password, authenticatorCode), mode)
    }

    companion object {
        const val TEST_AUTH_CODE: String = "123"
    }
}

data class SsoProviderInfo(val registrationId: String, val displayName: String)
