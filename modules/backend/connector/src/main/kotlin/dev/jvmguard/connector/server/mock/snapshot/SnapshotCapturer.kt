package dev.jvmguard.connector.server.mock.snapshot

import dev.jvmguard.data.dashboard.Group
import dev.jvmguard.data.transactions.*
import dev.jvmguard.data.vmdata.*
import dev.jvmguard.connector.api.ServerConnection

// Invoked by the captureMockSnapshot control command
class SnapshotCapturer(private val connection: ServerConnection, private val bucketsPerInterval: Int = 8) {

    fun capture(): DemoSnapshot {
        val snapshot = DemoSnapshot()
        val anchor = connection.currentTime
        snapshot.captureAnchor = anchor

        val types = connection.idToTelemetryType.values.toList()
        snapshot.vms.addAll(connection.namedVms.map { captureVm(it) })
        snapshot.telemetryTypes.addAll(types.map { captureTelemetryType(it) })
        snapshot.customTelemetryNodes.addAll(
            connection.customTelemetryInfo.customTelemetryNodeIdentifiers.map { CustomTelemetryNodeDto(it.type.name, it.name) }
        )

        captureSparklines(snapshot, types)
        for (vm in leafVms()) {
            captureTelemetry(snapshot, vm, anchor)
            captureTransactions(snapshot, vm, anchor)
        }
        snapshot.inbox.addAll(connection.inboxItems.map { captureInbox(it) })
        snapshot.snapshotFiles.addAll(connection.getSnapshotFiles(null, null).map { captureSnapshotFile(it) })
        return snapshot
    }

    private fun captureSparklines(snapshot: DemoSnapshot, types: Collection<TelemetryType>) {
        for (range in SparkLineRange.entries) {
            val root = connection.getVmDataHolders(VmFilter.CONNECTED, range, types)
            walkGroup(root) { holder -> captureSparkline(snapshot, holder, range) }
        }
    }

    private fun walkGroup(group: Group<VmDataHolder>, visit: (VmDataHolder) -> Unit) {
        group.data?.let(visit)
        for (child in group.groupChildren.values) {
            walkGroup(child, visit)
        }
        for (holder in group.vmDataMap.values) {
            visit(holder)
        }
    }

    private fun captureSparkline(snapshot: DemoSnapshot, holder: VmDataHolder, range: SparkLineRange) {
        val dto = SparkLineHolderDto()
        dto.isConnected = holder.isConnected
        dto.isOutdatedAgent = holder.isOutdatedAgent
        dto.statusChangeTime = holder.statusChangeTime
        dto.hostName = holder.hostName
        dto.port = holder.port
        dto.frequencyUnit = holder.frequencyUnit.name
        for (type in snapshot.telemetryTypes) {
            val combinedId = combinedId(type)
            val spark = runCatching { holder.getSparkLineData(toTelemetryType(type)) }.getOrNull() ?: continue
            dto.series[combinedId] = SparkLineSeriesDto(spark.data.copyOf(), spark.min, spark.max)
        }
        snapshot.sparklines.getOrPut(holder.vm.id) { mutableMapOf() }[range.name] = dto
    }

    private fun captureTelemetry(snapshot: DemoSnapshot, vm: VM, anchor: Long) {
        for (telemetry in Telemetry.entries) {
            for (interval in TelemetryInterval.entries) {
                val data = connection.getTelemetryData(vm, telemetry.mainId, interval, anchor)
                if ((data.timestamps?.size ?: 0) == 0) {
                    continue
                }
                snapshot.telemetry.getOrPut(vm.id) { mutableMapOf() }
                    .getOrPut(telemetry.mainId) { mutableMapOf() }[interval.name] = captureSeries(data)
            }
        }
        for (node in connection.customTelemetryInfo.customTelemetryNodeIdentifiers) {
            for (interval in TelemetryInterval.entries) {
                val data = connection.getCustomTelemetryData(vm, node, interval, anchor)
                if ((data.timestamps?.size ?: 0) == 0) {
                    continue
                }
                snapshot.customTelemetry.getOrPut(vm.id) { mutableMapOf() }
                    .getOrPut(node.name) { mutableMapOf() }[interval.name] = captureSeries(data)
            }
        }
    }

    private fun captureSeries(data: TelemetryData): TelemetrySeriesDto {
        val dto = TelemetrySeriesDto()
        dto.timestamps = data.timestamps?.copyOf() ?: LongArray(0)
        data.rootNode?.let { dto.root = captureNode(it) }
        return dto
    }

