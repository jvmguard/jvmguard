package com.jvmguard.rest.restInterface

import com.jvmguard.common.export.TelemetryExport
import com.jvmguard.common.export.TransactionTreeExport
import com.jvmguard.data.transactions.TransactionTreeInterval
import com.jvmguard.data.user.AccessLevel
import com.jvmguard.data.vmdata.TelemetryInterval
import com.jvmguard.rest.entity.GroupEntity
import com.jvmguard.rest.entity.TelemetryDescriptor

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
