package com.jvmguard.connector.server.mock

import com.jvmguard.agent.config.VmType
import com.jvmguard.agent.data.MethodInfo
import com.jvmguard.agent.mbean.MBeanData
import com.jvmguard.agent.mbean.MBeanModificationData
import com.jvmguard.agent.mbean.MBeanOperationData
import com.jvmguard.common.Loggers
import com.jvmguard.common.helper.Direction
import com.jvmguard.common.helper.ListModification
import com.jvmguard.common.update.InstallationInfo
import com.jvmguard.common.update.UpdateResult
import com.jvmguard.data.base.StoredConfig
import com.jvmguard.data.config.GlobalConfig
import com.jvmguard.data.config.GroupConfig
import com.jvmguard.data.config.external.ExternalConfig
import com.jvmguard.data.config.external.ServerInitConfig
import com.jvmguard.data.config.sets.*
import com.jvmguard.data.config.triggers.actions.RecordJfrAction
import com.jvmguard.data.config.triggers.actions.RecordJpsAction
import com.jvmguard.data.dashboard.Group
import com.jvmguard.data.file.SnapshotFile
import com.jvmguard.data.file.SnapshotFileType
import com.jvmguard.data.transactions.*
import com.jvmguard.data.user.InboxItem
import com.jvmguard.data.user.User
import com.jvmguard.data.user.viewsettings.ViewSettings
import com.jvmguard.data.vmdata.*
import com.jvmguard.connector.api.ServerConnection
import com.jvmguard.connector.api.ServerConnectionRegistry
import com.jvmguard.connector.api.log.LogFile
import com.jvmguard.connector.api.log.LogFileDescriptor
import com.jvmguard.connector.api.log.LogFileImpl
import com.jvmguard.connector.api.log.LogFileType
import com.jvmguard.connector.server.AbstractServerConnectionImpl
import com.jvmguard.connector.server.ServerConnectionImpl
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.util.*
import javax.management.MBeanAttributeInfo
import javax.management.MBeanOperationInfo

@Suppress("SpringJavaInjectionPointsAutowiringInspection", "SpringAutowiredFieldsWarningInspection")
@Component
@Primary
@Scope("prototype")
class MockServerConnectionImpl(override val user: User) : AbstractServerConnectionImpl() {

    private val random = Random(getRandomSeed())

    @field:Autowired
    private lateinit var serverConnectionRegistry: ServerConnectionRegistry // needed for strong reference to this ServerConnection

    @field:Autowired
    private lateinit var realConnectionProvider: ObjectProvider<ServerConnectionImpl>

    private lateinit var realConnection: ServerConnection
    private var globalConfig = GlobalConfig()

    private val mockMBeans = MockMBeans()
    private val mockEntities = MockEntities(this)
    private val mockTransactions = MockTransactions(this)
    private val mockTelemetries = MockTelemetries(this)

    // Wires the registry directly for browserless tests that construct the mock outside the Spring container.
    constructor(user: User, serverConnectionRegistry: ServerConnectionRegistry) : this(user) {
        this.serverConnectionRegistry = serverConnectionRegistry
    }

    @PostConstruct
    private fun register() {
        realConnection = realConnectionProvider.getObject(user)
        serverConnectionRegistry.add(this)
    }

    fun getRandom(): Random = random

    private fun getRandomSeed(): Long = 1234567890

    override val inboxItems: Collection<InboxItem>
        get() = mockEntities.getInboxItems()

    override fun getSnapshotFileSize(snapshotFile: SnapshotFile): Long = realConnection.getSnapshotFileSize(snapshotFile)

    override fun logout() {
        serverConnectionRegistry.remove(this)
    }

    override fun checkForUpdates(): UpdateResult = UpdateResult("1.0", "1.1")

    override val installationInfo: InstallationInfo
        get() = InstallationInfo("1.0", "1000")

    override fun saveViewSettings(viewSettings: ViewSettings) {
        realConnection.saveViewSettings(viewSettings)
    }

    override fun saveSelf(user: User) {
        applyListModification(ListModification(setOf(user), emptyList(), emptyList(), User::class.java))
    }

    override fun getLogFileDescriptors(logFileType: LogFileType): List<LogFileDescriptor> = ArrayList()

