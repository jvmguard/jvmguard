package dev.jvmguard.rest.restInterface

import dev.jvmguard.agent.comm.CodecTypes
import dev.jvmguard.agent.config.VmType
import dev.jvmguard.agent.data.MethodInfo
import dev.jvmguard.agent.mbean.MBeanData
import dev.jvmguard.agent.mbean.MBeanModificationData
import dev.jvmguard.agent.mbean.MBeanOperationData
import dev.jvmguard.collector.api.AgentConnection
import dev.jvmguard.collector.api.TelemetryProvider
import dev.jvmguard.collector.api.TransactionProvider
import dev.jvmguard.collector.api.VmManager
import dev.jvmguard.collector.util.BackupHandler
import dev.jvmguard.common.JvmGuardConfig
import dev.jvmguard.common.JvmGuardDirectories
import dev.jvmguard.common.JvmGuardProperties
import dev.jvmguard.common.config.ConfigManager
import dev.jvmguard.common.config.ConfigStorage
import dev.jvmguard.common.helper.PasswordHelper
import dev.jvmguard.data.config.triggers.actions.RecordJfrAction
import dev.jvmguard.data.config.triggers.actions.RecordJpsAction
import dev.jvmguard.data.dashboard.Group
import dev.jvmguard.data.transactions.*
import dev.jvmguard.data.user.AccessLevel
import dev.jvmguard.data.user.User
import dev.jvmguard.data.user.UserManager
import dev.jvmguard.data.vmdata.*
import dev.jvmguard.rest.entity.GroupEntity
import dev.jvmguard.rest.provider.RestException
import org.h2.jdbcx.JdbcDataSource
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.core.task.TaskExecutor
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import java.io.File
import java.util.EnumSet
import java.util.concurrent.atomic.AtomicInteger
import javax.management.MBeanAttributeInfo
import javax.management.MBeanOperationInfo
import javax.sql.DataSource
import kotlin.io.path.createTempDirectory

class RestInterfaceGroupScopeTest {

    private lateinit var userManager: UserManager
    private lateinit var restInterface: RestInterfaceImpl

    private val originalProperties = JvmGuardConfig.properties()

    @BeforeEach
    fun setUp() {
        JvmGuardConfig.setProperties(JvmGuardProperties())
        val dataSource = freshDataSource()
        val configStorage = ConfigStorage(dataSource)
        userManager = UserManager(configStorage).apply { postConstruct() }
        val configManager = ConfigManager(configStorage)
        configManager.groupConnected(VmIdentifier("Demo", VmType.GROUP))
        configManager.groupConnected(VmIdentifier("Demo/Storefront", VmType.POOL))
        configManager.groupConnected(VmIdentifier("Prod", VmType.GROUP))
        restInterface = RestInterfaceImpl(
            userManager, configManager, vmManager(),
            RestTelemetryProvider(unusedTelemetryProvider()), unusedTransactionProvider(),
            BackupHandler(TaskExecutor { it.run() }, JvmGuardDirectories.getInstance(), dataSource)
        )
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
        JvmGuardConfig.setProperties(originalProperties)
    }

    @Test
    fun adminSeesAllGroups() {
        authenticateAs(createUser("admin", AccessLevel.ADMIN).loginName)
        assertEquals(listOf("Demo", "Demo/Storefront", "Prod"), restInterface.getGroups().map { it.name })
    }

    @Test
    fun scopedViewerSeesOnlyOwnGroups() {
        authenticateAs(createUser("viewer", AccessLevel.VIEWER, "Demo").loginName)
        assertEquals(listOf("Demo", "Demo/Storefront"), restInterface.getGroups().map { it.name })
    }

    @Test
    fun scopedViewerSeesOnlyOwnVms() {
        authenticateAs(createUser("viewer", AccessLevel.VIEWER, "Demo").loginName)
        assertEquals(listOf("Demo/shop"), restInterface.getVms(null, false))
    }

