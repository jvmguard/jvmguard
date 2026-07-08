package com.jvmguard.collector.main

import com.jvmguard.agent.JvmGuardAgent
import com.jvmguard.agent.artifact.ArtifactKind
import com.jvmguard.agent.comm.CommandType
import com.jvmguard.agent.config.VmType
import com.jvmguard.agent.data.*
import com.jvmguard.agent.mbean.*
import com.jvmguard.agent.parameter.*
import com.jvmguard.agent.util.MaxThreadPoolExecutor
import com.jvmguard.agent.util.JvmGuardThreadFactory
import com.jvmguard.annotation.MethodTransaction
import com.jvmguard.annotation.Telemetry
import com.jvmguard.collector.api.VmManager
import com.jvmguard.collector.connection.AgentConnectionImpl
import com.jvmguard.collector.connection.AgentConnectionImpl.Handler
import com.jvmguard.collector.connection.ConnectionServer
import com.jvmguard.collector.connection.SslManager
import com.jvmguard.collector.telemetry.TelemetryStorage
import com.jvmguard.collector.transactions.TransactionManager
import com.jvmguard.collector.util.CurrentConnectionEntry
import com.jvmguard.collector.vmdata.VmGroupData
import com.jvmguard.collector.vmdata.startup.StartupBuilder
import com.jvmguard.common.DatabaseWriter
import com.jvmguard.common.Loggers
import com.jvmguard.common.JvmGuardProperties
import com.jvmguard.common.config.ConfigChangeListener
import com.jvmguard.common.config.ConfigManager
import com.jvmguard.common.notification.InboxManager
import com.jvmguard.data.agent.UpdateArchiveFile
import com.jvmguard.data.config.GlobalConfig
import com.jvmguard.data.config.GroupHierarchyWrapper
import com.jvmguard.data.config.thresholds.Threshold
import com.jvmguard.data.config.triggers.actions.RecordJfrAction
import com.jvmguard.data.config.triggers.actions.RecordJpsAction
import com.jvmguard.data.dashboard.Group
import com.jvmguard.data.file.SnapshotFileType
import com.jvmguard.data.user.User
import com.jvmguard.data.vmdata.*
import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.SmartLifecycle
import org.springframework.stereotype.Component
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.util.*
import java.util.concurrent.TimeUnit
import javax.management.MBeanAttributeInfo
import javax.management.MBeanInfo
import javax.management.MBeanOperationInfo
import javax.sql.DataSource

