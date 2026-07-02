package com.jvmguard.connector.api

import com.jvmguard.agent.data.MethodInfo
import com.jvmguard.agent.mbean.MBeanData
import com.jvmguard.agent.mbean.MBeanModificationData
import com.jvmguard.agent.mbean.MBeanOperationData
import com.jvmguard.annotation.ClassTransaction
import com.jvmguard.annotation.Inheritance
import com.jvmguard.annotation.Inheritance.Mode
import com.jvmguard.annotation.NoTransaction
import com.jvmguard.annotation.Part
import com.jvmguard.annotation.Part.Type
import com.jvmguard.common.helper.Direction
import com.jvmguard.common.helper.ListModification
import com.jvmguard.common.notification.ModificationType
import com.jvmguard.common.update.InstallationInfo
import com.jvmguard.common.update.UpdateResult
import com.jvmguard.data.agent.ArchiveFile
import com.jvmguard.data.agent.ArchiveFileType
import com.jvmguard.data.base.StoredConfig
import com.jvmguard.data.config.GlobalConfig
import com.jvmguard.data.config.GroupConfig
import com.jvmguard.data.config.SmtpConfig
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
import com.jvmguard.data.user.RequireAdmin
import com.jvmguard.data.user.User
import com.jvmguard.data.user.viewsettings.ViewSettings
import com.jvmguard.data.vmdata.*
import com.jvmguard.connector.api.log.LogFile
import com.jvmguard.connector.api.log.LogFileDescriptor
import com.jvmguard.connector.api.log.LogFileType
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

    val hiddenDevOpsTelemetryNodes: Collection<String>

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

    fun modifyGroupConfigs(listModification: ListModification<GroupConfig>)

    fun resetCaps()

    fun readConfig(fileContent: ByteArray): ExternalConfig?

    fun applyListModification(listModification: ListModification<out StoredConfig>)

    fun getVmDataHolders(vmFilter: VmFilter, sparkLineRange: SparkLineRange, telemetryTypes: Collection<TelemetryType>): Group<VmDataHolder>
    fun getGroupVmDataHolder(vmIdentifier: VmIdentifier?, sparkLineRange: SparkLineRange, telemetryTypes: Collection<TelemetryType>): VmDataHolder
    fun getVmDataHolder(vm: VM, sparkLineRange: SparkLineRange, telemetryTypes: Collection<TelemetryType>): VmDataHolder

    fun getTelemetryData(vm: VM?, mainId: String, interval: TelemetryInterval, endTime: Long): TelemetryData
    fun getCustomTelemetryData(vm: VM?, nodeIdentifier: CustomTelemetryNodeIdentifier, interval: TelemetryInterval, endTime: Long): TelemetryData

    fun setDevOpsTelemetryNodeVisibilities(nameToVisibility: Map<String, Boolean>)

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
