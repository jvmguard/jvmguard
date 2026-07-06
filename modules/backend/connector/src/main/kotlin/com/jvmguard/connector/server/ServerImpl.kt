package com.jvmguard.connector.server

import com.jvmguard.agent.config.base.Identifiable
import com.jvmguard.collector.api.TelemetryProvider
import com.jvmguard.common.Loggers
import com.jvmguard.common.JvmGuardConfig
import com.jvmguard.common.JvmGuardProperties
import com.jvmguard.common.config.ConfigManager
import com.jvmguard.common.helper.GroupHelper
import com.jvmguard.common.helper.ListModification
import com.jvmguard.common.helper.PasswordHelper
import com.jvmguard.common.util.BeanUtil
import com.jvmguard.data.config.*
import com.jvmguard.data.user.AccessLevel
import com.jvmguard.data.user.User
import com.jvmguard.data.user.UserManager
import com.jvmguard.data.user.UserType
import com.jvmguard.data.vmdata.TelemetryType
import com.jvmguard.data.vmdata.VmIdentifier
import com.jvmguard.connector.api.MockMode
import com.jvmguard.connector.api.Server
import com.jvmguard.connector.api.ServerConnection
import com.jvmguard.connector.api.SsoProviderInfo
import com.jvmguard.connector.server.mock.MockServerConnectionImpl
import com.jvmguard.connector.server.mock.SnapshotReplayConnection
import com.jvmguard.connector.totp.TOTP
import com.jvmguard.connector.totp.TotpEncryption
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component
import java.time.Instant
import javax.security.auth.login.CredentialException
import javax.security.auth.login.FailedLoginException

