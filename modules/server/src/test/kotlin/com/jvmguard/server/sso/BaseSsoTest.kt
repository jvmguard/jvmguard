package com.jvmguard.server.sso

import com.jvmguard.common.JvmGuardDirectories
import com.jvmguard.common.JvmGuardProperties
import com.jvmguard.common.config.ConfigManager
import com.jvmguard.common.config.ConfigStorage
import com.jvmguard.collector.api.TelemetryProvider
import com.jvmguard.connector.server.ServerImpl
import com.jvmguard.connector.totp.TotpEncryption
import com.jvmguard.data.config.GlobalConfig
import com.jvmguard.data.config.SsoGroupMapping
import com.jvmguard.data.config.SsoPreset
import com.jvmguard.data.config.SsoProviderConfig
import com.jvmguard.data.user.UserManager
import com.jvmguard.data.vmdata.CustomTelemetryInfo
import com.jvmguard.data.vmdata.CustomTelemetryNodeIdentifier
import com.jvmguard.data.vmdata.TelemetryData
import com.jvmguard.data.vmdata.TelemetryInterval
import com.jvmguard.data.vmdata.TelemetryType
import com.jvmguard.data.vmdata.VM
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
        dataSource = JdbcConnectionPool.create("jdbc:h2:mem:sso-test;DB_CLOSE_DELAY=-1", "sa", "")
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
        )
    }

    @AfterEach
    fun tearDownBase() {
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
