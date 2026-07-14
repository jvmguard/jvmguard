package dev.jvmguard.connector.server.mock

import dev.jvmguard.agent.config.VmType
import dev.jvmguard.agent.config.telemetry.TelemetryUnit
import dev.jvmguard.agent.config.transactions.TransactionType
import dev.jvmguard.agent.tree.AbstractTransactionTree
import dev.jvmguard.common.helper.Direction
import dev.jvmguard.common.transactions.DataAvailability
import dev.jvmguard.common.transactions.TransactionCursorImpl
import dev.jvmguard.data.config.FrequencyUnit
import dev.jvmguard.data.dashboard.Group
import dev.jvmguard.data.file.SnapshotFile
import dev.jvmguard.data.file.SnapshotFileType
import dev.jvmguard.data.transactions.*
import dev.jvmguard.data.user.InboxItem
import dev.jvmguard.data.vmdata.*
import dev.jvmguard.connector.server.mock.snapshot.*
import java.lang.reflect.Field

/**
 * Replays a captured [DemoSnapshot] Falls back to the synthetic mock for anything not captured.
 */
class SnapshotReplay(snapshot: DemoSnapshot) {

    val isValid: Boolean = snapshot.schemaVersion == DemoSnapshot.SCHEMA_VERSION

    val captureAnchor: Long = snapshot.captureAnchor

    private val vmById: Map<Long, VM> = snapshot.vms.associate { it.id to toVm(it) }
    private val leafVms: List<VM> = vmById.values.filter { !it.isGroupNode }
    private val allVmsWithGroups: List<VM> = vmById.values.sorted()

    private val telemetryTypes: List<TelemetryType> = snapshot.telemetryTypes.map { toTelemetryType(it) }
    private val idToTelemetryTypeMap: Map<String, TelemetryType> =
        telemetryTypes.associateBy { it.telemetryIdentifier.combinedId }
    private val customNodes: List<CustomTelemetryNodeIdentifier> =
        snapshot.customTelemetryNodes.map { CustomTelemetryNodeIdentifier(CustomTelemetryNodeIdentifier.Type.valueOf(it.type), it.name) }

    private val sparklines: Map<Long, Map<String, SparkLineHolderDto>> = snapshot.sparklines
    private val telemetrySeries: Map<Long, Map<String, Map<String, TelemetrySeriesDto>>> = snapshot.telemetry
    private val customTelemetrySeries: Map<Long, Map<String, Map<String, TelemetrySeriesDto>>> = snapshot.customTelemetry
    private val transactionBuckets: Map<Long, Map<String, List<TransactionBucketDto>>> = snapshot.transactions

    private val inbox: List<InboxItem> = snapshot.inbox.map { toInboxItem(it) }
    private val snapshotFiles: List<SnapshotFile> = snapshot.snapshotFiles.map { toSnapshotFile(it) }

    fun currentTime(): Long = captureAnchor

    fun namedVms(): Collection<VM> = allVmsWithGroups
    fun connectedVms(): Collection<VM> = leafVms
    fun idToTelemetryType(): Map<String, TelemetryType> = idToTelemetryTypeMap
    fun customTelemetryInfo(): CustomTelemetryInfo = CustomTelemetryInfo(customNodes)

    fun getVmDataHolders(vmFilter: VmFilter, sparkLineRange: SparkLineRange): Group<VmDataHolder> {
        val leafHolders = ArrayList<VmDataHolder>()
        val groupHolders = ArrayList<VmDataHolder>()
        for (vm in allVmsWithGroups) {
            val holder = buildHolder(vm, sparkLineRange) ?: continue
            if (!passesFilter(holder, vmFilter)) {
                continue
            }
            if (vm.isGroupNode) groupHolders.add(holder) else leafHolders.add(holder)
        }
        return assembleGroup(groupHolders, leafHolders)
    }

    fun getVmDataHolder(vm: VM, sparkLineRange: SparkLineRange): VmDataHolder =
        buildHolder(vm, sparkLineRange) ?: emptyHolder(vm, sparkLineRange)

    fun getGroupVmDataHolder(vmIdentifier: VmIdentifier?, sparkLineRange: SparkLineRange): VmDataHolder {
        val target = vmById.values.firstOrNull { it.qualifiedIdentifier == vmIdentifier }
            ?: vmById.values.firstOrNull { it.hierarchyPath == vmIdentifier?.name }
            ?: return emptyHolder(rootGroupVm(), sparkLineRange)
        return getVmDataHolder(target, sparkLineRange)
    }

