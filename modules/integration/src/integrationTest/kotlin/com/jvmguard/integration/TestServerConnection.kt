package com.jvmguard.integration

import com.jvmguard.agent.data.MethodInfo
import com.jvmguard.agent.mbean.MBeanData
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
import com.jvmguard.data.config.external.ExternalConfig
import com.jvmguard.data.config.external.ServerInitConfig
import com.jvmguard.data.config.sets.ActionSet
import com.jvmguard.data.config.sets.ThresholdSet
import com.jvmguard.data.config.sets.TransactionDefSet
import com.jvmguard.data.config.sets.TriggerSet
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
import com.jvmguard.connector.api.log.LogFile
import com.jvmguard.connector.api.log.LogFileDescriptor
import com.jvmguard.connector.api.log.LogFileType
import java.net.InetAddress
import java.util.*
import javax.management.MBeanAttributeInfo
import javax.management.MBeanOperationInfo

@Suppress("unused")
class TestServerConnection(private val delegate: ServerConnection) {

    val agentPath: String get() = delegate.agentPath
    val user: User get() = delegate.user
    val users: Collection<User> get() = delegate.users
    val loggedInUsers: Collection<User> get() = delegate.loggedInUsers
    val installationInfo: InstallationInfo get() = delegate.installationInfo
    val inboxItems: Collection<InboxItem> get() = delegate.inboxItems
    val serverInitConfig: ServerInitConfig get() = delegate.serverInitConfig
    val groupConfigs: Collection<GroupConfig> get() = delegate.groupConfigs
    val currentTime: Long get() = delegate.currentTime
    val transactionDefSets: Collection<TransactionDefSet> get() = delegate.transactionDefSets
    val thresholdSets: Collection<ThresholdSet> get() = delegate.thresholdSets
    val triggerSets: Collection<TriggerSet> get() = delegate.triggerSets
    val actionSets: Collection<ActionSet> get() = delegate.actionSets
    val idToTelemetryType: Map<String, TelemetryType> get() = delegate.idToTelemetryType
    val namedVms: Collection<VM> get() = delegate.namedVms
    val connectedVms: Collection<VM> get() = delegate.connectedVms
    val hiddenDevOpsTelemetryNodes: Collection<String> get() = delegate.hiddenDevOpsTelemetryNodes
    val caps: EnumSet<CapType> get() = delegate.caps
    val isUseSsl: Boolean get() = delegate.isUseSsl
    val customTelemetryNodes: Collection<CustomTelemetryNodeIdentifier>
        get() = delegate.customTelemetryInfo.customTelemetryNodeIdentifiers

    fun isLocalAddress(inetAddress: InetAddress) = delegate.isLocalAddress(inetAddress)

    fun deleteVM(vm: VM) = delegate.deleteVM(vm)

    fun getMBeanNames(vm: VM, createPlatformServer: Boolean) = delegate.getMBeanNames(vm, createPlatformServer)

    fun getMBeanData(vm: VM, name: String, fetchStructure: Boolean, fetchSimpleValues: Boolean): MBeanData =
        requireNotNull(delegate.getMBeanData(vm, name, fetchStructure, fetchSimpleValues))

    fun invokeMBeanOperation(vm: VM, name: String, operationInfo: MBeanOperationInfo, parameters: Array<Any?>) =
        delegate.invokeMBeanOperation(vm, name, operationInfo, parameters)

    fun setMBeanAttribute(vm: VM, name: String, attributeInfo: MBeanAttributeInfo?, value: Any?) =
        delegate.setMBeanAttribute(vm, name, requireNotNull(attributeInfo), value)

    fun modifyInboxItems(listModification: ListModification<InboxItem>) = delegate.modifyInboxItems(listModification)

    fun resetCaps() = delegate.resetCaps()

    fun modifySnapshotFiles(listModification: ListModification<SnapshotFile>) =
        delegate.modifySnapshotFiles(listModification)

    fun modifyUsers(listModification: ListModification<User>) = delegate.modifyUsers(listModification)

    fun getAndClearModificationTypes(): Set<ModificationType> = delegate.getAndClearModificationTypes()

    fun getVms(ids: LongArray): Collection<VM> = delegate.getVms(ids)

    fun getConnectedPooledVms(pool: VM): Collection<VM> = delegate.getConnectedPooledVms(pool)

    fun logout() = delegate.logout()

    fun checkForUpdates(): UpdateResult = requireNotNull(delegate.checkForUpdates())

    fun saveViewSettings(viewSettings: ViewSettings) = delegate.saveViewSettings(viewSettings)

    fun saveSelf(user: User) = delegate.saveSelf(user)

    fun getLogFileDescriptors(logFileType: LogFileType): List<LogFileDescriptor> =
        delegate.getLogFileDescriptors(logFileType)

    fun getLogFile(fileName: String): LogFile = delegate.getLogFile(fileName)

    fun getSnapshotFileSize(snapshotFile: SnapshotFile): Long = delegate.getSnapshotFileSize(snapshotFile)

    fun getGlobalConfig(obfuscated: Boolean): GlobalConfig = delegate.getGlobalConfig(obfuscated)

    fun applyInitConfig(serverInitConfig: ServerInitConfig) = delegate.applyInitConfig(serverInitConfig)

    fun setGlobalConfig(globalConfig: GlobalConfig) = delegate.setGlobalConfig(globalConfig)