    @Test
    fun scopedViewerCannotQueryForeignGroup() {
        authenticateAs(createUser("viewer", AccessLevel.VIEWER, "Demo").loginName)
        val e = assertThrows<RestException> { restInterface.getVms("Prod", false) }
        assertEquals(HttpStatus.FORBIDDEN, e.status)
    }

    @Test
    fun adminCanQueryAnyGroup() {
        authenticateAs(createUser("admin", AccessLevel.ADMIN).loginName)
        assertEquals(listOf("Prod/api"), restInterface.getVms("Prod", false))
    }

    @Test
    fun scopedViewerCannotResolveForeignVm() {
        authenticateAs(createUser("viewer", AccessLevel.VIEWER, "Demo").loginName)
        val e = assertThrows<RestException> {
            restInterface.getTelemetry("Prod/api", null, "heap", TelemetryInterval.TEN_MINUTES, 0L)
        }
        assertEquals(HttpStatus.FORBIDDEN, e.status)
    }

    @Test
    fun missingAuthenticationSeesNothing() {
        assertEquals(emptyList<GroupEntity>(), restInterface.getGroups())
        assertEquals(emptyList<String>(), restInterface.getVms(null, false))
    }

    @Test
    fun apiKeyHashIsUpgradedOnUse() {
        JvmGuardConfig.setProperties(JvmGuardProperties().apply { passwordIterations = 1000 })
        val user = createUser("api", AccessLevel.VIEWER)
        user.apiKeyHash = PasswordHelper.createHash("key")
        userManager.store(user)
        val oldHash = user.apiKeyHash
        JvmGuardConfig.setProperties(JvmGuardProperties().apply { passwordIterations = 100_000 })

        assertEquals(AccessLevel.VIEWER, restInterface.checkAccess("api", "key"))
        val updated = userManager.getByLoginName("api")!!.apiKeyHash
        assertNotEquals(oldHash, updated)
        assertFalse(PasswordHelper.needsRehash(updated))
    }

    private fun createUser(loginName: String, level: AccessLevel, vararg groups: String): User {
        val user = User(loginName, "Full Name", PasswordHelper.createHash("pw"), "$loginName@example.com", level)
        user.groupNames = arrayListOf(*groups)
        userManager.store(user)
        return user
    }

    private fun authenticateAs(loginName: String) {
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(loginName, "n/a")
    }