    private fun passesFilter(holder: VmDataHolder, vmFilter: VmFilter): Boolean = when (vmFilter) {
        VmFilter.CONNECTED -> holder.isConnected
        VmFilter.RECENT -> holder.isConnected || captureAnchor - holder.statusChangeTime < TimeHelper.MILLISECONDS_TO_HOUR
    }

    private fun buildHolder(vm: VM, range: SparkLineRange): VmDataHolder? {
        val holderDto = sparklines[vm.id]?.get(range.name) ?: return null
        val frequencyUnit = runCatching { FrequencyUnit.valueOf(holderDto.frequencyUnit) }.getOrDefault(FrequencyUnit.PER_MINUTE)
        val holder = VmDataHolder(
            vm,
            isConnected = holderDto.isConnected,
            isOutdatedAgent = holderDto.isOutdatedAgent,
            statusChangeTime = holderDto.statusChangeTime,
            sparkLineRange = range,
            frequencyUnit = frequencyUnit,
            hostName = holderDto.hostName,
            port = holderDto.port,
        )
        for (type in telemetryTypes) {
            val series = holderDto.series[type.telemetryIdentifier.combinedId]
            val data = series?.values ?: LongArray(range.numberOfPoints)
            val min = series?.min ?: Long.MIN_VALUE
            val max = series?.max ?: Long.MIN_VALUE
            holder.addSparkLineData(type, SparkLineData(type, frequencyUnit, range, data, min, max))
        }
        return holder
    }

    private fun emptyHolder(vm: VM, range: SparkLineRange): VmDataHolder =
        VmDataHolder(
            vm, isConnected = false, isOutdatedAgent = false, statusChangeTime = 0,
            sparkLineRange = range, frequencyUnit = FrequencyUnit.PER_MINUTE, hostName = vm.hostName, port = vm.port
        )

    private fun assembleGroup(groupHolders: List<VmDataHolder>, leafHolders: List<VmDataHolder>): Group<VmDataHolder> {
        val root = Group<VmDataHolder>()
        for (holder in groupHolders.sortedBy { it.vm.hierarchyPath.length }) {
            val path = holder.vm.hierarchyPath
            if (path.isEmpty()) {
                root.data = holder
                continue
            }
            val names = path.split("/")
            var current = root
            for (i in names.indices) {
                current = current.getOrCreateGroupChild(VmIdentifier(names[i], holder.vm.type.parentType))
                if (i == names.size - 1) current.data = holder
            }
        }
        for (holder in leafHolders) {
            val names = holder.vm.hierarchyPath.split("/")
            var current = root
            for (i in names.indices) {
                if (i < names.size - 1) {
                    current = current.getOrCreateGroupChild(VmIdentifier(names[i], holder.vm.type.parentType))
                } else {
                    current.setVmData(holder.vm, holder)
                }
            }
        }
        return root
    }

    private fun rootGroupVm(): VM = VM(VmType.GROUP, 0, 0, "", "")

    fun getTelemetryData(vm: VM?, mainId: String, interval: TelemetryInterval, endTime: Long): TelemetryData {
        val series = telemetrySeries[vm?.id]?.get(mainId)?.let { byInterval ->
            byInterval[interval.name] ?: byInterval.values.firstOrNull()
        } ?: return emptyTelemetryData(mainId)
        return sliceTelemetry(series, endTime - interval.timeExtent, endTime)
    }

    fun getCustomTelemetryData(vm: VM?, node: CustomTelemetryNodeIdentifier, interval: TelemetryInterval, endTime: Long): TelemetryData {
        val series = customTelemetrySeries[vm?.id]?.get(node.name)?.let { byInterval ->
            byInterval[interval.name] ?: byInterval.values.firstOrNull()
        } ?: return emptyTelemetryData(node.name)
        return sliceTelemetry(series, endTime - interval.timeExtent, endTime)
    }

    private fun emptyTelemetryData(description: String): TelemetryData = TelemetryData().apply {
        rootNode = TelemetryNode(description, false).apply { setTelemetryUnit(TelemetryUnit.PLAIN, 0) }
        timestamps = LongArray(0)
        isNoPreviousData = true
    }

    private fun sliceTelemetry(series: TelemetrySeriesDto, startTime: Long, endTime: Long): TelemetryData {
        val timestamps = series.timestamps
        if (timestamps.isEmpty() || !hasIntersection(timestamps, startTime, endTime)) {
            return emptyTelemetryData(series.root.description)
        }
        val startIdx = (indexOf(timestamps, startTime) - 1).coerceAtLeast(0)
        val endIdx = (indexOf(timestamps, endTime) + 1).coerceIn(0, timestamps.size - 1)
        if (endIdx < startIdx) return emptyTelemetryData(series.root.description)

        val data = TelemetryData()
        data.rootNode = rebuildNode(series.root, startIdx, endIdx)
        data.timestamps = timestamps.copyOfRangeSafe(startIdx, endIdx)
        return data
    }