    fun modifyGroupConfigs(listModification: ListModification<GroupConfig>) =
        delegate.modifyGroupConfigs(listModification)

    fun readConfig(fileContent: ByteArray): ExternalConfig = requireNotNull(delegate.readConfig(fileContent))

    fun applyListModification(listModification: ListModification<out StoredConfig>) =
        delegate.applyListModification(listModification)

    fun getVmDataHolders(
        vmFilter: VmFilter,
        sparkLineRange: SparkLineRange,
        telemetryTypes: Collection<TelemetryType>,
    ): Group<VmDataHolder> = delegate.getVmDataHolders(vmFilter, sparkLineRange, telemetryTypes)

    fun getGroupVmDataHolder(
        vmIdentifier: VmIdentifier?,
        sparkLineRange: SparkLineRange,
        telemetryTypes: Collection<TelemetryType>,
    ): VmDataHolder = delegate.getGroupVmDataHolder(vmIdentifier, sparkLineRange, telemetryTypes)

    fun getTelemetryData(vm: VM?, mainId: String, interval: TelemetryInterval, endTime: Long): TelemetryData =
        delegate.getTelemetryData(vm, mainId, interval, endTime)

    fun getPackageStats(vm: VM): Map<String, Int> = requireNotNull(delegate.getPackageStats(vm))

    fun getClassNames(vm: VM, allClasses: Boolean): Collection<String> = requireNotNull(delegate.getClassNames(vm, allClasses))

    fun getMethods(className: String, vm: VM): Collection<MethodInfo> = requireNotNull(delegate.getMethods(className, vm))

    fun getSnapshotFiles(snapshotFileType: SnapshotFileType?, vm: VM?): Collection<SnapshotFile> =
        delegate.getSnapshotFiles(snapshotFileType, vm)

    fun getSnapshotFile(id: Long): SnapshotFile = requireNotNull(delegate.getSnapshotFile(id))

    fun heapDump(vm: VM) = delegate.heapDump(vm)

    fun threadDump(vm: VM) = delegate.threadDump(vm)

    fun recordJps(vm: VM, recordJpsAction: RecordJpsAction) = delegate.recordJps(vm, recordJpsAction)

    fun recordJfr(vm: VM, recordJfrAction: RecordJfrAction) = delegate.recordJfr(vm, recordJfrAction)

    fun runGC(vm: VM) = delegate.runGC(vm)

    fun getTransactionTreeCursor(
        vm: VM?,
        interval: TransactionTreeInterval,
        transactionDataType: TransactionDataType,
        time: Long,
    ): TransactionCursor =
        delegate.getTransactionTreeCursor(vm, interval, transactionDataType, time, TimeRequirement.NEAREST_START_TIME)

    fun getCurrentTransactionTreeCursor(
        vm: VM?,
        interval: TransactionTreeInterval,
        transactionDataType: TransactionDataType,
    ): TransactionCursor = delegate.getCurrentTransactionTreeCursor(vm, interval, transactionDataType)

    fun changeTransactionCursor(
        transactionCursor: TransactionCursor,
        vm: VM?,
        interval: TransactionTreeInterval,
    ): TransactionCursor = delegate.changeTransactionCursor(transactionCursor, vm, interval)

    fun getCallTree(transactionCursor: TransactionCursor, mergePolicies: Boolean): TransactionTreeData =
        delegate.getCallTree(transactionCursor, mergePolicies)

    fun getHotspots(transactionCursor: TransactionCursor, mergePolicies: Boolean): TransactionTreeData =
        delegate.getHotspots(transactionCursor, mergePolicies)

    fun moveTransactionTreeCursor(cursor: TransactionCursor, direction: Direction): TransactionCursor =
        delegate.moveTransactionTreeCursor(cursor, direction)

    fun getTransactionTreeTimeLine(
        vm: VM?,
        startTime: Long,
        endTime: Long,
        selectedItem: TransactionTreeIdentifier,
        valueType: TransactionTreeValueType,
        transactionTreeInterval: TransactionTreeInterval,
        mergePolicies: Boolean,
    ): TelemetryData = delegate.getTransactionTreeTimeLine(
        vm, startTime, endTime, selectedItem, valueType, transactionTreeInterval, mergePolicies)

    fun getHotspotsTimeLine(
        vm: VM?,
        startTime: Long,
        endTime: Long,
        selectedItem: TransactionInfo,
        valueType: TransactionTreeValueType,
        transactionTreeInterval: TransactionTreeInterval,
    ): TelemetryData =
        delegate.getHotspotsTimeLine(vm, startTime, endTime, selectedItem, valueType, transactionTreeInterval)

    fun getAgentArchiveFile(archiveFileType: ArchiveFileType): ArchiveFile =
        delegate.getAgentArchiveFile(archiveFileType)

    fun setDevOpsTelemetryNodeVisibility(nodeName: String, visible: Boolean) =
        delegate.setDevOpsTelemetryNodeVisibilities(mapOf(nodeName to visible))

    fun getCustomTelemetryData(
        vm: VM?,
        nodeIdentifier: CustomTelemetryNodeIdentifier,
        interval: TelemetryInterval,
        endTime: Long,
    ): TelemetryData = delegate.getCustomTelemetryData(vm, nodeIdentifier, interval, endTime)
}
