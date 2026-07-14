package dev.jvmguard.connector.server.mock

import dev.jvmguard.agent.config.VmType
import dev.jvmguard.agent.config.base.AbstractEntity
import dev.jvmguard.agent.config.base.Identifiable
import dev.jvmguard.agent.config.base.LogCategory
import dev.jvmguard.agent.config.telemetry.TelemetryUnit
import dev.jvmguard.common.helper.GroupHelper
import dev.jvmguard.common.helper.ListModification
import dev.jvmguard.common.io.FileUtil
import dev.jvmguard.common.util.BeanUtil
import dev.jvmguard.data.base.StoredConfig
import dev.jvmguard.data.config.FrequencyUnit
import dev.jvmguard.data.config.GroupConfig
import dev.jvmguard.data.config.sets.*
import dev.jvmguard.data.config.triggers.actions.EmailAction
import dev.jvmguard.data.config.triggers.actions.LogAction
import dev.jvmguard.data.config.triggers.actions.RecordJpsAction
import dev.jvmguard.data.config.triggers.actions.TriggerAction
import dev.jvmguard.data.dashboard.Group
import dev.jvmguard.data.file.SnapshotFile
import dev.jvmguard.data.file.SnapshotFileType
import dev.jvmguard.data.user.InboxItem
import dev.jvmguard.data.user.User
import dev.jvmguard.data.vmdata.*
import java.lang.reflect.Field
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

class MockEntities(private val serverConnection: MockServerConnectionImpl) {

    private var idCounter = 0

    private val entityLists = HashMap<Class<*>, MutableList<out Identifiable>>()
    private var inboxAddTime: Long = 0

    private val users: MutableList<User>
    private val transactionDefSets: MutableList<TransactionDefSet>
    private val thresholdSets: MutableList<ThresholdSet>
    private val triggerSets: MutableList<TriggerSet>
    private val actionsSets: MutableList<ActionSet>
    private val telemetrySets: MutableList<TelemetrySet>
    private val vms: MutableList<VM> = ArrayList()
    private val inboxItems: MutableList<InboxItem>
    private val snapshotFiles: MutableList<SnapshotFile>
    private val groupConfigs: MutableList<GroupConfig>

    private val sparkLineRangeToDataHolders = IdentityHashMap<SparkLineRange, MutableMap<VM, VmDataHolder>>()
    private val sparklineFactors = IdentityHashMap<VM, Float>()
    private val connectionStatuses = IdentityHashMap<VM, ConnectionStatus>()
    private val sparkLineIndexRanges = IdentityHashMap<VM, IndexRange>()
    private val telemetryTypes: MutableList<TelemetryType>
    private val idToSparklineType = HashMap<String, TelemetryType>()

    private var erpCount = 2
    private var erpAdd = true
    private var currentValuesUpdateTime: Long = 0
    private var hourUpdateTime: Long = 0
    private var dayUpdateTime: Long = 0

    init {
        users = createEntityList(User::class.java)
        configureUsers()
        transactionDefSets = createEntityList(TransactionDefSet::class.java)
        configureTransactionDefSets()
        thresholdSets = createEntityList(ThresholdSet::class.java)
        configureThresholdSets()
        triggerSets = createEntityList(TriggerSet::class.java)
        configureTriggerSets()
        actionsSets = createEntityList(ActionSet::class.java)
        configureActionSets()
        telemetrySets = createEntityList(TelemetrySet::class.java)
        configureVms()
        inboxItems = createEntityList(InboxItem::class.java)
        snapshotFiles = createEntityList(SnapshotFile::class.java)
        configureInboxItems()
        groupConfigs = createEntityList(GroupConfig::class.java)
        configureGroupConfigs()
        inboxAddTime = serverConnection.currentTime

        telemetryTypes = ArrayList()
        configureSparkLineTypes()
    }

    fun getVms(): MutableList<VM> = vms

