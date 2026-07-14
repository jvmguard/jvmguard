package dev.jvmguard.collector.telemetry

import dev.jvmguard.agent.AgentConstants
import dev.jvmguard.agent.comm.CommandType
import dev.jvmguard.agent.config.telemetry.TelemetryUnit
import dev.jvmguard.agent.data.Message
import dev.jvmguard.agent.telemetry.AdditionalData
import dev.jvmguard.agent.telemetry.TelemetryFormatImpl
import dev.jvmguard.agent.telemetry.TelemetryHelper
import dev.jvmguard.agent.telemetry.TelemetryResult
import dev.jvmguard.annotation.TelemetryFormat
import dev.jvmguard.collector.api.TelemetryProvider
import dev.jvmguard.collector.main.VmManagerImpl
import dev.jvmguard.collector.main.VmRegistry
import dev.jvmguard.collector.util.CurrentConnectionEntry
import dev.jvmguard.collector.vmdata.structures.TelemetryCollection.CollectionType
import dev.jvmguard.common.config.ConfigChangeListener
import dev.jvmguard.common.config.ConfigManager
import dev.jvmguard.common.notification.ModificationEvent
import dev.jvmguard.common.notification.ModificationType
import dev.jvmguard.common.telemetry.AdditionalTelemetryManager
import dev.jvmguard.data.config.GlobalConfig
import dev.jvmguard.data.vmdata.*
import dev.jvmguard.data.vmdata.CustomTelemetryNodeIdentifier.Type
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.util.*

