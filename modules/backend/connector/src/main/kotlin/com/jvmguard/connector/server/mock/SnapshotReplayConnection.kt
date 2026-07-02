package com.jvmguard.connector.server.mock

import com.jvmguard.data.dashboard.Group
import com.jvmguard.data.file.SnapshotFile
import com.jvmguard.data.file.SnapshotFileType
import com.jvmguard.data.transactions.*
import com.jvmguard.data.user.InboxItem
import com.jvmguard.data.user.User
import com.jvmguard.data.vmdata.*
import com.jvmguard.connector.server.mock.snapshot.DemoSnapshot
import com.jvmguard.connector.server.mock.snapshot.SnapshotLoader
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

// Selected by `?mock=demo`
@Component
@Scope("prototype")
class SnapshotReplayConnection(@Suppress("SpringJavaInjectionPointsAutowiringInspection") user: User) : MockServerConnectionImpl(user) {

    private val replay: SnapshotReplay = SnapshotReplay(SNAPSHOT ?: error("No demo snapshot configured"))

    init {
        check(replay.isValid) { "Demo snapshot schema version mismatch" }
    }

    // Sourced from the companion so it is valid even when called during super-construction.
    override val currentTime: Long
        get() = CAPTURE_ANCHOR

    override val namedVms: Collection<VM>
        get() = replay.namedVms()

    override val connectedVms: Collection<VM>
        get() = replay.connectedVms()

    override val idToTelemetryType: Map<String, TelemetryType>
        get() = replay.idToTelemetryType()

    override val customTelemetryInfo: CustomTelemetryInfo
        get() = replay.customTelemetryInfo()

    override fun getVmDataHolders(
        vmFilter: VmFilter,
        sparkLineRange: SparkLineRange,
        telemetryTypes: Collection<TelemetryType>,
    ): Group<VmDataHolder> = replay.getVmDataHolders(vmFilter, sparkLineRange)

    override fun getVmDataHolder(vm: VM, sparkLineRange: SparkLineRange, telemetryTypes: Collection<TelemetryType>): VmDataHolder =
        replay.getVmDataHolder(vm, sparkLineRange)

    override fun getGroupVmDataHolder(
        vmIdentifier: VmIdentifier?,
        sparkLineRange: SparkLineRange,
        telemetryTypes: Collection<TelemetryType>,
    ): VmDataHolder = replay.getGroupVmDataHolder(vmIdentifier, sparkLineRange)

    override fun getTelemetryData(vm: VM?, mainId: String, interval: TelemetryInterval, endTime: Long): TelemetryData =
        replay.getTelemetryData(vm, mainId, interval, endTime)

    override fun getCustomTelemetryData(
        vm: VM?,
        nodeIdentifier: CustomTelemetryNodeIdentifier,
        interval: TelemetryInterval,
        endTime: Long,
    ): TelemetryData = replay.getCustomTelemetryData(vm, nodeIdentifier, interval, endTime)

    override fun getTransactionTreeCursor(
        vm: VM?,
        interval: TransactionTreeInterval,
        transactionDataType: TransactionDataType,
        time: Long,
        timeRequirement: TimeRequirement,
    ): TransactionCursor = replay.getTransactionTreeCursor(vm, interval, time, timeRequirement)

    override fun getCurrentTransactionTreeCursor(vm: VM?, interval: TransactionTreeInterval, transactionDataType: TransactionDataType): TransactionCursor =
        replay.getCurrentTransactionTreeCursor(vm, interval)

    override fun changeTransactionCursor(transactionCursor: TransactionCursor, vm: VM?, interval: TransactionTreeInterval): TransactionCursor =
        replay.changeTransactionCursor(transactionCursor, interval)

    override fun moveTransactionTreeCursor(cursor: TransactionCursor, direction: com.jvmguard.common.helper.Direction): TransactionCursor =
        replay.moveTransactionTreeCursor(cursor, direction)

    override fun getCallTree(transactionCursor: TransactionCursor, mergePolicies: Boolean): TransactionTreeData =
        replay.getCallTree(transactionCursor)

    override fun getHotspots(transactionCursor: TransactionCursor, mergePolicies: Boolean): TransactionTreeData =
        replay.getHotspots(transactionCursor)

    override val inboxItems: Collection<InboxItem>
        get() = replay.inboxItems()

    override fun getSnapshotFiles(snapshotFileType: SnapshotFileType?, vm: VM?): Collection<SnapshotFile> =
        replay.snapshotFiles(snapshotFileType, vm)

    override fun getSnapshotFile(id: Long): SnapshotFile? = replay.snapshotFile(id)

    companion object {

        // Loaded statically so currentTime can read CAPTURE_ANCHOR during super-construction
        private val SNAPSHOT: DemoSnapshot? = SnapshotLoader.load()
        private val CAPTURE_ANCHOR: Long = SNAPSHOT?.captureAnchor ?: System.currentTimeMillis()

        fun isAvailable(): Boolean = SNAPSHOT != null
    }
}