    fun getVmsWithGroups(): List<VM> {
        val handledGroupNames = HashSet<String>()
        val vmsWithGroup = ArrayList<VM>()
        for (vm in vms) {
            vmsWithGroup.add(vm)
            addGroupVms(handledGroupNames, vmsWithGroup, GroupHelper.getParentHierarchyPath(vm.hierarchyPath))
        }
        return vmsWithGroup
    }

    private fun addGroupVms(handledGroupNames: MutableSet<String>, vmsWithGroup: MutableList<VM>, hierarchyPath: String?) {
        val path = hierarchyPath ?: GroupHelper.ROOT_GROUP_ID
        if (!handledGroupNames.contains(path)) {
            vmsWithGroup.add(createGroupVm(path, VmType.GROUP))
            handledGroupNames.add(path)
            if (path != GroupHelper.ROOT_GROUP_ID) {
                addGroupVms(handledGroupNames, vmsWithGroup, GroupHelper.getParentHierarchyPath(path))
            }
        }
    }

    fun getInboxItems(): Collection<InboxItem> {
        updateInboxItems()
        return inboxItems
    }

    private fun updateInboxItems() {
        if (serverConnection.currentTime - inboxAddTime > TimeHelper.MILLISECONDS_TO_MINUTE) {
            inboxItems.add(createSnapshotFileInboxItem(0, SnapshotFileType.HPZ))
            inboxAddTime = serverConnection.currentTime
        }
    }

    fun getUsers(): Collection<User> = users

    fun getGroupConfigs(): Collection<GroupConfig> = groupConfigs

    @Suppress("UNCHECKED_CAST")
    fun applyListModification(listModification: ListModification<*>) {
        val entityList = entityLists[listModification.itemClass] as MutableList<Identifiable>
        val modifiedItems = listModification.modifiedOrNewItems()
        fillIds(modifiedItems)
        for (item in modifiedItems) {
            val existingItem = getById(entityList, item.id)
            if (existingItem != null) {
                BeanUtil.copyValues(item, existingItem)
            } else {
                entityList.add(item)
            }
        }
        for (item in listModification.removedItems) {
            entityList.remove(item)
        }
    }

    private fun getById(entityList: List<Identifiable>, id: Long?): Identifiable? =
        entityList.firstOrNull { it.id == id }

    fun getTransactionDefSets(): Collection<TransactionDefSet> = transactionDefSets

    fun getThresholdSets(): Collection<ThresholdSet> = thresholdSets

    fun getTriggerSets(): Collection<TriggerSet> = triggerSets

    fun getActionsSets(): Collection<ActionSet> = actionsSets

    fun getTelemetrySets(): Collection<TelemetrySet> = telemetrySets

    fun getSnapshotFiles(snapshotFileType: SnapshotFileType?, vm: VM?): Collection<SnapshotFile> {
        updateInboxItems()
        return snapshotFiles.filter {
            (snapshotFileType == null || it.type == snapshotFileType) && vm == it.vm
        }
    }

    fun getSnapshotFile(id: Long): SnapshotFile? =
        snapshotFiles.firstOrNull { it.id == id }

    private fun <T : Identifiable> createEntityList(itemClass: Class<T>): MutableList<T> {
        val entityList = ArrayList<T>()
        entityLists[itemClass] = entityList
        return entityList
    }

    private fun <T : Identifiable> fillIds(entityList: Iterable<T>) {
        for (bean in entityList) {
            fillId(bean)
        }
    }

    private fun <T : Identifiable> fillId(bean: T): T {
        if (bean.id == null) {
            val id = (++idCounter).toLong()
            if (bean is StoredConfig) {
                bean.id = id
            } else {
                try {
                    ID_FIELD.set(bean, id)
                } catch (e: IllegalAccessException) {
                    throw RuntimeException(e)
                }
            }
        }
        return bean
    }

    private fun configureUsers() {
        users.add(getUser())
        fillIds(users)
    }

