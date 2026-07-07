package com.jvmguard.connector.server

import com.jvmguard.agent.data.MethodInfo
import com.jvmguard.agent.mbean.MBeanData
import com.jvmguard.agent.mbean.MBeanModificationData
import com.jvmguard.agent.mbean.MBeanOperationData
import com.jvmguard.collector.api.TelemetryProvider
import com.jvmguard.collector.api.TransactionProvider
import com.jvmguard.collector.api.VmManager
import com.jvmguard.collector.main.SnapshotFileStorage
import com.jvmguard.common.Loggers
import com.jvmguard.common.config.ConfigManager
import com.jvmguard.common.config.ConfigStorage
import com.jvmguard.common.config.ImportManager
import com.jvmguard.common.helper.Direction
import com.jvmguard.common.helper.ListModification
import com.jvmguard.common.notification.InboxManager
import com.jvmguard.common.notification.ModificationEvent
import com.jvmguard.common.notification.ModificationType
import com.jvmguard.common.update.InstallationInfo
import com.jvmguard.common.update.UpdateManager
import com.jvmguard.common.update.UpdateResult
import com.jvmguard.data.base.StoredConfig
import com.jvmguard.data.config.GlobalConfig
import com.jvmguard.data.config.GroupConfig
import com.jvmguard.data.config.external.ExternalConfig
import com.jvmguard.data.config.external.ServerInitConfig
import com.jvmguard.data.config.sets.*
import com.jvmguard.data.config.triggers.actions.RecordArtifactAction
import com.jvmguard.data.config.triggers.actions.RecordJfrAction
import com.jvmguard.data.config.triggers.actions.RecordJpsAction
import com.jvmguard.data.dashboard.Group
import com.jvmguard.data.file.SnapshotFile
import com.jvmguard.data.file.SnapshotFileType
import com.jvmguard.data.transactions.*
import com.jvmguard.data.user.*
import com.jvmguard.data.user.viewsettings.ViewSettings
import com.jvmguard.data.vmdata.*
import com.jvmguard.connector.api.ServerConnectionRegistry
import com.jvmguard.connector.api.log.LogFile
import com.jvmguard.connector.api.log.LogFileDescriptor
import com.jvmguard.connector.api.log.LogFileHandler
import com.jvmguard.connector.api.log.LogFileType
import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.io.File
import java.util.*
import javax.management.MBeanAttributeInfo
import javax.management.MBeanOperationInfo
import javax.security.auth.login.CredentialException

// One ServerConnection per logged-in user: a prototype bean whose constructor takes the assisted User argument
// (so ObjectProvider.getObject(user) matches it) while the collaborators are injected as fields afterward.
@Suppress("SpringAutowiredFieldsWarningInspection")
@Component
@Scope("prototype")
class ServerConnectionImpl(@Suppress("SpringJavaInjectionPointsAutowiringInspection") override var user: User) : AbstractServerConnectionImpl() {

    @field:Autowired
    private lateinit var serverConnectionRegistry: ServerConnectionRegistry
    @field:Autowired
    private lateinit var eventPublisher: ApplicationEventPublisher
    @field:Autowired
    private lateinit var configManager: ConfigManager
    @field:Autowired
    private lateinit var serverControl: ServerControl
    @field:Autowired
    private lateinit var vmManager: VmManager
    @field:Autowired
    private lateinit var telemetryProvider: TelemetryProvider
    @field:Autowired
    private lateinit var transactionProvider: TransactionProvider
    @field:Autowired
    private lateinit var snapshotFileStorage: SnapshotFileStorage
    @field:Autowired
    private lateinit var inboxManager: InboxManager
    @field:Autowired
    private lateinit var userManager: UserManager
    @field:Autowired
    private lateinit var configStorage: ConfigStorage
    @field:Autowired
    private lateinit var importManager: ImportManager
    @field:Autowired
    private lateinit var updateManager: UpdateManager

    private lateinit var logFileHandler: LogFileHandler

    @PostConstruct
    private fun register() {
        serverConnectionRegistry.add(this)
        logFileHandler = LogFileHandler(user.accessLevel)
    }

    override fun logout() {
        serverConnectionRegistry.remove(this)
    }

    override fun checkForUpdates(): UpdateResult? = updateManager.checkForUpdates()

    override val installationInfo: InstallationInfo
        get() = updateManager.getInstallationInfo()

    override fun saveViewSettings(viewSettings: ViewSettings) {
        // This is called when Vaadin sessions are destroyed, which also happens when the server is shut down
        // In that case, the database is already gone
        if (!serverControl.isShuttingDown) {
            user.viewSettings = viewSettings
            try {
                userManager.store(user)
            } catch (e: CredentialException) {
                SERVER_LOGGER.error("unexpected credential error", e)
            }
        }
    }

