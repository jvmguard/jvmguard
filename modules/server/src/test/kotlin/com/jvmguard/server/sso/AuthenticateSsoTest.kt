package com.jvmguard.server.sso

import com.jvmguard.common.JvmGuardDirectories
import com.jvmguard.common.JvmGuardProperties
import com.jvmguard.common.config.ConfigManager
import com.jvmguard.common.config.ConfigStorage
import com.jvmguard.common.helper.GroupHelper
import com.jvmguard.collector.api.TelemetryProvider
import com.jvmguard.connector.server.ServerImpl
import com.jvmguard.connector.server.mock.MockServerConnectionImpl
import com.jvmguard.connector.server.ServerConnectionImpl
import com.jvmguard.connector.totp.TotpEncryption
import com.jvmguard.data.config.GlobalConfig
import com.jvmguard.data.config.SsoGroupMapping
import com.jvmguard.data.config.SsoPreset
import com.jvmguard.data.config.SsoProviderConfig
import com.jvmguard.data.user.AccessLevel
import com.jvmguard.data.user.User
import com.jvmguard.data.user.UserManager
import com.jvmguard.data.user.UserType
import com.jvmguard.data.vmdata.CustomTelemetryInfo
import com.jvmguard.data.vmdata.TelemetryData
import com.jvmguard.data.vmdata.TelemetryInterval
import com.jvmguard.data.vmdata.TelemetryType
import com.jvmguard.data.vmdata.VM
import com.jvmguard.data.vmdata.CustomTelemetryNodeIdentifier
import org.h2.jdbcx.JdbcConnectionPool
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.ObjectProvider
import java.util.stream.Stream
import javax.sql.DataSource

class AuthenticateSsoTest {

    private lateinit var dataSource: DataSource
    private lateinit var configStorage: ConfigStorage
    private lateinit var userManager: UserManager
    private lateinit var configManager: ConfigManager
    private lateinit var server: ServerImpl

    private val issuer = "https://accounts.example.com"

    @BeforeEach
    fun setUp() {
        dataSource = JdbcConnectionPool.create("jdbc:h2:mem:sso-test;DB_CLOSE_DELAY=-1", "sa", "")
        dataSource.connection.use { conn ->
            conn.createStatement().execute(
                """
                CREATE TABLE IF NOT EXISTS config_storage (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    bean_type VARCHAR(255) NOT NULL,
                    content MEDIUMTEXT NOT NULL
                )
                """.trimIndent(),
            )
        }

        configStorage = ConfigStorage(dataSource)
        configManager = ConfigManager(configStorage)
        userManager = UserManager(configStorage)
        userManager.postConstruct()

        JvmGuardDirectories.init("build/sso-test-data", true, false)
        val properties = JvmGuardProperties()
        val totpEncryption = TotpEncryption(properties, JvmGuardDirectories.getInstance())

        server = ServerImpl(
            connectionProvider = dummyObjectProvider(),
            mockConnectionProvider = dummyObjectProvider(),
            snapshotConnectionProvider = dummyObjectProvider(),
            configManager = configManager,
            telemetryProvider = dummyTelemetryProvider(),
            userManager = userManager,
            properties = properties,
            totpEncryption = totpEncryption,
        )
    }

    @AfterEach
    fun tearDown() {
        (dataSource as JdbcConnectionPool).dispose()
    }

    private fun configureProvider(
        domainRestriction: String = "example.com",
        accessRules: List<SsoGroupMapping.() -> Unit> = emptyList(),
    ) {
        val config = GlobalConfig()
        val provider = SsoProviderConfig().apply {
            displayName = "Test IdP"
            preset = SsoPreset.GENERIC_OIDC
            issuerUri = issuer
            clientId = "client-id"
            clientSecret = "client-secret"
            enabled = true
            this.domainRestriction = domainRestriction
            this.accessRules = accessRules.map { rule ->
                SsoGroupMapping().apply(rule)
            }.toMutableList()
        }
        config.ssoConfig.providers.add(provider)
        configManager.setGlobalConfig(config, false)
    }

    @Test
    fun existingUserBySubjectReturnsImmediately() {
        configureProvider(accessRules = listOf({ claimValue = "*"; accessLevel = AccessLevel.VIEWER }))

        val user = User().apply {
            loginName = "alice@example.com"
            userType = UserType.OIDC
            ssoIssuer = issuer
            ssoSubject = "sub-123"
            email = "alice@example.com"
            accessLevel = AccessLevel.ADMIN
            groupNames = arrayListOf(GroupHelper.ROOT_GROUP_ID)
        }
        userManager.store(user)

        val result = server.authenticateSso(issuer, "sub-123", "alice@example.com", null, emptyList())
        assertEquals("alice@example.com", result.loginName)
        assertEquals(AccessLevel.ADMIN, result.accessLevel, "admin override on the row is respected")
    }