    private fun fillHeapTriggerActions(triggerActions: MutableList<TriggerAction>): List<TriggerAction> {
        triggerActions.add(LogAction().apply {
            category = LogCategory.WARNING
            text = "The heap is getting to big!"
        })
        triggerActions.add(EmailAction().apply {
            email = "tester@test.com"
            text = "Heap anomaly detected. Auto-destruct in T-7 minutes."
        })
        return triggerActions
    }

    private fun fillCpuTriggerActions(triggerActions: MutableList<TriggerAction>): Collection<TriggerAction> {
        triggerActions.add(LogAction().apply {
            category = LogCategory.WARNING
            text = "The CPU will overheat"
        })
        triggerActions.add(RecordJpsAction().apply {
            artifactName = "CPU threshold exceeded"
        })
        triggerActions.add(EmailAction().apply {
            email = "tester@test.com"
            text = "The CPU might be melting, profiling data has been acquired."
        })
        return triggerActions
    }

    private fun configureThresholdSets() {
        fillIds(thresholdSets)
    }

    private fun configureTriggerSets() {
        fillIds(triggerSets)
    }

    private fun configureTransactionDefSets() {
        fillIds(transactionDefSets)
    }

    private fun configureActionSets() {
        actionsSets.add(ActionSet("My CPU actions", fillCpuTriggerActions(ArrayList())))
        actionsSets.add(ActionSet("My heap actions", fillHeapTriggerActions(ArrayList())))
        fillIds(actionsSets)
    }

    private fun configureVms() {
        for (i in 0 until 5) {
            val vm = createVm("db", i, "Database", "DB", 1f, ConnectionStatus.CONNECTED)
            vms.add(vm)
            if (i > 2) {
                sparkLineIndexRanges[vm] = IndexRange(20, -1)
            }
        }
        for (i in 0 until 12) {
            vms.add(createVm("web", i, "Web VMs", "Web", 0.5f, if (i < 10) ConnectionStatus.CONNECTED else ConnectionStatus.RECENT))
        }
        for (i in 0 until 5) {
            vms.add(createVm("jms", i, "ERP/Messaging", "JMS", 0.3f, ConnectionStatus.CONNECTED))
        }
        for (i in 0 until 5) {
            vms.add(createVm("processing", i, "ERP/Processing", "Processing", 1f, ConnectionStatus.CONNECTED))
        }
        for (i in 0 until 2) {
            vms.add(createVm("manager", i, "ERP", "ERP Manager", 0.1f, ConnectionStatus.CONNECTED))
        }
        for (i in 0 until 5) {
            vms.add(createVm("test", i, "", "Test", 1f, if (i < 2) ConnectionStatus.RECENT else ConnectionStatus.DISCONNECTED))
        }
        for (i in 0 until 5) {
            vms.add(createVm("validation", i, "ERP/Validation", "Validation", 1f, if (i < 2) ConnectionStatus.RECENT else ConnectionStatus.DISCONNECTED))
        }

        var id = 1L
        for (vm in vms) {
            vm.id = id++
        }
    }

    private fun createVm(hostname: String, index: Int, groupId: String, name: String, sparklineFactor: Float, connectionStatus: ConnectionStatus): VM {
        val vm = VM(VmType.NAMED, 0, 0, "$name ${padIndex(index)}", groupId, createClusterHostname(hostname, index), 0)
        vm.groupName = groupId
        sparklineFactors[vm] = sparklineFactor
        connectionStatuses[vm] = connectionStatus
        if (connectionStatus == ConnectionStatus.RECENT) {
            sparkLineIndexRanges[vm] = IndexRange(0, 20)
        } else if (connectionStatus == ConnectionStatus.DISCONNECTED) {
            sparkLineIndexRanges[vm] = IndexRange(0, 0)
        }
        return vm
    }

    private fun createClusterHostname(prefix: String, i: Int): String = "$prefix${padIndex(i)}.mycluster.com"

