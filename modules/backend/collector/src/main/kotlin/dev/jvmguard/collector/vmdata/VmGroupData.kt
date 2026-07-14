package dev.jvmguard.collector.vmdata

import dev.jvmguard.agent.config.VmType
import dev.jvmguard.collector.main.CollectorContext
import dev.jvmguard.collector.main.VmManagerImpl
import dev.jvmguard.collector.telemetry.TelemetryDataInterval
import dev.jvmguard.collector.threshold.ViolationEvent
import dev.jvmguard.collector.vmdata.components.GroupTransactionData
import dev.jvmguard.collector.vmdata.components.GroupTriggerHandler
import dev.jvmguard.data.base.Interval
import dev.jvmguard.data.config.FrequencyUnit
import dev.jvmguard.data.dashboard.Group
import dev.jvmguard.data.vmdata.*
import java.util.*

class VmGroupData(
    parent: VmGroupData?,
    vmIdentifier: VmIdentifier,
    collectorContext: CollectorContext,
) : AbstractVmData(
    collectorContext,
    parent,
    collectorContext.getGroupVM(vmIdentifier.toQualified(parent?.vm?.hierarchyPath ?: "")),
) {

    // single Object initialized in the root group to avoid locking bugs
    private val structureLock: Any = parent?.structureLock ?: Any()

    private val groupChildren = HashMap<VmIdentifier, VmGroupData>()
    private val vmChildren = HashMap<VM, VmData>()

    private val connectedGroupChildren = HashMap<VmIdentifier, VmGroupData>()
    private val connectedVmChildren = HashMap<VM, VmData>()

    // will be locked nested started from the vm to the root group and use structureLock via isComplete. ATTENTION: do not access otherwise
    val transactionData = GroupTransactionData(this)
    val triggerHandler = GroupTriggerHandler(this)

    private var lastDisconnectNanos = System.nanoTime() - Interval.DAY.nanos * 2

    fun disconnectGroupChild(groupData: VmGroupData) {
        synchronized(structureLock) {
            val vmIdentifier = groupData.vm.unqualifiedIdentifier
            if (connectedGroupChildren[vmIdentifier] === groupData) {
                connectedGroupChildren.remove(vmIdentifier)
                checkDisconnected()
            }
        }
    }

    fun disconnectVmChild(vmData: VmData) {
        synchronized(structureLock) {
            if (connectedVmChildren[vmData.vm] === vmData) {
                connectedVmChildren.remove(vmData.vm)
                checkDisconnected()
            }
        }
    }

    private fun checkDisconnected() {
        if (connectedVmChildren.isEmpty() && connectedGroupChildren.isEmpty()) {
            parent?.disconnectGroupChild(this)
            lastDisconnectNanos = System.nanoTime()
        }
    }

    fun visitConnectedVmTelemetryValue(telemetryIdentifier: TelemetryIdentifier, visitor: TelemetryValueVisitor) {
        synchronized(structureLock) {
            for (vmData in connectedVmChildren.values) {
                vmData.visitConnectedVmTelemetryValue(telemetryIdentifier, visitor)
            }
            for (vmGroupData in connectedGroupChildren.values) {
                vmGroupData.visitConnectedVmTelemetryValue(telemetryIdentifier, visitor)
            }
        }
    }

    override fun visitTelemetryData(visitor: TelemetryDataVisitor, dataInterval: TelemetryDataInterval): Boolean {
        var childStored = false
        synchronized(structureLock) {
            for (vmData in vmChildren.values) {
                if (vmData.visitTelemetryData(visitor, dataInterval)) {
                    childStored = true
                }
            }
            for (vmGroupData in groupChildren.values) {
                if (vmGroupData.visitTelemetryData(visitor, dataInterval)) {
                    childStored = true
                }
            }
        }
        if (childStored) {
            return super.visitTelemetryData(visitor, dataInterval)
        }
        return false
    }

    override fun addViolation(snapshotTimeStamp: Long, nanoTime: Long, violationEvent: ViolationEvent) {
        super.addViolation(snapshotTimeStamp, nanoTime, violationEvent)
        triggerHandler.addViolation(snapshotTimeStamp, nanoTime, violationEvent, vm, collectorContext)
    }

    fun updateTriggerFrequency(snapshotTimeStamp: Long, nanoTime: Long) {
        triggerHandler.updateTriggerFrequency(snapshotTimeStamp, nanoTime, collectorContext)

        val groupDatas = synchronized(structureLock) { ArrayList(groupChildren.values) }
        for (vmGroupData in groupDatas) {
            vmGroupData.updateTriggerFrequency(snapshotTimeStamp, nanoTime)
        }
    }

    fun commitTelemetryData(nanoTime: Long, recordingTime: Long, recordingInterval: TelemetryDataInterval): Int {
        var connectedChildren = 0
        synchronized(structureLock) {
            for (vmData in vmChildren.values) {
                connectedChildren += vmData.commitTelemetryData(nanoTime, recordingTime, recordingInterval)
            }
            for (vmGroupData in groupChildren.values) {
                connectedChildren += vmGroupData.commitTelemetryData(nanoTime, recordingTime, recordingInterval)
            }
        }
        commitOwnData(connectedChildren, nanoTime, recordingTime, recordingInterval, true)
        triggerHandler.checkConnectionTrigger(nanoTime, connectedChildren, collectorContext)
        return connectedChildren
    }

    fun connectVm(groupNames: Array<String>, connection: Connection, outdatedAgent: Boolean, nanoTime: Long, hostName: String, port: Int): VmData {
        return connectVm(ArrayDeque(groupNames.toList()), connection, outdatedAgent, nanoTime, hostName, port)
    }

    private fun connectVm(groupNames: Deque<String>, connection: Connection, outdatedAgent: Boolean, nanoTime: Long, hostName: String, port: Int): VmData {
        if (groupNames.isEmpty()) {
            synchronized(structureLock) {
                val result = vmChildren.computeIfAbsent(connection.vm) { _ -> VmData(collectorContext, this, connection.vm) }
                connectedVmChildren[connection.vm] = result
                result.connect(nanoTime, outdatedAgent, hostName, port)
                return result
            }
        } else {
            synchronized(structureLock) {
                val vmIdentifier = popGroupIdentifier(groupNames, connection.vm.type.parentType)
                val child = groupChildren.computeIfAbsent(vmIdentifier) { i -> VmGroupData(this, i, collectorContext) }
                connectedGroupChildren[vmIdentifier] = child
                return child.connectVm(groupNames, connection, outdatedAgent, nanoTime, hostName, port)
            }
        }
    }

    fun clearDisconnected(nanoTime: Long): Boolean {
        var hasTransitiveVm = false
        synchronized(structureLock) {
            val iterator = vmChildren.values.iterator()
            while (iterator.hasNext()) {
                val vmData = iterator.next()
                if (vmData.clearDisconnected(nanoTime)) {
                    connectedVmChildren.remove(vmData.vm)
                    iterator.remove()
                } else {
                    hasTransitiveVm = true
                }
            }
            for (vmGroupData in groupChildren.values) {
                if (vmGroupData.clearDisconnected(nanoTime)) {
                    connectedGroupChildren.remove(vmGroupData.vm.unqualifiedIdentifier)
                } else {
                    hasTransitiveVm = true
                }
            }
        }
        if (!hasTransitiveVm) {
            freeTelemetryData()
        }
        return !hasTransitiveVm
    }

    override fun getVmDataHolder(
        vmFilter: VmFilter,
        sparkLineRange: SparkLineRange,
        telemetryTypes: Collection<TelemetryType>,
        nanoTime: Long,
        timeStamp: Long,
        frequencyUnit: FrequencyUnit,
    ): VmDataHolder {
        val vmDataHolder = VmDataHolder(vm,
            isConnected = true,
            isOutdatedAgent = false,
            statusChangeTime = 0,
            sparkLineRange = sparkLineRange,
            frequencyUnit = frequencyUnit,
            hostName = "",
            port = 0
        )
        addSparklineData(sparkLineRange, telemetryTypes, nanoTime, vmDataHolder, frequencyUnit)
        return vmDataHolder
    }

    fun addAllVmDataHolders(
        group: Group<VmDataHolder>,
        vmFilter: VmFilter,
        sparkLineRange: SparkLineRange,
        telemetryTypes: Collection<TelemetryType>,
        nanoTime: Long,
        timeStamp: Long,
        frequencyUnit: FrequencyUnit,
    ) {
        val vmDatas = synchronized(structureLock) { ArrayList(vmChildren.values) }
        for (vmData in vmDatas) {
            val vmDataHolder = vmData.getVmDataHolder(vmFilter, sparkLineRange, telemetryTypes, nanoTime, timeStamp, frequencyUnit)
            if (vmDataHolder != null) {
                group.setVmData(vmData.vm, vmDataHolder)
            }
        }

        val groupDatas = synchronized(structureLock) { ArrayList(groupChildren.values) }
        for (vmGroupData in groupDatas) {
            val subGroupIdentifier = vmGroupData.vm.qualifiedIdentifier

            val childGroup = group.getOrCreateGroupChild(subGroupIdentifier)
            vmGroupData.addAllVmDataHolders(childGroup, vmFilter, sparkLineRange, telemetryTypes, nanoTime, timeStamp, frequencyUnit)
            if (childGroup.isEmpty) {
                group.removeGroupChild(subGroupIdentifier)
            }
        }
        if (!group.isEmpty) {
            group.data = getVmDataHolder(vmFilter, sparkLineRange, telemetryTypes, nanoTime, timeStamp, frequencyUnit)
        }
    }

    fun disconnectVm(groupNames: Deque<String>, vm: VM, nanoTime: Long) {
        if (groupNames.isEmpty()) {
            synchronized(structureLock) {
                vmChildren[vm]?.disconnect(nanoTime)
            }
        } else {
            synchronized(structureLock) {
                val child = groupChildren[popGroupIdentifier(groupNames, vm.type.parentType)]
                child?.disconnectVm(groupNames, vm, nanoTime)
            }
        }
    }

    fun getGroupData(vmIdentifier: VmIdentifier?, collectorContext: CollectorContext?): VmGroupData? {
        return if (vmIdentifier == null) {
            this
        } else {
            getGroupData(ArrayDeque(VmManagerImpl.getGroupNames(vmIdentifier.name).toList()), vmIdentifier.type, collectorContext)
        }
    }

    private fun getGroupData(groupNames: Deque<String>, groupType: VmType, collectorContext: CollectorContext?): VmGroupData? {
        if (groupNames.isEmpty()) {
            return this
        }
        val child: VmGroupData
        synchronized(structureLock) {
            val vmIdentifier = popGroupIdentifier(groupNames, groupType)
            var existing = groupChildren[vmIdentifier]
            if (existing == null) {
                if (collectorContext == null) {
                    return null
                } else {
                    existing = VmGroupData(this, vmIdentifier, collectorContext)
                    groupChildren[vmIdentifier] = existing
                }
            }
            child = existing
        }
        return child.getGroupData(groupNames, groupType, collectorContext)
    }

    fun getVmData(vm: VM?): AbstractVmData? {
        return when {
            vm == null -> this
            vm.isGroupNode -> getGroupData(vm.qualifiedIdentifier, null)
            else -> getGroupData(vm.parentIdentifier, null)?.getVmChild(vm)
        }
    }

    fun createVmChild(vm: VM, lastStateNanoTime: Long): VmData {
        synchronized(structureLock) {
            return vmChildren.computeIfAbsent(vm) { v -> VmData(collectorContext, this, v, lastStateNanoTime) }
        }
    }

    private fun getVmChild(vm: VM): VmData? {
        synchronized(structureLock) {
            return vmChildren[vm]
        }
    }

    fun isComplete(arrivedVms: Set<VM>, arrivedGroups: Set<VmIdentifier>): Boolean {
        synchronized(structureLock) {
            return arrivedVms.size >= connectedVmChildren.size &&
                    arrivedGroups.size >= connectedGroupChildren.size &&
                    arrivedVms.containsAll(connectedVmChildren.keys) &&
                    arrivedGroups == connectedGroupChildren.keys
        }
    }

    fun getAllGroupTransactionDataDepthFirst(): Collection<GroupTransactionData> {
        val result = ArrayList<GroupTransactionData>()
        synchronized(structureLock) {
            addGroupTransactionDataDepthFirst(result)
        }
        return result
    }

    private fun addGroupTransactionDataDepthFirst(result: MutableCollection<GroupTransactionData>) {
        for (groupData in groupChildren.values) {
            groupData.addGroupTransactionDataDepthFirst(result)
        }
        result.add(transactionData)
    }

    fun setMinDisconnectedTime(minValue: Long) {
        lastDisconnectNanos = maxOf(lastDisconnectNanos, minValue)
        parent?.setMinDisconnectedTime(minValue)
    }

    fun getConnectedVms(): Collection<VM> {
        synchronized(structureLock) {
            return ArrayList(connectedVmChildren.keys)
        }
    }

    companion object {
        private fun popGroupIdentifier(groupNames: Deque<String>, groupType: VmType): VmIdentifier {
            val groupName = groupNames.pop()
            return VmIdentifier(groupName, if (groupNames.isEmpty()) groupType else VmType.GROUP)
        }
    }
}