    @Test
    fun existingUserByEmailPinsSubject() {
        configureProvider(accessRules = listOf({ claimValue = "*"; accessLevel = AccessLevel.VIEWER }))

        val user = User().apply {
            loginName = "bob@example.com"
            userType = UserType.OIDC
            ssoIssuer = issuer
            ssoSubject = ""
            email = "bob@example.com"
            accessLevel = AccessLevel.PROFILER
            groupNames = arrayListOf(GroupHelper.ROOT_GROUP_ID)
        }
        userManager.store(user)

        val result = server.authenticateSso(issuer, "sub-456", "bob@example.com", null, emptyList())
        assertEquals("bob@example.com", result.loginName)
        assertEquals(AccessLevel.PROFILER, result.accessLevel)

        val stored = userManager.getByLoginName("bob@example.com")!!
        assertEquals("sub-456", stored.ssoSubject, "subject was pinned")
    }

    @Test
    fun noUserWithGroupMatchAutoProvisions() {
        configureProvider(accessRules = listOf(
            { claimValue = "admins"; accessLevel = AccessLevel.ADMIN },
            { claimValue = "*"; accessLevel = AccessLevel.VIEWER },
        ))

        val result = server.authenticateSso(issuer, "sub-789", "carol@example.com", null, listOf("admins"))
        assertEquals("carol@example.com", result.loginName)
        assertEquals(AccessLevel.ADMIN, result.accessLevel, "matched the 'admins' group rule")
        assertEquals(UserType.OIDC, result.userType)

        val stored = userManager.getByLoginName("carol@example.com")
        assertNotNull(stored, "user was auto-provisioned")
        assertEquals("sub-789", stored!!.ssoSubject)
    }

    @Test
    fun noUserWithCatchAllAutoProvisions() {
        configureProvider(accessRules = listOf(
            { claimValue = "*"; accessLevel = AccessLevel.VIEWER },
        ))

        val result = server.authenticateSso(issuer, "sub-000", "dave@example.com", null, emptyList())
        assertEquals(AccessLevel.VIEWER, result.accessLevel, "matched the catch-all")
    }

    @Test
    fun noUserWithEmptyRulesIsDenied() {
        configureProvider(accessRules = emptyList())

        assertThrows(javax.security.auth.login.FailedLoginException::class.java) {
            server.authenticateSso(issuer, "sub-aaa", "eve@example.com", null, listOf("admins"))
        }
    }

    @Test
    fun noUserWithRulesButNoMatchIsDenied() {
        configureProvider(accessRules = listOf(
            { claimValue = "admins"; accessLevel = AccessLevel.ADMIN },
        ))

        assertThrows(javax.security.auth.login.FailedLoginException::class.java) {
            server.authenticateSso(issuer, "sub-bbb", "frank@example.com", null, listOf("developers"))
        }
    }

    @Test
    fun domainRestrictionMismatchIsDenied() {
        configureProvider(domainRestriction = "example.com")

        assertThrows(javax.security.auth.login.FailedLoginException::class.java) {
            server.authenticateSso(issuer, "sub-ccc", "intruder@evil.com", null, listOf("admins"))
        }
    }

    @Test
    fun specificGroupWinsOverCatchAll() {
        configureProvider(accessRules = listOf(
            { claimValue = "viewers"; accessLevel = AccessLevel.VIEWER },
            { claimValue = "admins"; accessLevel = AccessLevel.ADMIN },
            { claimValue = "*"; accessLevel = AccessLevel.VIEWER },
        ))

        val result = server.authenticateSso(issuer, "sub-ddd", "grace@example.com", null, listOf("admins"))
        assertEquals(AccessLevel.ADMIN, result.accessLevel, "specific 'admins' rule wins over catch-all")
    }

    @Test
    fun unconfiguredIssuerIsDenied() {
        assertThrows(javax.security.auth.login.FailedLoginException::class.java) {
            server.authenticateSso("https://unknown-issuer.com", "sub-eee", "user@example.com", null, emptyList())
        }
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        private fun <T : Any> dummyObjectProvider(): ObjectProvider<T> = object : ObjectProvider<T> {
            override fun getObject(vararg args: Any?): T = throw UnsupportedOperationException()
            override fun getObject(): T = throw UnsupportedOperationException()
            override fun getIfAvailable(): T? = null
            override fun getIfUnique(): T? = null
            override fun iterator(): MutableIterator<T> = mutableListOf<T>().iterator()
            override fun stream(): Stream<T> = Stream.empty()
        }

        private fun dummyTelemetryProvider(): TelemetryProvider = object : TelemetryProvider {
            override val idToTelemetryType: Map<String, TelemetryType> = emptyMap()
            override val customTelemetryInfo: CustomTelemetryInfo = CustomTelemetryInfo(emptyList())
            override val hiddenDevOpsTelemetryNodes: Collection<String> = emptyList()
            override fun setDevOpsTelemetryNodeVisibility(nodeName: String, visible: Boolean): Boolean = false
            override fun getTelemetryData(vm: VM?, mainId: String, interval: TelemetryInterval, endTime: Long, plainHeap: Boolean): TelemetryData = throw UnsupportedOperationException()
            override fun getCustomTelemetryData(vm: VM?, nodeIdentifier: CustomTelemetryNodeIdentifier, interval: TelemetryInterval, endTime: Long): TelemetryData = throw UnsupportedOperationException()
        }
    }
}