    private fun padIndex(i: Int): String = String.format("%02d", i + 1)

    private fun configureSparkLineTypes() {
        telemetryTypes.add(TelemetryType(Telemetry.HEAP.mainId, TelemetryType.SUB_ID_USED_HEAP, "", "Heap size", TelemetryUnit.BYTES, 0))
        telemetryTypes.add(TelemetryType(Telemetry.CPU.mainId, null, "", "CPU load", TelemetryUnit.PERCENT, 0))
        for (i in 0 until 10) {
            telemetryTypes.add(TelemetryType("fake$i", "", "Fake telemetries", "Fake telemetry ${i + 1}", TelemetryUnit.PLAIN, 0))
        }

        for (telemetryType in telemetryTypes) {
            idToSparklineType[telemetryType.telemetryIdentifier.combinedId] = telemetryType
        }
    }

    private fun configureInboxItems() {
        inboxItems.add(createSnapshotFileInboxItem(-1, SnapshotFileType.HPZ))
        inboxItems.add(createSnapshotFileInboxItem(-2, SnapshotFileType.THREAD_DUMP))
        inboxItems.add(createSnapshotFileInboxItem(-3, SnapshotFileType.JPS))
        inboxItems.add(createMessageInboxItem(-1))
        inboxItems.add(createMessageInboxItem(-2))
    }

    private fun createSnapshotFileInboxItem(hours: Int, itemType: SnapshotFileType): InboxItem {
        val snapshotFile = fillId(SnapshotFile(null, vms.first(), itemType, System.currentTimeMillis(), "Demo $itemType"))
        fillId(snapshotFile)
        snapshotFiles.add(snapshotFile)

        val inboxItem = fillId(InboxItem(null, System.currentTimeMillis(), snapshotFile.id, snapshotFile.type, vms.first(), "item", "", false))
        inboxItem.date = Instant.now().minus(hours.toLong(), ChronoUnit.HOURS)

        try {
            FileUtil.writeTextFile(snapshotFile.file, "demo")
        } catch (e: java.io.IOException) {
            throw RuntimeException(e)
        }

        return inboxItem
    }

    private fun createMessageInboxItem(hours: Int): InboxItem =
        fillId(
            InboxItem(
                null,
                Instant.now().minus(hours.toLong(), ChronoUnit.HOURS).toEpochMilli(),
                null,
                // TODO InboxItem.snapshotFileType should be nullable (message items have no snapshot);
                //  the data API declares it non-null, so a placeholder type is required here.
                SnapshotFileType.JPS,
                null,
                "Fatal error message",
                "This is a lorem ipsum message.\n\n.$LOREM_IPSUM\n\n$LOREM_IPSUM\n\n$LOREM_IPSUM",
                false,
            ),
        )

    private fun getUser(): User = serverConnection.user

    private fun configureGroupConfigs() {
        groupConfigs.add(GroupConfig.createDefault())
        groupConfigs.add(GroupConfig.createDefault(VmIdentifier("ERP", VmType.GROUP)))
        groupConfigs.add(GroupConfig.createDefault(VmIdentifier("ERP/Messaging", VmType.GROUP)))
        groupConfigs.add(GroupConfig.createDefault(VmIdentifier("Web VMs", VmType.GROUP)))
        fillIds(groupConfigs)
    }

