package dev.jvmguard.connector.api

import dev.jvmguard.agent.data.MethodInfo
import dev.jvmguard.agent.mbean.MBeanData
import dev.jvmguard.agent.mbean.MBeanModificationData
import dev.jvmguard.agent.mbean.MBeanOperationData
import dev.jvmguard.annotation.ClassTransaction
import dev.jvmguard.annotation.Inheritance
import dev.jvmguard.annotation.Inheritance.Mode
import dev.jvmguard.annotation.NoTransaction
import dev.jvmguard.annotation.Part
import dev.jvmguard.annotation.Part.Type
import dev.jvmguard.common.helper.Direction
import dev.jvmguard.common.helper.ListModification
import dev.jvmguard.common.notification.ModificationType
import dev.jvmguard.common.update.InstallationInfo
import dev.jvmguard.common.update.UpdateResult
import dev.jvmguard.data.agent.ArchiveFile
import dev.jvmguard.data.agent.ArchiveFileType
import dev.jvmguard.data.base.StoredConfig
import dev.jvmguard.data.config.GlobalConfig
import dev.jvmguard.data.config.GroupConfig
import dev.jvmguard.data.config.SmtpConfig
import dev.jvmguard.data.config.external.ExternalConfig
import dev.jvmguard.data.config.external.ServerInitConfig
import dev.jvmguard.data.config.sets.*
import dev.jvmguard.data.config.triggers.actions.RecordJfrAction
import dev.jvmguard.data.config.triggers.actions.RecordJpsAction
import dev.jvmguard.data.dashboard.Group
import dev.jvmguard.data.file.SnapshotFile
import dev.jvmguard.data.file.SnapshotFileType
import dev.jvmguard.data.transactions.*
import dev.jvmguard.data.user.InboxItem
import dev.jvmguard.data.user.RequireAdmin
import dev.jvmguard.data.user.User
import dev.jvmguard.data.user.viewsettings.ViewSettings
import dev.jvmguard.data.vmdata.*
import dev.jvmguard.connector.api.log.LogFile
import dev.jvmguard.connector.api.log.LogFileDescriptor
import dev.jvmguard.connector.api.log.LogFileType
import java.io.File
import java.net.InetAddress
import java.util.*
import javax.management.MBeanAttributeInfo
import javax.management.MBeanOperationInfo

@ClassTransaction(
    naming = [Part(text = "ServerConnection."), Part(Type.METHOD)],
    group = "serverConnection",
    inheritance = Inheritance(value = Mode.WITH_SUPERCLASS_NAME, implementingOnly = true),
)
interface ServerConnection {
    @get:NoTransaction
    val user: User

    val installationInfo: InstallationInfo
    val inboxItems: Collection<InboxItem>
    val users: Collection<User>
    val loggedInUsers: Collection<User>

    @get:RequireAdmin
    val serverInitConfig: ServerInitConfig

    val groupConfigs: Collection<GroupConfig>
    val caps: EnumSet<CapType>

    @get:NoTransaction
    val currentTime: Long

    val transactionDefSets: Collection<TransactionDefSet>
    val thresholdSets: Collection<ThresholdSet>
    val triggerSets: Collection<TriggerSet>
    val actionSets: Collection<ActionSet>
    val telemetrySets: Collection<TelemetrySet>

    @get:NoTransaction
    val idToTelemetryType: Map<String, TelemetryType>

    val namedVms: Collection<VM>
    val connectedVms: Collection<VM>

    @get:NoTransaction
    val customTelemetryInfo: CustomTelemetryInfo

    val hiddenDeclaredTelemetryNodes: Collection<String>

    val isUseSsl: Boolean
    val dataDirectory: File
    val agentPath: String

    fun logout()

    fun encryptTotpSecret(secretAsHex: String): String
    fun checkForUpdates(): UpdateResult?
    fun saveViewSettings(viewSettings: ViewSettings)
    fun saveSelf(user: User)
    fun getLogFileDescriptors(logFileType: LogFileType): List<LogFileDescriptor>
    fun getLogFile(fileName: String): LogFile
    fun getGlobalConfig(obfuscated: Boolean): GlobalConfig

    fun applyInitConfig(serverInitConfig: ServerInitConfig)

    fun setGlobalConfig(globalConfig: GlobalConfig)

    fun sendTestMail(recipient: String, subject: String, content: String, smtpConfig: SmtpConfig)

    fun testSsoDiscovery(issuerUri: String): String

    fun modifyGroupConfigs(listModification: ListModification<GroupConfig>)