@Component
class TelemetryManager(
    private val additionalTelemetryManager: AdditionalTelemetryManager,
    private val configManager: ConfigManager,
    private val eventPublisher: ApplicationEventPublisher,
    private val collectionTypeResolver: CollectionTypeResolver,
    private val vmRegistry: VmRegistry,
    private val dataPointManager: DataPointManager,
) : TelemetryProvider, ConfigChangeListener {

    private val plainTelemetryConverter = PlainTelemetryConverter(this, additionalTelemetryManager)

    private val idToTelemetryTypeMap = LinkedHashMap<String, TelemetryType>()

    private val nodeNameToMbeanTelemetryFormat = HashMap<String, TelemetryFormat>()
    private val assignedIdToNodeName = HashMap<String, String>()

    val summarizedSparklines = ArrayList<SummarizedTelemetryInfo>()

    init {
        val telemetryTypes = LinkedHashSet<TelemetryType>()
        telemetryTypes.add(TelemetryType(Telemetry.HEAP.mainId, TelemetryType.SUB_ID_USED_HEAP, CATEGORY_VM_TELEMETRIES, "Used Heap", TelemetryUnit.BYTES, 0))
        telemetryTypes.add(
            TelemetryType(
                Telemetry.HEAP.mainId,
                TelemetryType.SUB_ID_FREE_HEAP,
                CATEGORY_VM_TELEMETRIES,
                "Committed Free Heap",
                TelemetryUnit.BYTES,
                0
            )
        )
        telemetryTypes.add(
            TelemetryType(
                Telemetry.HEAP.mainId,
                TelemetryType.SUB_ID_USED_HEAP_PERCENTAGE,
                CATEGORY_VM_TELEMETRIES,
                "Used Heap Percentage",
                TelemetryUnit.PERCENT,
                2
            )
        )
        collectionTypeResolver.addSpecial(TelemetryIdentifier(Telemetry.HEAP.mainId, TelemetryType.SUB_ID_USED_HEAP_PERCENTAGE), CollectionType.SPARKLINE_ONLY)
        telemetryTypes.add(TelemetryType(Telemetry.CPU.mainId, null, CATEGORY_VM_TELEMETRIES, "CPU", TelemetryUnit.PERCENT, 2))
        telemetryTypes.add(TelemetryType(Telemetry.GC.mainId, null, CATEGORY_VM_TELEMETRIES, "GC Activity", TelemetryUnit.PERCENT, 2))
        telemetryTypes.add(TelemetryType(Telemetry.THREADS.mainId, null, CATEGORY_VM_TELEMETRIES, "Thread Count", TelemetryUnit.PLAIN, 0))
        telemetryTypes.add(TelemetryType(Telemetry.CONNECTIONS.mainId, null, CATEGORY_VM_TELEMETRIES, "Connected VMs", TelemetryUnit.PLAIN, 0))

        addTransaction(telemetryTypes, Telemetry.TRANSACTIONS.mainId, CATEGORY_TRANSACTIONS)

        for (additionalTelemetry in additionalTelemetryManager.getTelemetries(AgentConstants.TELEMETRY_TYPE_DECLARED, false)) {
            val telemetryFormat = additionalTelemetryManager.getFormat(additionalTelemetry)
            telemetryTypes.add(
                TelemetryType(
                    Telemetry.CUSTOM.mainId, additionalTelemetry.assignedStringId, CATEGORY_CUSTOM, additionalTelemetry.description,
                    TelemetryUnit.fromAnnotationUnit(telemetryFormat.value), CUSTOM_TELEMETRY_BASE_SCALE + telemetryFormat.scale
                )
            )
        }

        synchronized(idToTelemetryTypeMap) {
            for (telemetryType in telemetryTypes) {
                idToTelemetryTypeMap[telemetryType.telemetryIdentifier.combinedId] = telemetryType
            }
        }

        configManager.addConfigChangeListener(this)
        groupConfigsChanged()
    }

    private fun addTransaction(telemetryTypes: MutableSet<TelemetryType>, mainId: String, category: String) {
        telemetryTypes.add(TelemetryType(mainId, TelemetryType.SUB_ID_COMPLETED, category, "Completed $category", TelemetryUnit.PER_SECOND, 4))
        telemetryTypes.add(TelemetryType(mainId, TelemetryType.SUB_ID_NORMAL, category, "Normal $category", TelemetryUnit.PER_SECOND, 4))
        telemetryTypes.add(TelemetryType(mainId, TelemetryType.SUB_ID_SLOW, category, "Slow $category", TelemetryUnit.PER_SECOND, 4))
        telemetryTypes.add(TelemetryType(mainId, TelemetryType.SUB_ID_VERY_SLOW, category, "Very Slow $category", TelemetryUnit.PER_SECOND, 4))
        telemetryTypes.add(TelemetryType(mainId, TelemetryType.SUB_ID_ERROR, category, "Error $category", TelemetryUnit.PER_SECOND, 4))
        telemetryTypes.add(
            TelemetryType(
                mainId,
                TelemetryType.SUB_ID_AVERAGE,
                category,
                "Average " + category.substring(0, category.length - 1) + " Duration",
                TelemetryUnit.NANOSECONDS,
                0
            )
        )
        summarizedSparklines.add(
            SummarizedTelemetryInfo(
                mainId,
                TelemetryType.SUB_ID_COMPLETED,
                TelemetryType.SUB_ID_NORMAL,
                TelemetryType.SUB_ID_SLOW,
                TelemetryType.SUB_ID_VERY_SLOW,
                TelemetryType.SUB_ID_ERROR
            )
        )
        collectionTypeResolver.addSpecial(TelemetryIdentifier(mainId, TelemetryType.SUB_ID_COMPLETED), CollectionType.SPARKLINE_ONLY)
    }

    fun retrieveData(connectionEntry: CurrentConnectionEntry, vm: VM): List<Message>? {
        val agentConnection = connectionEntry.agentConnection
        try {
            val telemetryData = agentConnection.executeCommand(CommandType.TELEMETRY) as TelemetryResult
            val usedMemory = telemetryData.totalMemory - telemetryData.freeMemory
            dataPointManager.addData(connectionEntry, Telemetry.HEAP.mainId, TelemetryType.SUB_ID_USED_HEAP, true, usedMemory)
            dataPointManager.addData(connectionEntry, Telemetry.HEAP.mainId, TelemetryType.SUB_ID_FREE_HEAP, true, telemetryData.freeMemory)
            if (telemetryData.maxMemory > 0 && telemetryData.maxMemory < Long.MAX_VALUE) {
                dataPointManager.addData(
                    connectionEntry,
                    Telemetry.HEAP.mainId,
                    TelemetryType.SUB_ID_USED_HEAP_PERCENTAGE,
                    true,
                    usedMemory * 10000 / telemetryData.maxMemory
                )
            }
            dataPointManager.addData(connectionEntry, Telemetry.CPU.mainId, "", true, telemetryData.cpuLoad)
            dataPointManager.addData(connectionEntry, Telemetry.THREADS.mainId, "", true, telemetryData.threadCount)
            dataPointManager.addData(connectionEntry, Telemetry.GC.mainId, "", true, telemetryData.gcActivity)

            var declaredTelemetriesModified = false
            for (additionalData in telemetryData.additionalData) {
                if (additionalData.type == AgentConstants.TELEMETRY_TYPE_DECLARED) {
                    if (addDeclaredTelemetryFormat(connectionEntry, additionalData)) {
                        declaredTelemetriesModified = true
                    }
                }
            }
            for (additionalData in telemetryData.additionalData) {
                if (additionalData.type == AgentConstants.TELEMETRY_TYPE_MBEAN) {
                    addMBeanTelemetryData(connectionEntry, additionalData)
                } else if (additionalData.type == AgentConstants.TELEMETRY_TYPE_DECLARED) {
                    addDeclaredTelemetryData(connectionEntry, additionalData)
                } else {
                    val additionalTelemetry = additionalTelemetryManager.getOrCreateAdditionalTelemetry(additionalData.type, additionalData.name)
                    if (additionalTelemetry != null) {
                        dataPointManager.addData(connectionEntry, Telemetry.HEAP.mainId, additionalTelemetry.assignedStringId, true, additionalData.value)
                    }
                }
            }

            if (declaredTelemetriesModified) {
                eventPublisher.publishEvent(ModificationEvent(this, null, ModificationType.TELEMETRY_IDS))
            }

            return telemetryData.messages
        } catch (e: Throwable) {
            VmManagerImpl.CONNECTION_LOGGER.error("error retrieving telemetry data on {}", vm.verbose, e)
        }
        return null
    }

    private fun addDeclaredTelemetryData(connectionEntry: CurrentConnectionEntry, additionalData: AdditionalData) {
        val additionalTelemetry = additionalTelemetryManager.getOrCreateAdditionalTelemetry(additionalData.type, additionalData.name)
        if (additionalTelemetry != null) {
            val format = additionalTelemetryManager.getFormat(additionalTelemetry)
            dataPointManager.addData(connectionEntry, Telemetry.CUSTOM.mainId, additionalTelemetry.assignedStringId, format.groupAverage, additionalData.value)
        }
    }

    private fun addDeclaredTelemetryFormat(connectionEntry: CurrentConnectionEntry, additionalData: AdditionalData): Boolean {
        val additionalTelemetry = additionalTelemetryManager.getOrCreateAdditionalTelemetry(additionalData.type, additionalData.name)
        if (additionalTelemetry != null) {
            var format = additionalData.format
            var formatUpdated = false
            if (format != null && connectionEntry.committedDeclaredTelemetryFormats.add(additionalTelemetry.nodeName)) {
                formatUpdated = additionalTelemetryManager.updateFormat(additionalTelemetry, format)
            } else {
                format = additionalTelemetryManager.getFormat(additionalTelemetry)
            }

            val assignedId = additionalTelemetry.assignedStringId
            val telemetryType = TelemetryType(
                Telemetry.CUSTOM.mainId,
                assignedId,
                additionalData.type,
                additionalData.name,
                CATEGORY_CUSTOM,
                additionalTelemetry.description,
                TelemetryUnit.fromAnnotationUnit(format.value),
                CUSTOM_TELEMETRY_BASE_SCALE + format.scale
            )
            val previousType = getTelemetryType(telemetryType.telemetryIdentifier.combinedId)
            if (previousType == null || formatUpdated) {
                putTelemetryType(telemetryType.telemetryIdentifier.combinedId, telemetryType)
                return true
            }
        }
        return false
    }

    fun getTelemetryType(id: String): TelemetryType? =
        synchronized(idToTelemetryTypeMap) {
            idToTelemetryTypeMap[id]
        }

    private fun putTelemetryType(id: String, telemetryType: TelemetryType): TelemetryType? =
        synchronized(idToTelemetryTypeMap) {
            idToTelemetryTypeMap.put(id, telemetryType)
        }

    private fun addMBeanTelemetryData(connectionEntry: CurrentConnectionEntry, additionalData: AdditionalData) {
        val additionalTelemetry = additionalTelemetryManager.getAdditionalTelemetry(additionalData.type, additionalData.name)
        if (additionalTelemetry != null) {
            val format = getMbeanTelemetryFormat(additionalTelemetry.nodeName)
            if (format != null) {
                dataPointManager.addData(
                    connectionEntry,
                    Telemetry.CUSTOM.mainId,
                    additionalTelemetry.assignedStringId,
                    format.groupAverage,
                    additionalData.value
                )
            }
        }
    }

    override val idToTelemetryType: Map<String, TelemetryType>
        get() {
            val types = synchronized(idToTelemetryTypeMap) {
                ArrayList(idToTelemetryTypeMap.values)
            }
            val ret = LinkedHashMap<String, TelemetryType>()
            for (telemetryType in types) {
                var resolved = telemetryType
                if (Telemetry.CUSTOM.mainId == resolved.telemetryIdentifier.mainId && additionalTelemetryManager.getVisibleTelemetry(resolved.telemetryIdentifier.subId) == null) {
                    resolved = resolved.clone().visible(false)
                }
                ret[resolved.telemetryIdentifier.combinedId] = resolved
            }
            return ret
        }

    override fun getTelemetryData(vm: VM?, mainId: String, interval: TelemetryInterval, endTime: Long, plainHeap: Boolean): TelemetryData {
        val resolvedVm = vm ?: vmRegistry.getRootGroupVM()
        return plainTelemetryConverter.createTelemetryData(
            mainId,
            dataPointManager.getTelemetryData(resolvedVm, mainId, interval, endTime),
            interval,
            plainHeap
        )
    }

    override fun getCustomTelemetryData(vm: VM?, nodeIdentifier: CustomTelemetryNodeIdentifier, interval: TelemetryInterval, endTime: Long): TelemetryData {
        val resolvedVm = vm ?: vmRegistry.getRootGroupVM()
        return plainTelemetryConverter.createCustomTelemetryData(
            nodeIdentifier,
            dataPointManager.getTelemetryData(resolvedVm, Telemetry.CUSTOM.mainId, interval, endTime),
            interval
        )
    }

    fun getOrCreateTelemetryType(telemetryIdentifier: PersistentTelemetryIdentifier): TelemetryType? {
        var telemetryType = getTelemetryType(telemetryIdentifier.combinedId)
        if (telemetryType == null) {
            if (telemetryIdentifier.mainId == Telemetry.CUSTOM.mainId && telemetryIdentifier.additionalType == AgentConstants.TELEMETRY_TYPE_DECLARED) { // can happen after import, declared telemetries are not added automatically to idToTelemetryType
                val additionalTelemetry =
                    additionalTelemetryManager.getOrCreateAdditionalTelemetry(AgentConstants.TELEMETRY_TYPE_DECLARED, telemetryIdentifier.additionalName)
                if (additionalTelemetry != null) {
                    val format = additionalTelemetryManager.getFormat(additionalTelemetry)
                    telemetryType = TelemetryType(
                        telemetryIdentifier.mainId,
                        telemetryIdentifier.subId,
                        telemetryIdentifier.additionalType,
                        telemetryIdentifier.additionalName,
                        CATEGORY_CUSTOM,
                        additionalTelemetry.description,
                        TelemetryUnit.fromAnnotationUnit(format.value),
                        CUSTOM_TELEMETRY_BASE_SCALE + format.scale
                    )
                    putTelemetryType(telemetryType.telemetryIdentifier.combinedId, telemetryType)
                    eventPublisher.publishEvent(ModificationEvent(this, null, ModificationType.TELEMETRY_IDS))
                }
            }
        }
        return telemetryType
    }

    override fun globalConfigChanged(oldConfig: GlobalConfig?, newConfig: GlobalConfig) {
    }

    override fun groupConfigsChanged() {
        val changed: Boolean
        val newTypes = HashMap<String, TelemetryType>()
        synchronized(nodeNameToMbeanTelemetryFormat) {
            val previousEmpty = nodeNameToMbeanTelemetryFormat.isEmpty()
            nodeNameToMbeanTelemetryFormat.clear()
            assignedIdToNodeName.clear()
            for (groupConfig in configManager.getGroupConfigs()) {
                for (telemetryConfig in groupConfig.telemetrySettings.mbeanTelemetries) {
                    val format = TelemetryFormatImpl(
                        telemetryConfig.unit.annotationUnit,
                        telemetryConfig.isStacked,
                        telemetryConfig.isGroupAveraged,
                        telemetryConfig.scale
                    )
                    val nodeName = telemetryConfig.name
                    nodeNameToMbeanTelemetryFormat[nodeName] = format
                    for (lineConfig in telemetryConfig.lines) {
                        val name = TelemetryHelper.getIdentifier(nodeName, lineConfig.lineName)
                        val additionalTelemetry =
                            additionalTelemetryManager.getOrCreateAdditionalTelemetry(AgentConstants.TELEMETRY_TYPE_MBEAN, name) ?: continue
                        val assignedId = additionalTelemetry.assignedStringId
                        val telemetryType = TelemetryType(
                            Telemetry.CUSTOM.mainId,
                            assignedId,
                            additionalTelemetry.type,
                            name,
                            CATEGORY_CUSTOM,
                            additionalTelemetry.description,
                            TelemetryUnit.fromAnnotationUnit(format.value),
                            CUSTOM_TELEMETRY_BASE_SCALE + format.scale
                        )
                        assignedIdToNodeName[assignedId] = nodeName
                        newTypes[name] = telemetryType
                    }
                }
            }
            changed = !previousEmpty || nodeNameToMbeanTelemetryFormat.isNotEmpty()
        }
        if (changed) {
            synchronized(idToTelemetryTypeMap) {
                idToTelemetryTypeMap.values.removeIf { it.telemetryIdentifier.additionalType == AgentConstants.TELEMETRY_TYPE_MBEAN }
                for (telemetryType in newTypes.values) {
                    idToTelemetryTypeMap[telemetryType.telemetryIdentifier.combinedId] = telemetryType
                }
            }
            eventPublisher.publishEvent(ModificationEvent(this, null, ModificationType.TELEMETRY_IDS))
        }
    }

    fun getMbeanTelemetryFormat(nodeName: String): TelemetryFormat? =
        synchronized(nodeNameToMbeanTelemetryFormat) {
            val tabIndex = nodeName.indexOf('\t')
            val key = if (tabIndex > -1) nodeName.substring(0, tabIndex) else nodeName
            nodeNameToMbeanTelemetryFormat[key]
        }

    override val customTelemetryInfo: CustomTelemetryInfo
        get() {
            val ret = TreeSet<CustomTelemetryNodeIdentifier>()
            synchronized(nodeNameToMbeanTelemetryFormat) {
                for (nodeName in nodeNameToMbeanTelemetryFormat.keys) {
                    ret.add(CustomTelemetryNodeIdentifier(Type.MBEAN, nodeName))
                }
            }
            for (additionalTelemetry in additionalTelemetryManager.getTelemetries(AgentConstants.TELEMETRY_TYPE_DECLARED, true)) {
                ret.add(CustomTelemetryNodeIdentifier(Type.DECLARED, additionalTelemetry.nodeName))
            }
            return CustomTelemetryInfo(ArrayList(ret))
        }

    override val hiddenDeclaredTelemetryNodes: Collection<String>
        get() {
            val ret = TreeSet<String>()
            for (additionalTelemetry in additionalTelemetryManager.getHiddenTelemetries(AgentConstants.TELEMETRY_TYPE_DECLARED)) {
                ret.add(additionalTelemetry.nodeName)
            }
            return ArrayList(ret)
        }

    override fun setDeclaredTelemetryNodeVisibility(nodeName: String, visible: Boolean): Boolean {
        if (additionalTelemetryManager.setDeclaredTelemetryHidden(nodeName, !visible)) {
            eventPublisher.publishEvent(ModificationEvent(this, null, ModificationType.TELEMETRY_IDS))
            return true
        }
        return false
    }

    class SummarizedTelemetryInfo(val mainId: String, val summarizedId: String, vararg componentIds: String) {
        private val componentIds: Set<String> = HashSet(componentIds.asList())

        fun isIncluded(subId: String): Boolean = componentIds.contains(subId)
    }

    companion object {
        val TRANSACTION_RECORDING_INTERVAL: TelemetryDataInterval = TelemetryDataInterval.getRecordingInterval(Telemetry.TRANSACTIONS.mainId)

        val DEFAULT_RECORDING_INTERVAL: TelemetryDataInterval = TelemetryDataInterval.getRecordingInterval("")

        private const val CATEGORY_VM_TELEMETRIES = "VM Telemetries"
        private const val CATEGORY_TRANSACTIONS = "Transactions"
        private const val CATEGORY_CUSTOM = "Custom Telemetries"

        const val CUSTOM_TELEMETRY_BASE_SCALE = 2

    }
}