    companion object {
        private val DB_COUNTER = AtomicInteger()

        private val VMS = listOf(
            VM(VmType.GROUP, 1, 0, "", ""),
            VM(VmType.GROUP, 2, 0, "Demo", ""),
            VM(VmType.POOL, 3, 0, "Storefront", "Demo"),
            VM(VmType.GROUP, 4, 0, "Prod", ""),
            VM(VmType.NAMED, 5, 0, "shop", "Demo"),
            VM(VmType.NAMED, 6, 0, "api", "Prod"),
        )

        private fun freshDataSource(): DataSource {
            val dataSource = JdbcDataSource()
            dataSource.setURL("jdbc:h2:mem:rest-${DB_COUNTER.getAndIncrement()};DB_CLOSE_DELAY=-1")
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

        private fun vmManager(): VmManager = object : VmManager {
            override val currentConnections: List<Connection> = emptyList()
            override val namedVms: Collection<VM> = VMS
            override val rootGroupVM: VM = VMS.first()
            override val agentKeystore: File get() = throw UnsupportedOperationException()
            override fun getConnection(vm: VM): AgentConnection = throw UnsupportedOperationException()
            override fun getVmDataHolders(
                vmFilter: VmFilter,
                sparkLineRange: SparkLineRange,
                telemetryTypes: Collection<TelemetryType>
            ): Group<VmDataHolder> = throw UnsupportedOperationException()

            override fun getGroupVmDataHolder(
                vmFilter: VmFilter,
                vmIdentifier: VmIdentifier?,
                sparkLineRange: SparkLineRange,
                telemetryTypes: Collection<TelemetryType>
            ): VmDataHolder = throw UnsupportedOperationException()

            override fun getVmDataHolder(
                vmFilter: VmFilter,
                vm: VM,
                sparkLineRange: SparkLineRange,
                telemetryTypes: Collection<TelemetryType>
            ): VmDataHolder = throw UnsupportedOperationException()

            override fun getPackageStats(vm: VM): Map<String, Int> = throw UnsupportedOperationException()
            override fun getClassNames(vm: VM, allClasses: Boolean): Collection<String> = throw UnsupportedOperationException()
            override fun getMethods(className: String, vm: VM): Collection<MethodInfo> = throw UnsupportedOperationException()
            override fun runGC(vm: VM) = throw UnsupportedOperationException()
            override fun heapDump(vm: VM, user: User) = throw UnsupportedOperationException()
            override fun threadDump(vm: VM, user: User) = throw UnsupportedOperationException()
            override fun recordJps(vm: VM, user: User, recordJpsAction: RecordJpsAction) = throw UnsupportedOperationException()
            override fun recordJfr(vm: VM, user: User, recordJfrAction: RecordJfrAction) = throw UnsupportedOperationException()
            override fun deleteVM(vm: VM): Boolean = throw UnsupportedOperationException()
            override fun getVms(ids: LongArray): Collection<VM> = throw UnsupportedOperationException()
            override fun getConnectedPooledVms(pool: VM): Collection<VM> = throw UnsupportedOperationException()
            override fun terminate(vm: VM, closeConnection: Boolean) = throw UnsupportedOperationException()
            override fun getMBeanNames(vm: VM, createPlatformServer: Boolean): Collection<String> = throw UnsupportedOperationException()
            override fun getMBeanData(vm: VM, name: String, fetchStructure: Boolean, fetchValues: Boolean): MBeanData =
                throw UnsupportedOperationException()

            override fun invokeMBeanOperation(vm: VM, name: String, operationInfo: MBeanOperationInfo, parameters: Array<Any?>): MBeanOperationData =
                throw UnsupportedOperationException()

            override fun setMBeanAttribute(vm: VM, name: String, attributeInfo: MBeanAttributeInfo, value: Any?): MBeanModificationData =
                throw UnsupportedOperationException()
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

        private fun unusedTransactionProvider(): TransactionProvider = object : TransactionProvider {
            override val caps: EnumSet<CapType> = EnumSet.noneOf(CapType::class.java)
            override fun getTransactionTreeCursor(
                vm: VM?,
                interval: TransactionTreeInterval,
                transactionDataType: TransactionDataType,
                time: Long,
                timeRequirement: TimeRequirement
            ): TransactionCursor = throw UnsupportedOperationException()

            override fun getCurrentTransactionTreeCursor(vm: VM?, interval: TransactionTreeInterval, transactionDataType: TransactionDataType): TransactionCursor =
                throw UnsupportedOperationException()

            override fun changeTransactionCursor(transactionCursor: TransactionCursor, vm: VM?, interval: TransactionTreeInterval): TransactionCursor =
                throw UnsupportedOperationException()

            override fun getNextTransactionCursor(cursor: TransactionCursor): TransactionCursor = throw UnsupportedOperationException()
            override fun getPreviousTransactionCursor(cursor: TransactionCursor): TransactionCursor = throw UnsupportedOperationException()
            override fun getCallTree(transactionCursor: TransactionCursor, mergePolicies: Boolean): TransactionTreeData =
                throw UnsupportedOperationException()

            override fun getHotspots(transactionCursor: TransactionCursor, mergePolicies: Boolean): TransactionTreeData =
                throw UnsupportedOperationException()

            override fun getTransactionInfo(transactionCursor: TransactionCursor): Set<TransactionInfo> = throw UnsupportedOperationException()
            override fun resetCapCount(ifCappedOnly: Boolean) = throw UnsupportedOperationException()
        }

        @BeforeAll
        @JvmStatic
        fun setUpClass() {
            CodecTypes.registerAll()
            JvmGuardDirectories.init(createTempDirectory("jvmguard-rest-test").toString(), false, true)
        }
    }
}