    fun resetCaps()

    fun readConfig(fileContent: ByteArray): ExternalConfig?

    fun applyListModification(listModification: ListModification<out StoredConfig>)

    fun getVmDataHolders(vmFilter: VmFilter, sparkLineRange: SparkLineRange, telemetryTypes: Collection<TelemetryType>): Group<VmDataHolder>
    fun getGroupVmDataHolder(vmIdentifier: VmIdentifier?, sparkLineRange: SparkLineRange, telemetryTypes: Collection<TelemetryType>): VmDataHolder
    fun getVmDataHolder(vm: VM, sparkLineRange: SparkLineRange, telemetryTypes: Collection<TelemetryType>): VmDataHolder

    fun getTelemetryData(vm: VM?, mainId: String, interval: TelemetryInterval, endTime: Long): TelemetryData
    fun getCustomTelemetryData(vm: VM?, nodeIdentifier: CustomTelemetryNodeIdentifier, interval: TelemetryInterval, endTime: Long): TelemetryData

    fun setDeclaredTelemetryNodeVisibilities(nameToVisibility: Map<String, Boolean>)

    fun getPackageStats(vm: VM): Map<String, Int>?
    fun getClassNames(vm: VM, allClasses: Boolean): Collection<String>?
    fun getMethods(className: String, vm: VM): Collection<MethodInfo>?

    fun getSnapshotFiles(snapshotFileType: SnapshotFileType?, vm: VM?): Collection<SnapshotFile>
    fun getSnapshotFile(id: Long): SnapshotFile?

    fun getSnapshotFileSize(snapshotFile: SnapshotFile): Long

    fun heapDump(vm: VM)
    fun threadDump(vm: VM)
    fun runGC(vm: VM)
    fun recordJps(vm: VM, recordJpsAction: RecordJpsAction)
    fun recordJfr(vm: VM, recordJfrAction: RecordJfrAction)

    fun getTransactionTreeCursor(
        vm: VM?,
        interval: TransactionTreeInterval,
        transactionDataType: TransactionDataType,
        time: Long,
        timeRequirement: TimeRequirement
    ): TransactionCursor

    fun getCurrentTransactionTreeCursor(vm: VM?, interval: TransactionTreeInterval, transactionDataType: TransactionDataType): TransactionCursor
    fun moveTransactionTreeCursor(cursor: TransactionCursor, direction: Direction): TransactionCursor
    fun changeTransactionCursor(transactionCursor: TransactionCursor, vm: VM?, interval: TransactionTreeInterval): TransactionCursor

    fun getCallTree(transactionCursor: TransactionCursor, mergePolicies: Boolean): TransactionTreeData
    fun getHotspots(transactionCursor: TransactionCursor, mergePolicies: Boolean): TransactionTreeData

    fun getTransactionTreeTimeLine(
        vm: VM?,
        startTime: Long,
        endTime: Long,
        selectedItem: TransactionTreeIdentifier,
        valueType: TransactionTreeValueType,
        transactionTreeInterval: TransactionTreeInterval,
        mergePolicies: Boolean
    ): TelemetryData

    fun getHotspotsTimeLine(
        vm: VM?,
        startTime: Long,
        endTime: Long,
        selectedItem: TransactionInfo,
        valueType: TransactionTreeValueType,
        transactionTreeInterval: TransactionTreeInterval
    ): TelemetryData

    fun getMBeanNames(vm: VM, createPlatformServer: Boolean): Collection<String>
    fun getMBeanData(vm: VM, name: String, fetchStructure: Boolean, fetchValues: Boolean): MBeanData?
    fun invokeMBeanOperation(vm: VM, name: String, operationInfo: MBeanOperationInfo, parameters: Array<Any?>): MBeanOperationData
    fun setMBeanAttribute(vm: VM, name: String, attributeInfo: MBeanAttributeInfo, value: Any?): MBeanModificationData

    fun getAgentArchiveFile(archiveFileType: ArchiveFileType): ArchiveFile

    fun isLocalAddress(inetAddress: InetAddress): Boolean

    fun deleteVM(vm: VM): Boolean

    fun modifyInboxItems(listModification: ListModification<InboxItem>)
    fun modifySnapshotFiles(listModification: ListModification<SnapshotFile>)

    fun modifyUsers(listModification: ListModification<User>)

    @NoTransaction
    fun getAndClearModificationTypes(): Set<ModificationType>

    fun getVms(ids: LongArray): Collection<VM>

    fun getConnectedPooledVms(pool: VM): Collection<VM>
}