    override fun getLogFile(fileName: String): LogFile = LogFileImpl(fileName)

    override val users: Collection<User>
        get() = mockEntities.getUsers()

    override val loggedInUsers: Collection<User>
        get() = setOf(user)

    override fun getGlobalConfig(obfuscated: Boolean): GlobalConfig = globalConfig

    override fun applyInitConfig(serverInitConfig: ServerInitConfig) {
    }

    override fun setGlobalConfig(globalConfig: GlobalConfig) {
        this.globalConfig = globalConfig
    }

    override fun testSsoDiscovery(issuerUri: String): String = "Mock: discovery test skipped"

    override val groupConfigs: Collection<GroupConfig>
        get() = mockEntities.getGroupConfigs()

    override fun modifyGroupConfigs(listModification: ListModification<GroupConfig>) {
        applyListModification(listModification)
    }

    override val caps: EnumSet<CapType>
        get() = EnumSet.noneOf(CapType::class.java)

    override fun resetCaps() {
    }

    override fun readConfig(fileContent: ByteArray): ExternalConfig? = null

    override fun applyListModification(listModification: ListModification<out StoredConfig>) {
        mockEntities.applyListModification(listModification)
    }

    override val transactionDefSets: Collection<TransactionDefSet>
        get() = mockEntities.getTransactionDefSets()

    override val thresholdSets: Collection<ThresholdSet>
        get() = mockEntities.getThresholdSets()

    override val triggerSets: Collection<TriggerSet>
        get() = mockEntities.getTriggerSets()

    override val actionSets: Collection<ActionSet>
        get() = mockEntities.getActionsSets()

    override val telemetrySets: Collection<TelemetrySet>
        get() = mockEntities.getTelemetrySets()

    override fun getVmDataHolders(vmFilter: VmFilter, sparkLineRange: SparkLineRange, telemetryTypes: Collection<TelemetryType>): Group<VmDataHolder> =
        mockEntities.getVmDataHolders(vmFilter, sparkLineRange)

    override fun getVmDataHolder(vm: VM, sparkLineRange: SparkLineRange, telemetryTypes: Collection<TelemetryType>): VmDataHolder =
        mockEntities.getVmDataHolder(vm, sparkLineRange)

    override fun getGroupVmDataHolder(vmIdentifier: VmIdentifier?, sparkLineRange: SparkLineRange, telemetryTypes: Collection<TelemetryType>): VmDataHolder {
        val rootGroup = mockEntities.getVmDataHolders(VmFilter.CONNECTED, sparkLineRange)

        if (vmIdentifier == null || vmIdentifier.isRoot) {
            return rootGroup.data ?: error("no mock VM data holder for root group")
        }

        var currentGroup = rootGroup
        val names = vmIdentifier.name.split("/")
        for (i in names.indices) {
            currentGroup = if (i == names.size - 1) {
                currentGroup.getOrCreateGroupChild(VmIdentifier(names[i], vmIdentifier.type))
            } else {
                currentGroup.getOrCreateGroupChild(VmIdentifier(names[i], VmType.GROUP))
            }
        }

        return currentGroup.data ?: error("no mock VM data holder for $vmIdentifier")
    }

    override val idToTelemetryType: Map<String, TelemetryType>
        get() = mockEntities.getIdToSparkLineType()

    override val namedVms: Collection<VM>
        get() = mockEntities.getVmsWithGroups()

    override val connectedVms: Collection<VM>
        get() = mockEntities.getVmsWithGroups()

    override fun getCustomTelemetryData(vm: VM?, nodeIdentifier: CustomTelemetryNodeIdentifier, interval: TelemetryInterval, endTime: Long): TelemetryData =
        mockTelemetries.getCustomTelemetryData(nodeIdentifier, endTime - interval.timeExtent, endTime)

    override val customTelemetryInfo: CustomTelemetryInfo
        get() {
            val nodes = ArrayList(
                listOf(
                    CustomTelemetryNodeIdentifier(CustomTelemetryNodeIdentifier.Type.DEVOPS, "Request queue length"),
                    CustomTelemetryNodeIdentifier(CustomTelemetryNodeIdentifier.Type.MBEAN, "Cache entries"),
                ),
            )
            return CustomTelemetryInfo(nodes)
        }