    override fun saveSelf(user: User) {
        this@ServerConnectionImpl.user.copyFromRestricted(user)
        try {
            userManager.store(this@ServerConnectionImpl.user)
        } catch (e: CredentialException) {
            SERVER_LOGGER.error("unexpected credential error", e)
        }
    }

    override fun getLogFileDescriptors(logFileType: LogFileType): List<LogFileDescriptor> =
        logFileHandler.getLogFileDescriptors(logFileType)

    override fun getLogFile(fileName: String): LogFile = logFileHandler.getLogFile(fileName)

    override val inboxItems: Collection<InboxItem>
        get() = inboxManager.getInboxItems(user)

    override fun getSnapshotFileSize(snapshotFile: SnapshotFile): Long = snapshotFile.file.length()

    @get:RequireAdmin
    override val users: Collection<User>
        get() = userManager.getAllUsers()

    @get:RequireAdmin
    override val loggedInUsers: Collection<User>
        get() = serverConnectionRegistry.getLoggedInUsers()

    override fun getGlobalConfig(obfuscated: Boolean): GlobalConfig = configManager.getGlobalConfig(obfuscated)

    @RequireAdmin
    override fun applyInitConfig(serverInitConfig: ServerInitConfig) {
        importManager.importServerInitConfig(serverInitConfig, user)
    }

    @RequireAdmin
    override fun setGlobalConfig(globalConfig: GlobalConfig) {
        configManager.setGlobalConfig(globalConfig, false)
    }

