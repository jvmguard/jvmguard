package dev.jvmguard.rest.restInterface

import dev.jvmguard.common.export.TelemetryExport
import dev.jvmguard.common.export.TransactionTreeExport
import dev.jvmguard.data.transactions.TransactionTreeInterval
import dev.jvmguard.data.user.AccessLevel
import dev.jvmguard.data.vmdata.TelemetryInterval
import dev.jvmguard.rest.entity.GroupEntity
import dev.jvmguard.rest.entity.TelemetryDescriptor

interface RestInterface {
    fun checkAccess(name: String, apiKey: String): AccessLevel?

    fun getGroups(): List<GroupEntity>
    fun getVms(groupName: String?, connected: Boolean): List<String>

    fun getTelemetryDescriptors(): List<TelemetryDescriptor>
    fun getTelemetry(
        vmName: String?,
        groupName: String?,
        telemetryName: String,
        telemetryInterval: TelemetryInterval,
        endTime: Long
    ): TelemetryExport

    fun getCallTree(
        vmName: String?,
        groupName: String?,
        interval: TransactionTreeInterval,
        startTime: Long,
        mergePolicies: Boolean
    ): TransactionTreeExport

    fun getHotSpots(
        vmName: String?,
        groupName: String?,
        interval: TransactionTreeInterval,
        startTime: Long,
        mergePolicies: Boolean
    ): TransactionTreeExport

    fun getOverdue(
        vmName: String?,
        groupName: String?,
        interval: TransactionTreeInterval,
        startTime: Long
    ): TransactionTreeExport

    fun triggerBackup()
}
