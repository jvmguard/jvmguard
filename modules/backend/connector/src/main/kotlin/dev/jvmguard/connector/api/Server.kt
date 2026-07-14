package dev.jvmguard.connector.api

import dev.jvmguard.annotation.Inheritance
import dev.jvmguard.annotation.Inheritance.Mode
import dev.jvmguard.annotation.MethodTransaction
import dev.jvmguard.annotation.Part
import dev.jvmguard.data.config.DefaultTheme
import dev.jvmguard.data.config.GroupConfig
import dev.jvmguard.data.config.SmtpConfig
import dev.jvmguard.data.user.User
import dev.jvmguard.data.vmdata.TelemetryType

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

data class SsoProviderInfo(val registrationId: String, val displayName: String, val google: Boolean = false)