    @RequireAdmin
    override fun testSsoDiscovery(issuerUri: String): String {
        val baseUri = issuerUri.trim().trimEnd('/')
        val discoveryUrl = "$baseUri/.well-known/openid-configuration"
        return try {
            val client = java.net.http.HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build()
            val request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(discoveryUrl))
                .timeout(java.time.Duration.ofSeconds(10))
                .GET()
                .build()
            val response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() == 200) {
                "Discovery OK: $discoveryUrl is reachable."
            } else {
                "Discovery failed: HTTP ${response.statusCode()} from $discoveryUrl"
            }
        } catch (e: Exception) {
            "Discovery failed: ${e.message}"
        }
    }

    override val groupConfigs: Collection<GroupConfig>
        get() = configManager.getGroupConfigs(user.accessLevel, user.groupNames)

    @RequireProfiler
    override fun modifyGroupConfigs(listModification: ListModification<GroupConfig>) {
        configManager.modifyGroupConfigs(listModification, user.accessLevel, user.groupNames)
        transactionProvider.resetCapCount(true)
    }

    override fun resetCaps() {
        transactionProvider.resetCapCount(false)
    }

    override val caps: EnumSet<CapType>
        get() = transactionProvider.caps

    override fun readConfig(fileContent: ByteArray): ExternalConfig? =
        importManager.readConfig(ByteArrayInputStream(fileContent))

    override fun modifyInboxItems(listModification: ListModification<InboxItem>) {
        eventPublisher.publishEvent(ModificationEvent(this, user, ModificationType.INBOX))
        for (inboxItem in listModification.removedItems) {
            inboxManager.delete(inboxItem)
        }
        for (inboxItem in listModification.modifiedItems) {
            inboxManager.modifyItemRead(inboxItem)
        }
    }

    @RequireProfiler
    override fun modifySnapshotFiles(listModification: ListModification<SnapshotFile>) {
        for (snapshotFile in listModification.removedItems) {
            checkVmGroupAccess(snapshotFile.vm)
            if (snapshotFile.id != null) {
                snapshotFileStorage.delete(snapshotFile)
            }
        }
    }

    @RequireAdmin
    override fun modifyUsers(listModification: ListModification<User>) {
        userManager.modifyUsers(listModification)

        val modifiedCurrentUser = getModifiedCurrentUser(listModification.modifiedItems)
        if (modifiedCurrentUser != null) {
            user = modifiedCurrentUser
        }
    }

    private fun getModifiedCurrentUser(users: Collection<User>): User? =
        users.firstOrNull { user.id == it.id }

    override fun getVms(ids: LongArray): Collection<VM> = vmManager.getVms(ids)

    override fun getConnectedPooledVms(pool: VM): Collection<VM> = vmManager.getConnectedPooledVms(pool)

    @RequireProfiler
    @Suppress("UNCHECKED_CAST")
    override fun applyListModification(listModification: ListModification<out StoredConfig>) {
        val itemClass: Class<out StoredConfig> = listModification.itemClass
        try {
            if (StoredConfig::class.java.isAssignableFrom(itemClass)) {
                val xmlItemClass = itemClass as Class<StoredConfig>
                for (item in listModification.modifiedItems) {
                    configStorage.store(xmlItemClass, xmlItemClass.cast(item))
                }
                for (item in listModification.removedItems) {
                    configStorage.remove(xmlItemClass, item.id)
                }
                for (item in listModification.newItems) {
                    configStorage.store(xmlItemClass, xmlItemClass.cast(item))
                }
            }
        } catch (e: Throwable) {
            SERVER_LOGGER.error("error applying list modification", e)
        }
    }

    override val transactionDefSets: Collection<TransactionDefSet>
        get() = configStorage.list(TransactionDefSet::class.java)

    override val thresholdSets: Collection<ThresholdSet>
        get() = configStorage.list(ThresholdSet::class.java)

    override val triggerSets: Collection<TriggerSet>
        get() = configStorage.list(TriggerSet::class.java)

    override val actionSets: Collection<ActionSet>
        get() = configStorage.list(ActionSet::class.java)

    override val telemetrySets: Collection<TelemetrySet>
        get() = configStorage.list(TelemetrySet::class.java)

    override fun getVmDataHolders(vmFilter: VmFilter, sparkLineRange: SparkLineRange, telemetryTypes: Collection<TelemetryType>): Group<VmDataHolder> =
        vmManager.getVmDataHolders(vmFilter, sparkLineRange, telemetryTypes)

    override fun getGroupVmDataHolder(vmIdentifier: VmIdentifier?, sparkLineRange: SparkLineRange, telemetryTypes: Collection<TelemetryType>): VmDataHolder =
        vmManager.getGroupVmDataHolder(VmFilter.RECENT, vmIdentifier, sparkLineRange, telemetryTypes)

    override fun getVmDataHolder(vm: VM, sparkLineRange: SparkLineRange, telemetryTypes: Collection<TelemetryType>): VmDataHolder =
        vmManager.getVmDataHolder(VmFilter.RECENT, vm, sparkLineRange, telemetryTypes)

    override val idToTelemetryType: Map<String, TelemetryType>
        get() = telemetryProvider.idToTelemetryType

    override val namedVms: Collection<VM>
        get() = ArrayList(vmManager.namedVms)

    override val connectedVms: Collection<VM>
        get() = vmManager.currentConnections.map { it.vm }

    override val customTelemetryInfo: CustomTelemetryInfo
        get() = telemetryProvider.customTelemetryInfo

    @RequireAdmin
    override fun setDeclaredTelemetryNodeVisibilities(nameToVisibility: Map<String, Boolean>) {
        for ((name, visibility) in nameToVisibility) {
            telemetryProvider.setDeclaredTelemetryNodeVisibility(name, visibility)
        }
    }

    override val hiddenDeclaredTelemetryNodes: Collection<String>
        get() = telemetryProvider.hiddenDeclaredTelemetryNodes

    override fun getCustomTelemetryData(vm: VM?, nodeIdentifier: CustomTelemetryNodeIdentifier, interval: TelemetryInterval, endTime: Long): TelemetryData =
        telemetryProvider.getCustomTelemetryData(vm, nodeIdentifier, interval, endTime)

    override fun getTelemetryData(vm: VM?, mainId: String, interval: TelemetryInterval, endTime: Long): TelemetryData =
        telemetryProvider.getTelemetryData(vm, mainId, interval, endTime, false)

    override fun getPackageStats(vm: VM): Map<String, Int> = vmManager.getPackageStats(vm)

    override fun getClassNames(vm: VM, allClasses: Boolean): Collection<String> = vmManager.getClassNames(vm, allClasses)

    override fun getMethods(className: String, vm: VM): Collection<MethodInfo> = vmManager.getMethods(className, vm)

    override fun getSnapshotFiles(snapshotFileType: SnapshotFileType?, vm: VM?): Collection<SnapshotFile> =
        snapshotFileStorage.getSnapshotFiles(snapshotFileType, updateNull(vm))

    override fun getSnapshotFile(id: Long): SnapshotFile? = snapshotFileStorage.load(id)

    @RequireProfiler
    override fun runGC(vm: VM) {
        checkVmGroupAccess(vm)
        vmManager.runGC(vm)
    }

    @RequireProfiler
    override fun heapDump(vm: VM) {
        checkVmGroupAccess(vm)
        vmManager.heapDump(vm, user)
    }

    @RequireProfiler
    override fun threadDump(vm: VM) {
        checkVmGroupAccess(vm)
        vmManager.threadDump(vm, user)
    }

    private fun updateNull(vm: VM?): VM = vm ?: vmManager.rootGroupVM

    override fun getTransactionTreeCursor(
        vm: VM?,
        interval: TransactionTreeInterval,
        transactionDataType: TransactionDataType,
        time: Long,
        timeRequirement: TimeRequirement
    ): TransactionCursor =
        transactionProvider.getTransactionTreeCursor(vm, interval, transactionDataType, time, timeRequirement)

    override fun getCurrentTransactionTreeCursor(vm: VM?, interval: TransactionTreeInterval, transactionDataType: TransactionDataType): TransactionCursor =
        transactionProvider.getCurrentTransactionTreeCursor(vm, interval, transactionDataType)

    override fun changeTransactionCursor(transactionCursor: TransactionCursor, vm: VM?, interval: TransactionTreeInterval): TransactionCursor =
        transactionProvider.changeTransactionCursor(transactionCursor, vm, interval)

    override fun moveTransactionTreeCursor(cursor: TransactionCursor, direction: Direction): TransactionCursor =
        when (direction) {
            Direction.CURRENT -> cursor
            Direction.NEXT -> transactionProvider.getNextTransactionCursor(cursor)
            Direction.PREVIOUS -> transactionProvider.getPreviousTransactionCursor(cursor)
        }

    override fun getMBeanNames(vm: VM, createPlatformServer: Boolean): Collection<String> {
        if (vm.isGroupNode) {
            return emptyList()
        }
        return vmManager.getMBeanNames(vm, createPlatformServer)
    }

    override fun getMBeanData(vm: VM, name: String, fetchStructure: Boolean, fetchValues: Boolean): MBeanData? {
        if (vm.isGroupNode) {
            return null
        }
        return vmManager.getMBeanData(vm, name, fetchStructure, fetchValues)
    }

    @RequireProfiler
    override fun invokeMBeanOperation(vm: VM, name: String, operationInfo: MBeanOperationInfo, parameters: Array<Any?>): MBeanOperationData {
        checkVmGroupAccess(vm)
        val error = checkMBeanParameters(vm, name, operationInfo)
        return if (error != null) {
            object : MBeanOperationData {
                override fun getErrorMessage(): String = error
                override fun getStackTrace(): String? = null
                override fun getReturnValue(): Any? = null
            }
        } else {
            vmManager.invokeMBeanOperation(vm, name, operationInfo, parameters)
        }
    }

    @RequireProfiler
    override fun setMBeanAttribute(vm: VM, name: String, attributeInfo: MBeanAttributeInfo, value: Any?): MBeanModificationData {
        checkVmGroupAccess(vm)
        val error = checkMBeanParameters(vm, name, attributeInfo)
        return if (error != null) {
            object : MBeanModificationData {
                override fun getErrorMessage(): String = error
                override fun getStackTrace(): String? = null
            }
        } else {
            vmManager.setMBeanAttribute(vm, name, attributeInfo, value)
        }
    }

    override fun getCallTree(transactionCursor: TransactionCursor, mergePolicies: Boolean): TransactionTreeData =
        transactionProvider.getCallTree(transactionCursor, mergePolicies)

    override fun getHotspots(transactionCursor: TransactionCursor, mergePolicies: Boolean): TransactionTreeData =
        transactionProvider.getHotspots(transactionCursor, mergePolicies)

    @RequireProfiler
    override fun recordJps(vm: VM, recordJpsAction: RecordJpsAction) {
        prepareRecordArtifactAction(recordJpsAction, vm)
        vmManager.recordJps(vm, user, recordJpsAction)
    }

    @RequireProfiler
    override fun recordJfr(vm: VM, recordJfrAction: RecordJfrAction) {
        prepareRecordArtifactAction(recordJfrAction, vm)
        vmManager.recordJfr(vm, user, recordJfrAction)
    }

    private fun prepareRecordArtifactAction(recordArtifactAction: RecordArtifactAction, vm: VM) {
        checkVmGroupAccess(vm)
        recordArtifactAction.isCreateInboxItem = false
        recordArtifactAction.artifactName = vm.name
    }

    @RequireProfiler
    override fun deleteVM(vm: VM): Boolean {
        checkVmGroupAccess(vm)
        return vmManager.deleteVM(vm)
    }

    private fun checkVmGroupAccess(vm: VM?) {
        if (vm != null) {
            configManager.checkGroupModificationRights(vm.qualifiedIdentifier, user.accessLevel, user.groupNames)
        }
    }

    override fun getAgentKeystore(): File? = vmManager.agentKeystore

    companion object {
        private val SERVER_LOGGER: Logger = Loggers.SERVER

        private fun checkMBeanParameters(vm: VM?, name: String?, info: Any?): String? {
            if (vm == null || vm.isGroupNode) {
                return "Select a single VM"
            }
            if (name == null) {
                return "No MBean name provided"
            }
            if (info == null) {
                return "No attribute/operation provided"
            }
            return null
        }
    }
}