    fun getVmDataHolders(vmFilter: VmFilter, sparkLineRange: SparkLineRange): Group<VmDataHolder> {
        val vms = getVms()
        if (erpAdd) {
            for (i in erpCount until erpCount + 2) {
                vms.add(createVm("manager", i, "ERP", "ERP Manager", 0.1f, ConnectionStatus.CONNECTED))
            }
            erpCount += 2
            if (erpCount > 12) {
                erpAdd = false
            }
        } else {
            var removeCount = 0
            val it = vms.listIterator(vms.size)
            while (removeCount < 2 && it.hasPrevious()) {
                val vm = it.previous()
                if (vm.name.startsWith("ERP")) {
                    it.remove()
                    removeCount++
                }
            }
            erpCount -= 2
            if (erpCount < 4) {
                erpAdd = true
            }
        }
        val random = serverConnection.getRandom()
        val holderList = ArrayList<VmDataHolder>()
        val currentTime = serverConnection.currentTime
        val updateCurrentValues = currentTime - currentValuesUpdateTime > 1000
        val updateHour = currentTime - hourUpdateTime > SparkLineRange.LAST_HOUR.dataInterval.millis
        val updateDay = currentTime - dayUpdateTime > SparkLineRange.LAST_DAY.dataInterval.millis

        val dataHolders = sparkLineRangeToDataHolders.computeIfAbsent(sparkLineRange) { HashMap() }

        for (vm in vms) {
            val dataHolder = dataHolders.computeIfAbsent(vm) { createVmDataHolder(it, currentTime, random, sparkLineRange) }
            if (updateDay) {
                updateData(dataHolder, random)
            }
            if (updateHour) {
                updateData(dataHolder, random)
            }
            if (updateCurrentValues && dataHolder.isConnected) {
                updateCurrentValues(dataHolder, random)
            }
            if (dataHolder.isConnected || (vmFilter == VmFilter.RECENT && (currentTime - dataHolder.statusChangeTime) < 1000 * 60 * 60)) {
                holderList.add(dataHolder)
            }
        }
        if (updateCurrentValues) {
            currentValuesUpdateTime = currentTime
        }
        if (updateDay) {
            dayUpdateTime = currentTime
        }
        if (updateHour) {
            hourUpdateTime = currentTime
        }
        val groupVmDataHolders = createGroups(holderList, sparkLineRange)

        return getGrouped(groupVmDataHolders, holderList)
    }

    fun getVmDataHolder(vm: VM, sparkLineRange: SparkLineRange): VmDataHolder {
        var dataHolders = sparkLineRangeToDataHolders[sparkLineRange]
        if (dataHolders == null || dataHolders[vm] == null) {
            // Populate the per-range cache (every VM gets a holder before the connection filter applies).
            getVmDataHolders(VmFilter.RECENT, sparkLineRange)
            dataHolders = sparkLineRangeToDataHolders[sparkLineRange]
        }
        return dataHolders!!.getValue(vm)
    }

    private fun getGrouped(groupVmDataHolders: List<VmDataHolder>, holderList: List<VmDataHolder>): Group<VmDataHolder> {
        val rootGroup = Group<VmDataHolder>()
        for (vmDataHolder in groupVmDataHolders) {
            val hierarchyPath = vmDataHolder.vm.hierarchyPath
            if (hierarchyPath.isEmpty()) {
                rootGroup.data = vmDataHolder
            } else {
                var currentGroup = rootGroup
                val names = hierarchyPath.split("/")
                for (i in names.indices) {
                    currentGroup = currentGroup.getOrCreateGroupChild(VmIdentifier(names[i], vmDataHolder.vm.type.parentType))
                    if (i == names.size - 1) {
                        currentGroup.data = vmDataHolder
                    }
                }
            }
        }
        for (vmDataHolder in holderList) {
            val hierarchyPath = vmDataHolder.vm.hierarchyPath
            var currentGroup = rootGroup
            val names = hierarchyPath.split("/")
            for (i in names.indices) {
                if (i < names.size - 1) {
                    currentGroup = currentGroup.getOrCreateGroupChild(VmIdentifier(names[i], vmDataHolder.vm.type.parentType))
                } else {
                    currentGroup.setVmData(vmDataHolder.vm, vmDataHolder)
                }
            }
        }
        return rootGroup
    }