    private fun rebuildNode(dto: TelemetryNodeDto, startIdx: Int, endIdx: Int): TelemetryNode {
        val node = TelemetryNode(dto.description, dto.stacked)
        node.setTelemetryUnit(runCatching { TelemetryUnit.valueOf(dto.unit) }.getOrDefault(TelemetryUnit.PLAIN), dto.scale)
        for (feed in dto.feeds) {
            node.addData(feed.description, feed.subId, feed.data.copyOfRangeSafe(startIdx, endIdx))
        }
        for (child in dto.children) {
            node.children.add(rebuildNode(child, startIdx, endIdx))
        }
        return node
    }

    private fun hasIntersection(timestamps: LongArray, startTime: Long, endTime: Long): Boolean =
        timestamps.isNotEmpty() && startTime < timestamps.last() && endTime > timestamps.first()

    private fun indexOf(timestamps: LongArray, target: Long): Int {
        for (i in timestamps.indices) {
            if (timestamps[i] >= target) return i
        }
        return timestamps.size
    }

    private fun LongArray.copyOfRangeSafe(from: Int, to: Int): LongArray =
        if (isEmpty() || from > to || from >= size) LongArray(0) else copyOfRange(from.coerceAtLeast(0), (to + 1).coerceAtMost(size))

    fun getTransactionTreeCursor(vm: VM?, interval: TransactionTreeInterval, time: Long, timeRequirement: TimeRequirement): TransactionCursor {
        val buckets = bucketsFor(vm, interval)
        val floorStart = interval.getFloorStartTime(time)
        return when (timeRequirement) {
            TimeRequirement.START_TIME ->
                ReplayCursor(vm?.id, interval, floorStart, containsStartTime(buckets, interval, floorStart), false)

            TimeRequirement.NEAREST_START_TIME -> {
                val nearest = buckets.minByOrNull { kotlin.math.abs(it.startTime - floorStart) }
                ReplayCursor(vm?.id, interval, nearest?.startTime ?: floorStart, nearest?.available ?: false, false)
            }

            TimeRequirement.INCLUDED -> {
                val bucket = buckets.firstOrNull { time >= it.startTime && time < it.startTime + interval.timeExtent }
                ReplayCursor(vm?.id, interval, bucket?.startTime ?: floorStart, bucket?.available ?: false, bucket?.latest ?: false)
            }
        }
    }

    fun getCurrentTransactionTreeCursor(vm: VM?, interval: TransactionTreeInterval): TransactionCursor {
        val buckets = bucketsFor(vm, interval)
        val newest = buckets.firstOrNull() ?: return ReplayCursor(vm?.id, interval, interval.getFloorStartTime(captureAnchor), false, latest = true)
        return ReplayCursor(vm?.id, interval, newest.startTime, newest.available, true)
    }

    fun changeTransactionCursor(cursor: TransactionCursor, interval: TransactionTreeInterval): TransactionCursor =
        if (cursor.isLatest) getCurrentTransactionTreeCursor(cursor.vm, interval)
        else getTransactionTreeCursor(cursor.vm, interval, cursor.startTime, TimeRequirement.INCLUDED)

    fun moveTransactionTreeCursor(cursor: TransactionCursor, direction: Direction): TransactionCursor {
        val newStart = cursor.startTime + direction.factor * cursor.interval.timeExtent
        return getTransactionTreeCursor(cursor.vm, cursor.interval, newStart, TimeRequirement.START_TIME)
    }

    fun getCallTree(cursor: TransactionCursor): TransactionTreeData = treeData(cursor, hotspots = false) { it.callTree }
    fun getHotspots(cursor: TransactionCursor): TransactionTreeData = treeData(cursor, hotspots = true) { it.hotspots }

