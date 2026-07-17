package dev.jvmguard.server.sso

import dev.jvmguard.common.JvmGuardDirectories
import dev.jvmguard.common.JvmGuardProperties
import dev.jvmguard.common.config.ConfigManager
import dev.jvmguard.common.config.ConfigStorage
import dev.jvmguard.common.helper.LoginThrottle
import dev.jvmguard.collector.api.TelemetryProvider
import dev.jvmguard.connector.server.ServerImpl
import dev.jvmguard.connector.totp.TotpEncryption
import dev.jvmguard.data.config.GlobalConfig
import dev.jvmguard.data.config.SsoGroupMapping
import dev.jvmguard.data.config.SsoPreset
import dev.jvmguard.data.config.SsoProviderConfig
import dev.jvmguard.data.user.UserManager
import dev.jvmguard.data.vmdata.CustomTelemetryInfo
import dev.jvmguard.data.vmdata.CustomTelemetryNodeIdentifier
import dev.jvmguard.data.vmdata.TelemetryData
import dev.jvmguard.data.vmdata.TelemetryInterval
import dev.jvmguard.data.vmdata.TelemetryType
import dev.jvmguard.data.vmdata.VM
import org.h2.jdbcx.JdbcConnectionPool
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.ObjectProvider
import java.util.stream.Stream
import javax.sql.DataSource

abstract class BaseSsoTest {

    protected lateinit var dataSource: DataSource
    protected lateinit var configStorage: ConfigStorage
    protected lateinit var userManager: UserManager
    protected lateinit var configManager: ConfigManager
    protected lateinit var server: ServerImpl

    @BeforeEach
    fun setUpBase() {
        val dbName = "sso-test-${DB_COUNTER.incrementAndGet()}"
        dataSource = JdbcConnectionPool.create("jdbc:h2:mem:$dbName;DB_CLOSE_DELAY=-1", "sa", "")
        dataSource.connection.use { conn ->
            @Suppress("SqlNoDataSourceInspection")
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

        JvmGuardDirectories.init("build/sso-test-data", integrationTest = true, dataDirectoryExplicit = false)
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
            loginThrottle = LoginThrottle(),
        )
    }

    @AfterEach
    fun tearDownBase() {
        runCatching { dataSource.connection.use { it.createStatement().execute("SHUTDOWN") } }
        (dataSource as JdbcConnectionPool).dispose()
    }

    protected fun configureProvider(
        issuer: String,
        domainRestriction: String = "",
        preset: SsoPreset = SsoPreset.GENERIC_OIDC,
        accessRules: List<SsoGroupMapping> = emptyList(),
    ) {
        val config = GlobalConfig()
        val provider = SsoProviderConfig().apply {
            displayName = "Test IdP"
            this.preset = preset
            this.issuerUri = issuer
            clientId = "client-id"
            clientSecret = "client-secret"
            enabled = true
            this.domainRestriction = domainRestriction
            this.accessRules = accessRules.toMutableList()
        }
        config.ssoConfig.providers.add(provider)
        configManager.setGlobalConfig(config, false)
    }

    companion object {
        private val DB_COUNTER = java.util.concurrent.atomic.AtomicLong()

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
            override val hiddenDeclaredTelemetryNodes: Collection<String> = emptyList()
            override fun setDeclaredTelemetryNodeVisibility(nodeName: String, visible: Boolean): Boolean = false
            override fun getTelemetryData(vm: VM?, mainId: String, interval: TelemetryInterval, endTime: Long, plainHeap: Boolean): TelemetryData =
                throw UnsupportedOperationException()

            override fun getCustomTelemetryData(
                vm: VM?,
                nodeIdentifier: CustomTelemetryNodeIdentifier,
                interval: TelemetryInterval,
                endTime: Long
            ): TelemetryData = throw UnsupportedOperationException()
        }
    }
}