    private fun createGroups(vmDataHolders: List<VmDataHolder>, sparkLineRange: SparkLineRange): List<VmDataHolder> {
        val groupVmDataHolders = ArrayList<VmDataHolder>()
        val groupIdToDataHolder = HashMap<String, VmDataHolder>()
        val groupIdToCount = HashMap<String, Int>()
        for (vmDataHolder in vmDataHolders) {
            addGroupData(GroupHelper.getParentHierarchyPath(vmDataHolder.vm.hierarchyPath), groupIdToDataHolder, groupIdToCount, sparkLineRange, vmDataHolder)
        }

        for ((groupId, vmDataHolder) in groupIdToDataHolder) {
            val count = groupIdToCount[groupId]!!
            for (telemetryType in telemetryTypes) {
                val data = vmDataHolder.getSparkLineData(telemetryType).data
                for (i in data.indices) {
                    data[i] = data[i] / count
                }
            }
            groupVmDataHolders.add(vmDataHolder)
        }

        groupVmDataHolders.sortBy { it.vm }

        return groupVmDataHolders
    }

    private fun addGroupData(
        hierarchyPath: String?,
        groupIdToDataHolder: MutableMap<String, VmDataHolder>,
        groupIdToCount: MutableMap<String, Int>,
        sparkLineRange: SparkLineRange,
        vmDataHolder: VmDataHolder
    ) {
        val path = hierarchyPath ?: GroupHelper.ROOT_GROUP_ID
        var groupDataHolder = groupIdToDataHolder[path]
        val createGroupDataHolder = groupDataHolder == null
        if (createGroupDataHolder) {
            groupDataHolder =
                VmDataHolder(
                    createGroupVm(path, vmDataHolder.vm.type.parentType),
                    isConnected = true,
                    isOutdatedAgent = false,
                    statusChangeTime = 0,
                    sparkLineRange = sparkLineRange,
                    frequencyUnit = vmDataHolder.frequencyUnit,
                    hostName = "",
                    port = 0
                )
            groupIdToDataHolder[path] = groupDataHolder
        }

        val count = groupIdToCount[path] ?: 0
        groupIdToCount[path] = count + 1

        for (telemetryType in telemetryTypes) {
            val sparkLineData = vmDataHolder.getSparkLineData(telemetryType)
            if (createGroupDataHolder) {
                val data = sparkLineData.data
                val dataCopy = LongArray(data.size)
                System.arraycopy(data, 0, dataCopy, 0, data.size)
                val copy =
                    SparkLineData(sparkLineData.telemetryType, sparkLineData.frequencyUnit, sparkLineRange, dataCopy, sparkLineData.min, sparkLineData.max)
                groupDataHolder.addSparkLineData(telemetryType, copy)
            } else {
                val groupData = groupDataHolder.getSparkLineData(telemetryType).data
                val data = sparkLineData.data
                for (i in data.indices) {
                    groupData[i] += data[i]
                }
            }
        }

        if (path != GroupHelper.ROOT_GROUP_ID) {
            addGroupData(GroupHelper.getParentHierarchyPath(path), groupIdToDataHolder, groupIdToCount, sparkLineRange, vmDataHolder)
        }
    }

    private fun createGroupVm(hierarchyPath: String, parentType: VmType): VM {
        val groupId = GroupHelper.getParentHierarchyPath(hierarchyPath) ?: ""
        return VM(parentType, 0, 0, GroupHelper.getSimpleName(hierarchyPath), groupId)
    }