@Component
class VmManagerImpl(
    private val connectionRegistry: ConnectionRegistry,
    private val collectorContext: CollectorContext,
    private val vmRegistry: VmRegistry,
    private val collector: Collector,
    private val configManager: ConfigManager,
    private val sslManager: SslManager,
    private val transactionManager: TransactionManager,
    private val telemetryStorage: TelemetryStorage,
    private val startupBuilder: StartupBuilder,
    private val snapshotFileStorage: SnapshotFileStorage,
    private val inboxManager: InboxManager,
    private val vmStorage: VmStorage,
    private val databaseWriter: DatabaseWriter,
    @param:Qualifier("vmPort") private val vmPort: Int,
    @param:Qualifier("vmUseSsl") private val vmUseSsl: Boolean,
    @param:Qualifier("agentDirectory") private val agentDirectory: File,
    private val properties: JvmGuardProperties,
    private val dataSource: DataSource,
) : VmManager, ConfigChangeListener, SmartLifecycle {

    private val triggerLock = Any()

    private val laterExecutor = MaxThreadPoolExecutor(20, 10, TimeUnit.SECONDS, JvmGuardThreadFactory("later", true, Thread.NORM_PRIORITY))

    private lateinit var connectionServer: ConnectionServer

    @Volatile
    private var running = false

    @PostConstruct
    fun postConstruct() {
        readBuildVersion()
        configManager.addConfigChangeListener(this)

        vmRegistry.rootVmGroupData = VmGroupData(null, VmIdentifier("", VmType.GROUP), collectorContext)
        connectionServer = ConnectionServer(
            InetSocketAddress(vmPort), vmUseSsl, this, sslManager,
            properties.vmEnabledProtocols, properties.vmEnabledCipherSuites, properties.commPoolSize,
        )

        singleInstance = this
    }

    override fun start() {
        val usedTime = transactionManager.initAdditionalIntervals()

        SERVER_LOGGER.info("Initializing data")
        startupBuilder.build()
        SERVER_LOGGER.info("Initializing data done")
        initTriggersAndGroupThresholds()

        if (!properties.isNoCollection) {
            collector.start(usedTime)
        }
        connectionServer.start()
        running = true
    }

    private fun readBuildVersion() {
        JvmGuardAgent.initBuildVersion(File(agentDirectory, "lib/agent.jar"))
        SERVER_LOGGER.info("Build version: {}", JvmGuardAgent.getBuildVersion())
    }

    override fun globalConfigChanged(oldConfig: GlobalConfig?, newConfig: GlobalConfig) {
        transactionManager.updateGlobalConfig(newConfig)
    }

    override fun groupConfigsChanged() {
        initTriggersAndGroupThresholds()
        for (entry in getConnections()) {
            val groupHierarchyWrapper = configManager.getGroupHierarchyWrapper(entry.key)
            setVmConfig(entry.value, groupHierarchyWrapper, false)
        }
    }

    private fun initTriggersAndGroupThresholds() {
        synchronized(triggerLock) {
            for (groupConfig in configManager.getGroupConfigs()) {
                val triggerSettings = groupConfig.triggerSettings
                val thresholdSettings = groupConfig.thresholdSettings
                if (triggerSettings.activeTriggerCount > 0 || thresholdSettings.activeGroupThresholdCount > 0) {
                    val groupData = vmRegistry.rootVmGroupData.getGroupData(groupConfig.groupIdentifier, collectorContext)!!

                    groupData.triggerHandler.setTriggers(triggerSettings.triggers)

                    val groupThresholds = ArrayList<Threshold>()
                    for (threshold in thresholdSettings.thresholds) {
                        if (threshold.isEnabled && threshold.target == Threshold.Target.GROUP) {
                            groupThresholds.add(threshold)
                        }
                    }
                    groupData.setThresholds(groupThresholds)
                }
            }
        }
    }

    private fun getVm(name: String, groupId: String, instanceId: Long, vmType: VmType, hostName: String, port: Int): VM {
        val vm = vmStorage.getVm(name, groupId, instanceId, vmType, hostName, port)!!
        configManager.groupConnected(vm.parentIdentifier)
        return vm
    }

    override fun terminate(vm: VM, closeConnection: Boolean) {
        val connectionEntry = synchronized(connectionRegistry) { connectionRegistry.get(vm) }
        if (connectionEntry != null) {
            connectionEntry.agentConnection.executeCommand(CommandType.KILL)
            if (closeConnection) {
                connectionEntry.agentConnection.close()
            }
        }
    }

    override fun stop() {
        connectionServer.shutdownServerSocket()
        collector.shutdown()

        SERVER_LOGGER.info("closing connections (1)")
        connectionServer.shutdownDeferred()
        SERVER_LOGGER.info("closing connections (2)")
        val connections = ArrayList<AgentConnectionImpl>()
        synchronized(connectionRegistry) {
            for (currentConnectionEntry in connectionRegistry.values()) {
                connections.add(currentConnectionEntry.agentConnection)
            }
        }
        for (connection in connections) {
            connection.close()
        }
        SERVER_LOGGER.info("closing connections done")
        running = false
    }

    override fun isRunning(): Boolean {
        return running
    }

    override fun runGC(vm: VM) {
        executeLater(vm, CommandType.RUN_GC, null, null)
    }

    private fun setVmConfig(connectionEntry: CurrentConnectionEntry?, groupHierarchyWrapper: GroupHierarchyWrapper?, immediately: Boolean) {
        if (groupHierarchyWrapper != null && connectionEntry != null) {
            val agentConnection = connectionEntry.agentConnection
            if (agentConnection.isAlive) {
                connectionEntry.vmData.setThresholds(groupHierarchyWrapper.vmThresholds)

                val parameter = ConfigurationParameter()
                parameter.setRecordingOptions(groupHierarchyWrapper.recordingOptions)
                parameter.transactionSettings = groupHierarchyWrapper.transactionSettings
                parameter.telemetrySettings = groupHierarchyWrapper.telemetrySettings
                val runnable = Runnable {
                    try {
                        agentConnection.executeCommand(CommandType.SET_CONFIGURATION, parameter)
                    } catch (e: Throwable) {
                        CONNECTION_LOGGER.error("setting config on {}", connectionEntry.vm.verbose, e)
                    }
                }
                if (immediately) {
                    runnable.run()
                } else {
                    laterExecutor.submit(runnable)
                }
            }
        }
    }

    override fun recordJps(vm: VM, user: User, recordJpsAction: RecordJpsAction) {
        collectorContext.recordJProfilerSnapshot(vm, user, recordJpsAction)
    }

    override fun getMBeanNames(vm: VM, createPlatformServer: Boolean): Collection<String> {
        val usedCreatePlatformServer = if (properties.isNoPlatformMBean) false else createPlatformServer
        val currentConnectionEntry = getConnectionEntry(vm)
        if (currentConnectionEntry != null) {
            val result = executeAndWait(vm, CommandType.MBEAN_LIST, MBeanListParameter(usedCreatePlatformServer), 1, TimeUnit.MINUTES) as MBeanListResult?
            return currentConnectionEntry.getMBeanNames(result)
        }
        return emptyList()
    }

    override fun getMBeanData(vm: VM, name: String, fetchStructure: Boolean, fetchValues: Boolean): MBeanData {
        val result = executeAndWait(vm, CommandType.MBEAN_DATA, MBeanDataParameter(name, fetchStructure, fetchValues), 1, TimeUnit.MINUTES) as MBeanDataResult?
        return result ?: object : MBeanData {
            override fun getBeanInfo(): MBeanInfo = MBeanInfo("", "", null, null, null, null)
            override fun getValues(): MutableList<Any> = mutableListOf()
        }
    }

    override fun invokeMBeanOperation(vm: VM, name: String, operationInfo: MBeanOperationInfo, parameters: Array<Any?>): MBeanOperationData {
        val result = executeAndWait(
            vm,
            CommandType.MBEAN_OPERATION,
            MBeanOperationParameter(name, operationInfo, parameters),
            30,
            TimeUnit.MINUTES
        ) as MBeanOperationResult?
        return result ?: object : MBeanOperationData {
            override fun getErrorMessage(): String {
                return "Connection to ${vm.verbose} not successful."
            }

            override fun getStackTrace(): String? {
                return null
            }

            override fun getReturnValue(): Any? {
                return null
            }
        }
    }

    override fun setMBeanAttribute(vm: VM, name: String, attributeInfo: MBeanAttributeInfo, value: Any?): MBeanModificationData {
        val result = executeAndWait(
            vm,
            CommandType.MBEAN_SET_ATTRIBUTE,
            MBeanSetAttributeParameter(name, attributeInfo, value),
            30,
            TimeUnit.MINUTES
        ) as MBeanSetAttributeResult?
        return result ?: object : MBeanModificationData {
            override fun getErrorMessage(): String {
                return "Connection to ${vm.verbose} not successful."
            }

            override fun getStackTrace(): String? {
                return null
            }
        }
    }

    override fun recordJfr(vm: VM, user: User, recordJfrAction: RecordJfrAction) {
        collectorContext.executeLater(vm, setOf(collectorContext.getRecordJfrCommand(vm, user, recordJfrAction)))
    }

    override fun heapDump(vm: VM, user: User) {
        collectorContext.executeLater(vm, setOf(collectorContext.getHeapDumpCommand(vm, user, false, vm.name)))
    }

    override fun threadDump(vm: VM, user: User) {
        collectorContext.executeLater(vm, setOf(collectorContext.getThreadDumpCommand(vm, user, false, vm.name)))
    }

    fun executeLater(vm: VM, commandType: CommandType, parameter: BaseParameter?, handler: Handler<*>?): Boolean {
        val agentConnection = connectionRegistry.getLiveConnection(vm)
        if (agentConnection != null) {
            agentConnection.executeLater(commandType, parameter, handler)
            return true
        }
        return false
    }

    fun executeAndWait(vm: VM, commandType: CommandType, parameter: BaseParameter?, timeout: Int, timeUnit: TimeUnit): BaseResult? {
        val agentConnection = connectionRegistry.getLiveConnection(vm)
        if (agentConnection != null) {
            try {
                return agentConnection.executeAndWait(commandType, parameter, timeout, timeUnit)
            } catch (e: InterruptedException) {
                CONNECTION_LOGGER.error("waiting for {} on {}", commandType, vm.verbose, e)
            }
        }
        return null
    }

    fun getConnectionEntry(vm: VM): CurrentConnectionEntry? {
        return synchronized(connectionRegistry) { connectionRegistry.get(vm) }
    }

    override fun getConnection(vm: VM): AgentConnectionImpl {
        return connectionRegistry.getLiveConnection(vm)!!
    }

    fun getConnections(): Array<Map.Entry<VM, CurrentConnectionEntry>> {
        return connectionRegistry.getConnections()
    }

    override fun getPackageStats(vm: VM): Map<String, Int> {
        val packageStats = getResult(vm, CommandType.PACKAGE_STATS, null) as PackageStats?
        return packageStats?.packageToValue ?: emptyMap()
    }

    override fun getClassNames(vm: VM, allClasses: Boolean): Collection<String> {
        val classesInfo = getResult(vm, CommandType.CLASSES_INFO, ClassesInfoParameter(allClasses)) as ClassesInfo?
        return classesInfo?.classNames ?: emptyList()
    }

    private fun getResult(vm: VM?, commandType: CommandType, parameter: BaseParameter?): BaseResult? {
        if (vm == null) {
            return null
        }
        try {
            val connection = connectionRegistry.getLiveConnection(vm)
            if (connection != null) {
                return connection.executeCommand(commandType, parameter)
            } else {
                CONNECTION_LOGGER.error("unconnected {} for {}", commandType.toString(), vm.verbose)
            }
        } catch (e: IOException) {
            CONNECTION_LOGGER.error("{} for {}", commandType.toString(), vm.verbose, e)
        }
        return null
    }

    override fun getMethods(className: String, vm: VM): Collection<MethodInfo> {
        val methodInfoResult = getResult(vm, CommandType.METHOD_INFO, MethodInfoParameter(className)) as MethodInfoResult?
        return methodInfoResult?.methodInfos ?: emptyList()
    }

    override fun getVmDataHolders(vmFilter: VmFilter, sparkLineRange: SparkLineRange, telemetryTypes: Collection<TelemetryType>): Group<VmDataHolder> {
        val root = Group<VmDataHolder>()
        vmRegistry.rootVmGroupData.addAllVmDataHolders(
            root,
            vmFilter,
            sparkLineRange,
            telemetryTypes,
            System.nanoTime(),
            System.currentTimeMillis(),
            configManager.getGlobalConfig(false).frequencyUnit
        )
        return root
    }

    override fun getGroupVmDataHolder(
        vmFilter: VmFilter,
        vmIdentifier: VmIdentifier?,
        sparkLineRange: SparkLineRange,
        telemetryTypes: Collection<TelemetryType>
    ): VmDataHolder {
        // this method would not work for single VMs because pooled vms need a VM object instead of VmIdentifier
        require(vmIdentifier == null || vmIdentifier.type.isGroupNode) { "expected a group identifier: $vmIdentifier" }
        val groupData = vmRegistry.rootVmGroupData.getGroupData(vmIdentifier, collectorContext)!!
        return groupData.getVmDataHolder(
            vmFilter,
            sparkLineRange,
            telemetryTypes,
            System.nanoTime(),
            System.currentTimeMillis(),
            configManager.getGlobalConfig(false).frequencyUnit
        )
    }

    override fun getVmDataHolder(vmFilter: VmFilter, vm: VM, sparkLineRange: SparkLineRange, telemetryTypes: Collection<TelemetryType>): VmDataHolder {
        val vmData = vmRegistry.rootVmGroupData.getVmData(vm)
        return vmData!!.getVmDataHolder(
            vmFilter,
            sparkLineRange,
            telemetryTypes,
            System.nanoTime(),
            System.currentTimeMillis(),
            configManager.getGlobalConfig(false).frequencyUnit
        )!!
    }

    override val currentConnections: List<Connection>
        get() {
            val result = ArrayList<Connection>()
            for (entry in getConnections()) {
                result.add(entry.value.connection)
            }
            return result
        }

    @MethodTransaction(group = "addConnection")
    fun addConnection(agentConnection: AgentConnectionImpl) {
        val outdatedAgent = checkAgentUpdate(agentConnection)

        agentConnection.executeCommand(CommandType.RESET, ResetParameter(collector.lastRecordingTime))

        val socketAddress = agentConnection.remoteAddress
        val hostName = socketAddress.address.hostAddress

        var vm: VM
        val connectionEntry: CurrentConnectionEntry
        synchronized(connectionRegistry) {
            val connectionInfo = agentConnection.connectionInfo!!
            var existingVm = connectionRegistry.getInstanceVm(connectionInfo.instanceId)
            if (existingVm == null) {
                var groupName = connectionInfo.vmGroup

                val poolName = connectionInfo.vmPool
                if (poolName.isNotEmpty()) {
                    if (groupName.isNotEmpty()) {
                        groupName += "/"
                    }
                    groupName += poolName

                    existingVm = getVm("", groupName, connectionInfo.instanceId, VmType.POOLED, hostName, socketAddress.port)
                } else {
                    var vmName = connectionInfo.vmName
                    if (vmName.trim().isEmpty()) {
                        vmName = "JVM"
                    }
                    existingVm = getVm(vmName, groupName, connectionInfo.instanceId, VmType.NAMED, hostName, socketAddress.port)

                    val replacedVm = existingVm
                    val previousConnectionEntry = connectionRegistry.get(existingVm)
                    if (previousConnectionEntry != null) {
                        val errorMessage = "New connection with this VM name from $hostName:${socketAddress.port}\n" +
                                "If you need multiple VMs with the same name, use the pool instead of the name parameter.\n" +
                                "This VM will be disconnected now."
                        previousConnectionEntry.agentConnection.executeLater(
                            CommandType.REJECT,
                            RejectParameter(errorMessage),
                            object : Handler<RejectResult>() {
                                override fun handle(result: RejectResult) {
                                    val rejectMessage =
                                        "${replacedVm.verbose} on ${result.hostName}:${result.port} was replaced from $hostName:${socketAddress.port}\n" +
                                                "If you need multiple VMs with the same name, use the pool instead of the name parameter."
                                    CONNECTION_LOGGER.error(rejectMessage)
                                    collectorContext.addInboxItems(
                                        replacedVm.parentIdentifier,
                                        "${replacedVm.verbose} replaced",
                                        rejectMessage,
                                        null,
                                        replacedVm
                                    )
                                }
                            })
                        connectionRegistry.removeInstanceVm(previousConnectionEntry.agentConnection.connectionInfo!!.instanceId)
                    }
                }
            }
            vm = existingVm

            val connection = Connection(vm)

            val groupNames = getGroupNames(vm.groupName)

            val vmData = vmRegistry.rootVmGroupData.connectVm(groupNames, connection, outdatedAgent, System.nanoTime(), hostName, socketAddress.port)
            CONNECTION_LOGGER.info("connection from {} on {}:{}", vm.verbose, vmData.hostName, vmData.port)

            connectionEntry = CurrentConnectionEntry(groupNames, vm, vmData, agentConnection, connection)
            addCloseListener(agentConnection, vm)
            connectionRegistry.put(vm, connectionEntry)
            connectionRegistry.putInstanceVm(connectionInfo.instanceId, vm)
        }
        val groupHierarchyWrapper = configManager.getGroupHierarchyWrapper(vm)
        setVmConfig(connectionEntry, groupHierarchyWrapper, true)

        addPushHandler(agentConnection, vm)
    }

    override fun deleteVM(vm: VM): Boolean {
        synchronized(connectionRegistry) {
            val vms = ArrayList<VM>()

            if (vm.isGroupNode) {
                val identifier = vm.qualifiedIdentifier

                if (identifier.isRoot) {
                    return false
                }
                for (connectedVM in connectionRegistry.keySet()) {
                    if (connectedVM.isIncluded(identifier)) {
                        return false
                    }
                }
                vms.addAll(vmStorage.getVmTree(vm, VmType.POOLED))
            } else {
                if (connectionRegistry.containsKey(vm)) {
                    return false
                }
                vms.add(vm)
            }
            vmStorage.delete(vms)
            databaseWriter.executeInWriter {
                try {
                    dataSource.connection.use { connection ->
                        telemetryStorage.deleteVMs(connection, vms)
                        transactionManager.deleteVMs(connection, vms)
                        snapshotFileStorage.deleteVMs(connection, vms)
                        inboxManager.deleteVMs(connection, vms)
                    }
                } catch (e: Exception) {
                    SERVER_LOGGER.error("error cleaning up deleted vm data", e)
                }
            }
            return true
        }
    }

    private fun checkAgentUpdate(agentConnection: AgentConnectionImpl): Boolean {
        val socketAddress = agentConnection.remoteAddress

        var outdatedAgent = false
        if (agentConnection.connectionInfo!!.buildVersion < JvmGuardAgent.getBuildVersion()) {
            CONNECTION_LOGGER.info(
                "{}:{} has outdated build version {}",
                socketAddress.address.hostAddress,
                socketAddress.port,
                agentConnection.connectionInfo!!.buildVersion
            )
            val agentKey = JvmGuardAgent.getBuildVersion().toString()
            val checkResult = agentConnection.executeCommand(
                CommandType.CHECK_ARTIFACT,
                CheckArtifactParameter(ArtifactKind.AGENT, agentKey)
            ) as CheckArtifactResult
            if (!checkResult.isAvailable) {
                val vmAddressVerbose = "${socketAddress.address.hostAddress}:${socketAddress.port}"
                CONNECTION_LOGGER.info("transferring new agent to {}", vmAddressVerbose)
                var file: File? = null
                try {
                    val updateArchiveFile = UpdateArchiveFile.create(agentDirectory)
                    file = updateArchiveFile.file
                } catch (t: Throwable) {
                    CONNECTION_LOGGER.warn("exception while preparing agent archive", t)
                }
                if (file != null && file.isFile && file.length() > 0) {
                    val pushResult = agentConnection.executeCommand(
                        CommandType.PUSH_ARTIFACT,
                        PushArtifactParameter(ArtifactKind.AGENT, agentKey, file)
                    ) as PushArtifactResult
                    if (pushResult.isSuccess) {
                        outdatedAgent = true
                    } else {
                        CONNECTION_LOGGER.warn("could not update agent on {}: {}", vmAddressVerbose, pushResult.errorMessage)
                    }
                }
            } else {
                outdatedAgent = true
            }
        }
        return outdatedAgent
    }

    private fun addCloseListener(agentConnection: AgentConnectionImpl, vm: VM) {
        agentConnection.setCloseListener {
            synchronized(connectionRegistry) {
                val connectionEntry = connectionRegistry.get(vm)
                if (connectionEntry != null && connectionEntry.agentConnection === agentConnection) {
                    val endTime = System.currentTimeMillis()

                    connectionRegistry.remove(vm)
                    connectionRegistry.removeInstanceVm(agentConnection.connectionInfo!!.instanceId)
                    vmRegistry.rootVmGroupData.disconnectVm(ArrayDeque(connectionEntry.groupNames.toList()), vm, System.nanoTime())
                    connectionEntry.connection.endTime = endTime
                }
            }
        }
    }

    private fun addPushHandler(agentConnection: AgentConnectionImpl, vm: VM) {
        agentConnection.setPushHandler { commandType, _, result, properties ->
            storePushedSnapshot(commandType, result, properties, vm)
        }
    }

    private fun storePushedSnapshot(commandType: CommandType, result: BaseResult, properties: Properties, vm: VM) {
        val snapshotFileType = getSnapshotFileType(commandType)

        if (snapshotFileType != null) {
            val rawName = properties.getProperty(DeferredDataResult.PROPERTY_NAME, "")
            val inboxAll = properties.getProperty(DeferredDataResult.PROPERTY_INBOX_ALL)?.toBoolean() ?: false
            val name = if (rawName.trim().isNotEmpty()) rawName else vm.name

            var fileMover: FileMover? = null
            if (result is ThreadDumpResult) {
                fileMover = result
            } else if (result is SnapshotTransferResult) {
                if (result.errorMessage != null) {
                    SERVER_LOGGER.error("could not store snapshot file {} type {} for {}: {}", name, snapshotFileType, vm, result.errorMessage)
                } else {
                    fileMover = result
                }
            }
            if (fileMover != null) {
                val snapshot = snapshotFileStorage.createSnapshotFile(vm, snapshotFileType, System.currentTimeMillis(), name, fileMover)

                if (snapshot != null && inboxAll) {
                    collectorContext.addInboxItems(vm.parentIdentifier, snapshot.name, "", snapshot, vm)
                }
            }
        }
    }

    override fun getVms(ids: LongArray): Collection<VM> {
        if (ids.isEmpty()) {
            return emptyList()
        }
        val vms = ArrayList<VM>()
        for (id in ids) {
            val vm = getVmById(id)
            if (vm != null) {
                vms.add(vm)
            }
        }
        return vms
    }

    override fun getConnectedPooledVms(pool: VM): Collection<VM> {
        if (pool.type == VmType.POOL) {
            val vmData = vmRegistry.rootVmGroupData.getVmData(pool)
            if (vmData is VmGroupData) {
                return vmData.getConnectedVms()
            }
        }
        return emptyList()
    }

    fun getVmById(id: Long): VM? {
        return vmStorage.getVmById(id)
    }

    override val agentKeystore: File
        get() = sslManager.getAgentKeystore()

    override val rootGroupVM: VM
        get() = vmRegistry.getRootGroupVM()

    fun getGroupVM(groupIdentifier: VmIdentifier): VM {
        return vmRegistry.getGroupVM(groupIdentifier)
    }

    override val namedVms: Collection<VM>
        get() = vmStorage.getVmTree(null, VmType.NAMED)

    private fun getConnectionCount(): Long {
        return synchronized(connectionRegistry) { connectionRegistry.size().toLong() }
    }

    companion object {
        val SERVER_LOGGER = Loggers.SERVER
        val CONNECTION_LOGGER: Logger = LoggerFactory.getLogger("connection")
        val EVENT_LOGGER: Logger = LoggerFactory.getLogger("event")

        @Volatile
        private var singleInstance: VmManagerImpl? = null

        fun getGroupNames(groupId: String?): Array<String> =
            if (groupId.isNullOrBlank()) emptyArray() else groupId.split("/").toTypedArray()

        private fun getSnapshotFileType(commandType: CommandType): SnapshotFileType? {
            return when (commandType) {
                CommandType.THREAD_DUMP -> SnapshotFileType.THREAD_DUMP
                CommandType.HEAP_DUMP -> SnapshotFileType.HPZ
                CommandType.RECORD_JPROFILER -> SnapshotFileType.JPS
                CommandType.JFR_SNAPSHOT -> SnapshotFileType.JFR
                else -> null
            }
        }

        @Telemetry("JvmGuard VMs")
        @JvmStatic
        private fun getVmCount(): Long {
            return singleInstance?.getConnectionCount() ?: 0
        }
    }
}