    private fun captureNode(node: TelemetryNode): TelemetryNodeDto {
        val dto = TelemetryNodeDto()
        dto.description = node.description
        dto.stacked = node.isStackedData
        dto.unit = node.telemetryUnit.name
        dto.scale = node.scale
        for (feed in node.data) {
            dto.feeds.add(TelemetryFeedDto(feed.description, feed.subId, feed.data?.copyOf() ?: LongArray(0)))
        }
        for (child in node.children) {
            dto.children.add(captureNode(child))
        }
        return dto
    }

    private fun captureTransactions(snapshot: DemoSnapshot, vm: VM, anchor: Long) {
        for (interval in TransactionTreeInterval.entries) {
            val buckets = ArrayList<TransactionBucketDto>()
            for (i in 0 until bucketsPerInterval) {
                val startTime = interval.getFloorStartTime(anchor - i * interval.timeExtent)
                val cursor = connection.getTransactionTreeCursor(vm, interval, TransactionDataType.TRANSACTION, startTime, TimeRequirement.START_TIME)
                val callTree = connection.getCallTree(cursor, false)
                val hotspots = connection.getHotspots(cursor, false)
                buckets.add(captureBucket(cursor, interval, i == 0, callTree, hotspots))
            }
            snapshot.transactions.getOrPut(vm.id) { mutableMapOf() }[interval.name] = buckets
        }
    }

    private fun captureBucket(
        cursor: TransactionCursor, interval: TransactionTreeInterval, latest: Boolean,
        callTree: TransactionTreeData, hotspots: TransactionTreeData
    ): TransactionBucketDto {
        val dto = TransactionBucketDto()
        dto.startTime = cursor.startTime
        dto.interval = interval.name
        dto.latest = latest
        dto.available = cursor.availability.isAvailable
        dto.minIntervalPercentage = callTree.minIntervalPercentage
        dto.maxIntervalPercentage = callTree.maxIntervalPercentage
        dto.callTree = captureTree(callTree.transactionTree)
        dto.hotspots = captureTree(hotspots.transactionTree)
        return dto
    }

    private fun captureTree(tree: TransactionTree): TransactionTreeDto {
        val dto = TransactionTreeDto()
        dto.name = tree.getName()
        dto.transactionType = tree.getTransactionType().name
        dto.policyTypeString = tree.getPolicyTypeString() ?: ""
        dto.time = tree.getTime()
        dto.count = tree.getCount()
        val children = tree.children()
        if (children != null) {
            for (child in children) {
                dto.children.add(captureTree(child))
            }
        }
        return dto
    }

    private fun leafVms(): List<VM> = connection.namedVms.filter { !it.isGroupNode }

    private fun captureVm(vm: VM): VmDto = VmDto(
        vm.id, vm.type.name, vm.rawName, vm.groupName, vm.hostName, vm.port, vm.instanceId
    )

    private fun captureTelemetryType(type: TelemetryType): TelemetryTypeDto {
        val id = type.telemetryIdentifier
        return TelemetryTypeDto(
            id.mainId, id.subId, id.additionalType, id.additionalName,
            type.categoryName, type.name, type.unit.name, type.scale
        )
    }

    private fun captureInbox(item: dev.jvmguard.data.user.InboxItem): InboxItemDto = InboxItemDto().apply {
        id = item.id ?: 0L
        millis = item.date.toEpochMilli()
        snapshotFileId = item.snapshotFileId
        snapshotFileType = item.snapshotFileType?.name
        vmId = item.vm?.id ?: 0L
        name = item.name
        message = item.message
        itemRead = item.isItemRead
    }

    private fun captureSnapshotFile(file: dev.jvmguard.data.file.SnapshotFile): SnapshotFileDto = SnapshotFileDto().apply {
        id = file.id ?: 0L
        vmId = file.vm.id
        type = file.type.name
        snapshotTime = file.dateCreated.toEpochMilli()
        name = file.name
        uncompressedLength = file.uncompressedLength
    }

    private fun combinedId(dto: TelemetryTypeDto): String =
        PersistentTelemetryIdentifier(dto.mainId, dto.subId, dto.additionalType, dto.additionalName).combinedId

    private fun toTelemetryType(dto: TelemetryTypeDto): TelemetryType = TelemetryType(
        dto.mainId, dto.subId, dto.additionalType, dto.additionalName, dto.categoryName, dto.name,
        runCatching { dev.jvmguard.agent.config.telemetry.TelemetryUnit.valueOf(dto.unit) }
            .getOrDefault(dev.jvmguard.agent.config.telemetry.TelemetryUnit.PLAIN), dto.scale)

}
