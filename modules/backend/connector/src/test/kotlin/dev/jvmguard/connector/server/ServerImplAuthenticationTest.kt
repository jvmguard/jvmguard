package dev.jvmguard.connector.server

import dev.jvmguard.agent.comm.CodecTypes
import dev.jvmguard.collector.api.TelemetryProvider
import dev.jvmguard.common.JvmGuardConfig
import dev.jvmguard.common.JvmGuardDirectories
import dev.jvmguard.common.JvmGuardProperties
import dev.jvmguard.common.config.ConfigManager
import dev.jvmguard.common.config.ConfigStorage
import dev.jvmguard.common.helper.LoginThrottle
import dev.jvmguard.common.helper.PasswordHelper
import dev.jvmguard.connector.totp.TOTP
import dev.jvmguard.connector.totp.TotpEncryption
import dev.jvmguard.connector.totp.TotpEncryptionTest
import dev.jvmguard.data.user.AccessLevel
import dev.jvmguard.data.user.User
import dev.jvmguard.data.user.UserManager
import dev.jvmguard.data.vmdata.CustomTelemetryInfo
import dev.jvmguard.data.vmdata.CustomTelemetryNodeIdentifier
import dev.jvmguard.data.vmdata.TelemetryData
import dev.jvmguard.data.vmdata.TelemetryInterval
import dev.jvmguard.data.vmdata.TelemetryType
import dev.jvmguard.data.vmdata.VM
import org.h2.jdbcx.JdbcDataSource
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.ObjectProvider
import java.util.Base64
import java.util.concurrent.atomic.AtomicInteger
import javax.security.auth.login.FailedLoginException
import javax.sql.DataSource
import kotlin.io.path.createTempDirectory

class ServerImplAuthenticationTest {

    private lateinit var userManager: UserManager
    private lateinit var configManager: ConfigManager
    private lateinit var totpEncryption: TotpEncryption
    private lateinit var server: ServerImpl

    private val originalProperties = JvmGuardConfig.properties()

    @BeforeEach
    fun setUp() {
        JvmGuardConfig.setProperties(JvmGuardProperties())
        val dataSource = freshDataSource()
        val configStorage = ConfigStorage(dataSource)
        userManager = UserManager(configStorage).apply { postConstruct() }
        configManager = ConfigManager(configStorage)
        totpEncryption = TotpEncryption(
            JvmGuardProperties().apply { totpKey = "direct:${TotpEncryptionTest.KEY_PROPERTY}" },
            JvmGuardDirectories.getInstance()
        )
        server = ServerImpl(
            unusedProvider(), unusedProvider(), unusedProvider(),
            configManager, unusedTelemetryProvider(), userManager, JvmGuardProperties(), totpEncryption, LoginThrottle()
        )
    }

    @AfterEach
    fun tearDown() {
        JvmGuardConfig.setProperties(originalProperties)
    }

    @Test
    fun passwordIsValidatedBeforeAuthenticatorCode() {
        val user = createUser("user", "correct")
        enable2fa(user)

        val e = assertThrows<FailedLoginException> { server.authenticate("user", "wrong", "000000") }
        assertEquals("The combination of user and password is incorrect.", e.message)
    }

    @Test
    fun missingAuthenticatorCodeFailsLogin() {
        val user = createUser("user", "correct")
        enable2fa(user)

        val e = assertThrows<FailedLoginException> { server.authenticate("user", "correct", null) }
        assertEquals("The authenticator code is not correct.", e.message)
    }

    @Test
    fun validPasswordAndCodeAuthenticate() {
        val user = createUser("user", "correct")
        enable2fa(user)

        val authenticated = server.authenticate("user", "correct", TOTP.generate(SECRET_HEX))
        assertEquals("user", authenticated.loginName)
    }

    @Test
    fun legacyTotpSecretIsMigratedOnLogin() {
        val user = createUser("user", "correct")
        user.isUse2fa = true
        user.encryptedTotpSecret = TotpEncryptionTest.legacyEncrypt(SECRET_HEX)
        userManager.store(user)
        configManager.getGlobalConfig(false).use2fa = true

        server.authenticate("user", "correct", TOTP.generate(SECRET_HEX))

        val stored = userManager.getByLoginName("user")!!
        assertTrue(stored.encryptedTotpSecret.startsWith("v2:"), "legacy secret must be re-encrypted with GCM")
        assertEquals(SECRET_HEX, totpEncryption.decryptSecret(stored.encryptedTotpSecret).value)
    }