@Component
class ServerImpl(
    private val connectionProvider: ObjectProvider<ServerConnectionImpl>,
    private val mockConnectionProvider: ObjectProvider<MockServerConnectionImpl>,
    private val snapshotConnectionProvider: ObjectProvider<SnapshotReplayConnection>,
    private val configManager: ConfigManager,
    private val telemetryProvider: TelemetryProvider,
    private val userManager: UserManager,
    private val properties: JvmGuardProperties,
    private val totpEncryption: TotpEncryption,
) : Server {

    override val isNewInstallation: Boolean
        get() = userManager.getAllUsers().isEmpty()

    override val windowTitle: String
        get() = getGlobalConfig().windowTitle.getOrDefault(properties.windowTitle)

    override val defaultTheme: DefaultTheme
        get() = getGlobalConfig().defaultTheme

    override val isUse2fa: Boolean
        get() = getGlobalConfig().use2fa

    override val enabledSsoProviders: List<SsoProviderInfo>
        get() = getGlobalConfig().ssoConfig.providers
            .filter { it.enabled }
            .map { SsoProviderInfo(SsoProviderConfig.slugify(it.displayName), it.displayName) }

    override fun createInitialUser(
        userName: String,
        fullName: String,
        email: String,
        passwordHash: String,
        use2fA: Boolean,
        smtpConfig: SmtpConfig?,
        groupConfig: GroupConfig
    ) {
        if (smtpConfig != null) {
            getGlobalConfig().smtpConfig = smtpConfig
        }
        val rootConfig = configManager.getGroupConfig(VmIdentifier.ROOT_GROUP_IDENTIFIER)
        BeanUtil.copyValues(groupConfig, rootConfig, listOf(Identifiable.PROPERTY_ID))
        val listModification = ListModification(listOf(rootConfig), emptyList(), emptyList(), GroupConfig::class.java)
        configManager.modifyGroupConfigs(listModification, AccessLevel.ADMIN, emptyList())
        getGlobalConfig().use2fa = use2fA

        val user = User()
        user.groupNames = arrayListOf(GroupHelper.ROOT_GROUP_ID)
        user.loginName = userName
        user.fullName = fullName
        user.passwordHash = passwordHash
        user.email = email
        user.accessLevel = AccessLevel.ADMIN
        userManager.store(user)
    }

    override fun authenticate(loginName: String, password: String, authenticatorCode: String?): User {
        if (password.isEmpty()) {
            throw FailedLoginException("The password cannot be empty.")
        }

        val user = findUser(loginName) ?: throwInvalidUser()
        if (getGlobalConfig().use2fa && user.isUse2fa && !user.isReset2fa) {
            if (!validateAuthCode(authenticatorCode, user)) {
                throwInvalidAuthenticatorCode()
            }
        } else {
            if (!authenticatorCode.isNullOrEmpty()) {
                throwInvalidAuthenticatorCode()
            }
        }
        if (!validatePassword(password, user)) {
            throwInvalidUser()
        }

        return stampLogin(user)
    }

    override fun authenticateSso(issuer: String, subject: String, email: String, name: String?, groups: List<String>): User {
        val providers = getGlobalConfig().ssoConfig.providers
        val provider = providers
            .find { it.issuerUri.trim() == issuer.trim() && it.enabled }
            ?: run {
                Loggers.SERVER.warn(
                    "SSO issuer '{}' not found among {} provider(s): {}",
                    issuer,
                    providers.size,
                    providers.map { "issuer=${it.issuerUri}, enabled=${it.enabled}" },
                )
                throw FailedLoginException("No SSO provider configured for issuer: $issuer")
            }

        if (provider.domainRestriction.isNotEmpty()) {
            val domain = email.substringAfter("@", "")
            if (domain.isBlank() || !domain.equals(provider.domainRestriction, ignoreCase = true)) {
                throw FailedLoginException("Email domain does not match the configured restriction.")
            }
        }

        userManager.getBySsoSubject(issuer, subject)?.let { return stampLogin(it) }

        if (subject.isNotEmpty()) {
            userManager.findOidcUserByLoginName(email, issuer)?.let { user ->
                user.ssoSubject = subject
                user.ssoIssuer = issuer.trim()
                if (user.email.isBlank()) user.email = email
                if (user.fullName.isBlank()) user.fullName = name?.takeIf { it.isNotBlank() } ?: email.substringBefore("@", email)
                return stampLogin(user)
            }
        }

        val accessLevel = evaluateSsoAccessRules(provider, groups)
            ?: throw FailedLoginException("Access denied. Contact your administrator.")

        val newUser = User()
        newUser.loginName = email
        newUser.userType = UserType.OIDC
        newUser.ssoIssuer = issuer
        newUser.ssoSubject = subject
        newUser.email = email
        newUser.fullName = email.substringBefore("@", email)
        newUser.accessLevel = accessLevel
        newUser.groupNames = arrayListOf(GroupHelper.ROOT_GROUP_ID)
        return stampLogin(newUser)
    }

    override fun connect(user: User, mode: MockMode): ServerConnection = when (mode) {
        MockMode.NONE -> connectionProvider.getObject(user)
        MockMode.SYNTHETIC -> mockConnectionProvider.getObject(user)
        MockMode.DEMO -> {
            if (SnapshotReplayConnection.isAvailable()) {
                snapshotConnectionProvider.getObject(user)
            } else {
                Loggers.SERVER.warn("Demo snapshot not available; falling back to synthetic mock")
                mockConnectionProvider.getObject(user)
            }
        }
    }

    private fun validateAuthCode(authenticatorCode: String?, user: User): Boolean =
        if (JvmGuardConfig.isIntegrationTest) {
            authenticatorCode == Server.TEST_AUTH_CODE
        } else {
            TOTP.validate(totpEncryption.decryptSecret(user.encryptedTotpSecret).value, authenticatorCode!!)
        }

    private fun findUser(loginName: String): User? {
        val directUser = userManager.getByLoginName(loginName)
        if (directUser != null) {
            return directUser
        }
        val ldapConfig = getLdapConfig()
        return if (ldapConfig.url.isNotEmpty()) {
            LdapHelper.findUser(loginName, ldapConfig)
        } else {
            null
        }
    }

    private fun validatePassword(password: String, user: User): Boolean =
        when (user.userType) {
            UserType.LOCAL -> {
                val passwordHash = user.passwordHash ?: return false
                PasswordHelper.validatePassword(password, passwordHash)
            }

            UserType.LDAP -> LdapHelper.validatePasswordLdap(password, user, getLdapConfig())
            UserType.OIDC -> false
        }

    private fun stampLogin(user: User): User {
        user.lastLogin = Instant.now()
        try {
            userManager.store(user)
        } catch (e: CredentialException) {
            Loggers.SERVER.warn("Could not store last login for {}", user, e)
        }
        return user
    }

    private fun evaluateSsoAccessRules(provider: SsoProviderConfig, groups: List<String>): AccessLevel? {
        val rules = provider.accessRules
        if (rules.isEmpty()) return null
        for (rule in rules) {
            if (!rule.isCatchAll && groups.contains(rule.claimValue)) {
                return rule.accessLevel
            }
        }
        for (rule in rules) {
            if (rule.isCatchAll) {
                return rule.accessLevel
            }
        }
        return null
    }

    private fun getLdapConfig(): LdapConfig = getGlobalConfig().ldapConfig

    private fun getGlobalConfig(): GlobalConfig = configManager.getGlobalConfig(false)

    override val idToTelemetryType: Map<String, TelemetryType>
        get() = telemetryProvider.idToTelemetryType

    companion object {
        private fun throwInvalidAuthenticatorCode(): Nothing =
            throw FailedLoginException("The authenticator code is not correct.")

        private fun throwInvalidUser(): Nothing =
            throw FailedLoginException("The combination of user and password is incorrect.")
    }
}