    private fun treeData(cursor: TransactionCursor, hotspots: Boolean, selector: (TransactionBucketDto) -> TransactionTreeDto?): TransactionTreeData {
        val replayCursor = cursor as? ReplayCursor
            ?: ReplayCursor(cursor.vm?.id, cursor.interval, cursor.startTime, cursor.availability.isAvailable, cursor.isLatest)
        val bucket = bucketsFor(replayCursor.vmId?.let { vmById[it] }, replayCursor.interval)
            .firstOrNull { it.startTime == replayCursor.startTime }
        val treeDto = bucket?.let(selector)
        val tree = if (treeDto != null) rebuildTree(treeDto) else TransactionTree(null, TransactionType.MATCHED, null)
        val cursorImpl = TransactionCursorImpl().apply {
            availability = if (replayCursor.available) DataAvailability.TRUE else DataAvailability.FALSE
            isLatest = replayCursor.latest
            startTime = replayCursor.startTime
            interval = replayCursor.interval
            vm = replayCursor.vmId?.let { vmById[it] }
        }
        return TransactionTreeData(
            cursorImpl,
            LongArray(0),
            bucket?.minIntervalPercentage ?: 0,
            bucket?.maxIntervalPercentage ?: 0,
            tree,
            hotspots,
            false,
        )
    }

    private fun bucketsFor(vm: VM?, interval: TransactionTreeInterval): List<TransactionBucketDto> =
        transactionBuckets[vm?.id]?.get(interval.name) ?: emptyList()

    private fun containsStartTime(buckets: List<TransactionBucketDto>, interval: TransactionTreeInterval, time: Long): Boolean =
        buckets.any { time >= it.startTime && time < it.startTime + interval.timeExtent }

    fun inboxItems(): Collection<InboxItem> = inbox
    fun snapshotFiles(type: SnapshotFileType?, vm: VM?): Collection<SnapshotFile> =
        snapshotFiles.filter { (type == null || it.type == type) && (vm == null || it.vm == vm) }

    fun snapshotFile(id: Long): SnapshotFile? = snapshotFiles.firstOrNull { it.id == id }

    private fun toVm(dto: VmDto): VM {
        val type = runCatching { VmType.valueOf(dto.type) }.getOrDefault(VmType.NAMED)
        return VM(type, dto.id, dto.instanceId, dto.rawName, dto.groupName, dto.hostName, dto.port)
    }

    private fun toTelemetryType(dto: TelemetryTypeDto): TelemetryType = TelemetryType(
        dto.mainId, dto.subId, dto.additionalType, dto.additionalName, dto.categoryName, dto.name,
        runCatching { TelemetryUnit.valueOf(dto.unit) }.getOrDefault(TelemetryUnit.PLAIN), dto.scale,
    )

    private fun toInboxItem(dto: InboxItemDto): InboxItem = InboxItem(
        dto.id, dto.millis, dto.snapshotFileId,
        dto.snapshotFileType?.let { runCatching { SnapshotFileType.valueOf(it) }.getOrNull() },
        dto.vmId.takeIf { it != 0L }?.let { vmById[it] },
        dto.name, dto.message, dto.itemRead,
    )

    private fun toSnapshotFile(dto: SnapshotFileDto): SnapshotFile {
        val vm = vmById[dto.vmId] ?: rootGroupVm()
        val file = SnapshotFile(
            dto.id, vm, runCatching { SnapshotFileType.valueOf(dto.type) }.getOrDefault(SnapshotFileType.JPS),
            dto.snapshotTime, dto.name
        )
        if (dto.uncompressedLength >= 0) file.updateUncompressedLength(dto.uncompressedLength)
        return file
    }

    private fun rebuildTree(dto: TransactionTreeDto): TransactionTree {
        val root = TransactionTree(dto.name, TransactionType.valueOf(dto.transactionType), dto.policyTypeString)
        rebuildChildren(root, dto)
        setTimeCount(root, dto.time, dto.count)
        return root
    }

    private fun rebuildChildren(parent: TransactionTree, dto: TransactionTreeDto) {
        for (childDto in dto.children) {
            val child = TransactionTree(parent, childDto.name, TransactionType.valueOf(childDto.transactionType), childDto.policyTypeString)
            parent.add(child)
            rebuildChildren(child, childDto)
            setTimeCount(child, childDto.time, childDto.count)
        }
    }

    private fun setTimeCount(node: TransactionTree, time: Long, count: Long) {
        TIME_FIELD.set(node, time)
        COUNT_FIELD.set(node, count)
    }

    private class ReplayCursor(
        val vmId: Long?,
        override val interval: TransactionTreeInterval,
        override val startTime: Long,
        val available: Boolean,
        val latest: Boolean,
    ) : TransactionCursor {
        override val availability: DataAvailability get() = if (available) DataAvailability.TRUE else DataAvailability.FALSE
        override val isLatest: Boolean = latest
        override val vm: VM? = null
        override val gap: Long = 0
    }

    companion object {
        private val TIME_FIELD: Field = AbstractTransactionTree::class.java.getDeclaredField("time").apply { isAccessible = true }
        private val COUNT_FIELD: Field = AbstractTransactionTree::class.java.getDeclaredField("count").apply { isAccessible = true }
    }
}