    @Test
    fun passwordHashIsUpgradedOnLogin() {
        JvmGuardConfig.setProperties(JvmGuardProperties().apply { passwordIterations = 1000 })
        val user = createUser("user", "secret")
        val oldHash = user.passwordHash!!
        JvmGuardConfig.setProperties(JvmGuardProperties().apply { passwordIterations = 100_000 })

        server.authenticate("user", "secret", null)

        val updated = userManager.getByLoginName("user")!!.passwordHash!!
        assertNotEquals(oldHash, updated)
        assertFalse(PasswordHelper.needsRehash(updated))
        assertTrue(PasswordHelper.validatePassword("secret", updated))
    }

    @Test
    fun repeatedFailuresAreThrottled() {
        createUser("user", "correct")
        repeat(3) {
            assertThrows<FailedLoginException> { server.authenticate("user", "wrong", null) }
        }
        val e = assertThrows<FailedLoginException> { server.authenticate("user", "correct", null) }
        assertEquals("Too many failed login attempts. Please try again later.", e.message)
    }

    @Test
    fun successfulLoginResetsThrottling() {
        createUser("user", "correct")
        repeat(2) { assertThrows<FailedLoginException> { server.authenticate("user", "wrong", null) } }
        server.authenticate("user", "correct", null)
        repeat(2) { assertThrows<FailedLoginException> { server.authenticate("user", "wrong", null) } }
        server.authenticate("user", "correct", null)
    }

    private fun createUser(loginName: String, password: String): User {
        val user = User(loginName, "Full Name", PasswordHelper.createHash(password), "$loginName@example.com", AccessLevel.ADMIN)
        userManager.store(user)
        return user
    }

    private fun enable2fa(user: User) {
        user.isUse2fa = true
        user.encryptedTotpSecret = totpEncryption.encryptSecret(SECRET_HEX)
        userManager.store(user)
        configManager.getGlobalConfig(false).use2fa = true
    }

    companion object {
        private const val SECRET_HEX = "3132333435363738393031323334353637383930"
        private val DB_COUNTER = AtomicInteger()

        private fun freshDataSource(): DataSource {
            val dataSource = JdbcDataSource()
            dataSource.setURL("jdbc:h2:mem:auth-${DB_COUNTER.getAndIncrement()};DB_CLOSE_DELAY=-1")
            dataSource.connection.use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute(
                        "CREATE TABLE config_storage (" +
                            "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                            "bean_type VARCHAR(255) NOT NULL, " +
                            "content MEDIUMTEXT NOT NULL)"
                    )
                }
            }
            return dataSource
        }

        private fun <T : Any> unusedProvider(): ObjectProvider<T> = object : ObjectProvider<T> {
            override fun getObject(vararg args: Any?): T = throw UnsupportedOperationException()
            override fun getObject(): T = throw UnsupportedOperationException()
        }

        private fun unusedTelemetryProvider(): TelemetryProvider = object : TelemetryProvider {
            override val idToTelemetryType: Map<String, TelemetryType> = emptyMap()
            override val customTelemetryInfo: CustomTelemetryInfo get() = throw UnsupportedOperationException()
            override val hiddenDeclaredTelemetryNodes: Collection<String> = emptyList()
            override fun setDeclaredTelemetryNodeVisibility(nodeName: String, visible: Boolean) = throw UnsupportedOperationException()
            override fun getTelemetryData(vm: VM?, mainId: String, interval: TelemetryInterval, endTime: Long, plainHeap: Boolean): TelemetryData =
                throw UnsupportedOperationException()

            override fun getCustomTelemetryData(
                vm: VM?,
                nodeIdentifier: CustomTelemetryNodeIdentifier,
                interval: TelemetryInterval,
                endTime: Long
            ): TelemetryData = throw UnsupportedOperationException()
        }

        @BeforeAll
        @JvmStatic
        fun setUpClass() {
            CodecTypes.registerAll()
            System.setProperty(TotpEncryptionTest.KEY_PROPERTY, Base64.getEncoder().encodeToString(TotpEncryptionTest.KEY_BYTES))
            JvmGuardDirectories.init(createTempDirectory("jvmguard-auth-test").toString(), false, true)
        }
    }
}