    private fun createVmDataHolder(vm: VM, currentTime: Long, random: Random, sparkLineRange: SparkLineRange): VmDataHolder {
        val connectionStatus = connectionStatuses[vm]
        val maxHours = if (connectionStatus == ConnectionStatus.RECENT) 1 else 24 * 3
        val minHours = if (connectionStatus == ConnectionStatus.DISCONNECTED) 1 else 0
        val dataHolder = VmDataHolder(
            vm,
            isConnected = true,
            isOutdatedAgent = false,
            statusChangeTime = currentTime - random.nextInt(maxHours * TimeHelper.MILLISECONDS_TO_HOUR.toInt()) - minHours * TimeHelper.MILLISECONDS_TO_HOUR,
            sparkLineRange = sparkLineRange,
            frequencyUnit = FrequencyUnit.PER_MINUTE,
            hostName = vm.hostName,
            port = 9876
        )
        dataHolder.isConnected = connectionStatus == ConnectionStatus.CONNECTED
        for (telemetryType in telemetryTypes) {
            val sparkLineData = dataHolder.getSparkLineData(telemetryType)
            for (i in 0 until sparkLineRange.numberOfPoints) {
                sparkLineData.data[i] = createDemoData(telemetryType.unit, sparkLineRange, random, i, sparklineFactors[vm]!!, sparkLineIndexRanges[vm])
            }
        }
        return dataHolder
    }

    private fun updateData(dataHolder: VmDataHolder, random: Random) {
        for (telemetryType in telemetryTypes) {
            val sparkLineData = dataHolder.getSparkLineData(telemetryType)
            val value = sparkLineData.currentValue
            val data = sparkLineData.data
            System.arraycopy(data, 1, data, 0, data.size - 1)
            data[data.size - 1] = if (dataHolder.isConnected) getChangedValue(value, random, 3) else 0L
        }
    }

    private fun updateCurrentValues(dataHolder: VmDataHolder, random: Random) {
        for (telemetryType in telemetryTypes) {
            val sparkLineData = dataHolder.getSparkLineData(telemetryType)
            val currentValue = sparkLineData.currentValue
            val data = sparkLineData.data
            data[data.size - 1] = getChangedValue(currentValue, random, 3)
        }
    }

    private fun getChangedValue(value: Long, random: Random, percent: Int): Long =
        (value * (1 + percent * 0.02 * (0.5 - random.nextFloat()))).toLong()

    private fun createDemoData(unit: TelemetryUnit, sparkLineRange: SparkLineRange, random: Random, i: Int, factor: Float, indexRange: IndexRange?): Long =
        if (indexRange == null || (indexRange.startIndex <= i && (indexRange.endIndex == -1 || indexRange.endIndex > i))) {
            (createUnscaledDemoData(unit, sparkLineRange, random, i) * factor).toLong()
        } else {
            0L
        }

    private fun createUnscaledDemoData(unit: TelemetryUnit, sparkLineRange: SparkLineRange, random: Random, i: Int): Long {
        val randomFactor = getRandomFactor(sparkLineRange)
        return when (unit) {
            TelemetryUnit.BYTES -> getRandomValue(random, randomFactor, 600000000) + 10000000L * i
            TelemetryUnit.PERCENT -> 20 + getRandomValue(random, randomFactor, 50) + i
            TelemetryUnit.MICROSECONDS -> getRandomValue(random, randomFactor, 100000) + 10000L * i
            else -> getRandomValue(random, randomFactor, 10) + i
        }
    }

    private fun getRandomValue(random: Random, randomFactor: Float, maximum: Int): Long =
        (randomFactor * random.nextInt(maximum)).toLong()

    private fun getRandomFactor(sparkLineRange: SparkLineRange): Float =
        when (sparkLineRange) {
            SparkLineRange.LAST_HOUR -> 1f
            SparkLineRange.LAST_DAY -> 0.5f
        }

    fun getIdToSparkLineType(): Map<String, TelemetryType> = idToSparklineType

    enum class ConnectionStatus { CONNECTED, RECENT, DISCONNECTED }

    private class IndexRange(val startIndex: Int, val endIndex: Int)

    companion object {
        @Suppress("SpellCheckingInspection")
        private const val LOREM_IPSUM =
            "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet."

        val ID_FIELD: Field

        init {
            try {
                ID_FIELD = AbstractEntity::class.java.getDeclaredField("id")
                ID_FIELD.isAccessible = true
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
    }
}