    override fun setDevOpsTelemetryNodeVisibilities(nameToVisibility: Map<String, Boolean>) {
    }

    override val hiddenDevOpsTelemetryNodes: Collection<String>
        get() = emptyList()

    override fun getTelemetryData(vm: VM?, mainId: String, interval: TelemetryInterval, endTime: Long): TelemetryData =
        mockTelemetries.getTelemetryData(mainId, endTime - interval.timeExtent, endTime)

    override fun getPackageStats(vm: VM): Map<String, Int>? = null

    override fun getClassNames(vm: VM, allClasses: Boolean): Collection<String>? = null

    override fun getMethods(className: String, vm: VM): Collection<MethodInfo>? = null

    override fun getSnapshotFiles(snapshotFileType: SnapshotFileType?, vm: VM?): Collection<SnapshotFile> =
        mockEntities.getSnapshotFiles(snapshotFileType, vm)

    override fun getSnapshotFile(id: Long): SnapshotFile? = mockEntities.getSnapshotFile(id)

    override fun getTransactionTreeCursor(
        vm: VM?,
        interval: TransactionTreeInterval,
        transactionDataType: TransactionDataType,
        time: Long,
        timeRequirement: TimeRequirement
    ): TransactionCursor =
        mockTransactions.getTransactionTreeCursor(interval, time, timeRequirement)

    override fun getCurrentTransactionTreeCursor(vm: VM?, interval: TransactionTreeInterval, transactionDataType: TransactionDataType): TransactionCursor =
        mockTransactions.getCurrentTransactionTreeCursor(interval)

    override fun changeTransactionCursor(transactionCursor: TransactionCursor, vm: VM?, interval: TransactionTreeInterval): TransactionCursor =
        mockTransactions.changeTransactionCursor(transactionCursor, interval)

    override fun getCallTree(transactionCursor: TransactionCursor, mergePolicies: Boolean): TransactionTreeData =
        mockTransactions.getTransactionTreeData(transactionCursor)!!

    override fun getHotspots(transactionCursor: TransactionCursor, mergePolicies: Boolean): TransactionTreeData =
        mockTransactions.getTransactionTreeData(transactionCursor)!!

    override fun moveTransactionTreeCursor(cursor: TransactionCursor, direction: Direction): TransactionCursor =
        mockTransactions.moveTransactionCursor(cursor, direction)

    override fun runGC(vm: VM) {
        try {
            Thread.sleep(5000)
        } catch (e: InterruptedException) {
            Loggers.SERVER.warn("Interrupted during mock runGC", e)
            Thread.currentThread().interrupt()
        }
    }

    override fun heapDump(vm: VM) {
    }

    override fun threadDump(vm: VM) {
    }

    override fun recordJps(vm: VM, recordJpsAction: RecordJpsAction) {
    }

    override fun recordJfr(vm: VM, recordJfrAction: RecordJfrAction) {
    }

    override fun getMBeanNames(vm: VM, createPlatformServer: Boolean): Collection<String> = mockMBeans.getMBeanNames()

    override fun getMBeanData(vm: VM, name: String, fetchStructure: Boolean, fetchValues: Boolean): MBeanData =
        mockMBeans.getMBeanData(name, fetchStructure, fetchValues)

    override fun invokeMBeanOperation(vm: VM, name: String, operationInfo: MBeanOperationInfo, parameters: Array<Any?>): MBeanOperationData =
        mockMBeans.invoke(name, operationInfo, parameters)

    override fun setMBeanAttribute(vm: VM, name: String, attributeInfo: MBeanAttributeInfo, value: Any?): MBeanModificationData =
        mockMBeans.setAttribute(name, attributeInfo, value)

    override fun deleteVM(vm: VM): Boolean = false

    override fun modifyUsers(listModification: ListModification<User>) {
        applyListModification(listModification)
    }

    override fun getVms(ids: LongArray): Collection<VM> = emptyList()

    override fun getConnectedPooledVms(pool: VM): Collection<VM> = emptyList()

    override fun modifySnapshotFiles(listModification: ListModification<SnapshotFile>) {
    }

    override fun modifyInboxItems(listModification: ListModification<InboxItem>) {
    }
}
