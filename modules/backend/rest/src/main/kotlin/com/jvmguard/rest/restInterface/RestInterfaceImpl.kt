package com.jvmguard.rest.restInterface

import com.jvmguard.agent.config.VmType
import com.jvmguard.collector.api.TransactionProvider
import com.jvmguard.collector.api.VmManager
import com.jvmguard.collector.util.BackupHandler
import com.jvmguard.collector.util.BackupHandler.BackupException
import com.jvmguard.common.export.TelemetryExport
import com.jvmguard.common.export.TransactionTreeExport
import com.jvmguard.common.export.TransactionTreeExport.DataType
import com.jvmguard.common.helper.PasswordHelper
import com.jvmguard.data.transactions.TimeRequirement
import com.jvmguard.data.transactions.TransactionDataType
import com.jvmguard.data.transactions.TransactionTreeInterval
import com.jvmguard.data.user.AccessLevel
import com.jvmguard.data.user.UserManager
import com.jvmguard.data.vmdata.TelemetryInterval
import com.jvmguard.data.vmdata.VM
import com.jvmguard.data.vmdata.VmIdentifier
import com.jvmguard.rest.RestHelper
import com.jvmguard.rest.entity.GroupEntity
import com.jvmguard.rest.entity.TelemetryDescriptor
import com.jvmguard.rest.provider.RestException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

@Component
class RestInterfaceImpl(
    private val userManager: UserManager,
    private val vmManager: VmManager,
    private val restTelemetryProvider: RestTelemetryProvider,
    private val transactionProvider: TransactionProvider,
    private val backupHandler: BackupHandler
) : RestInterface {

    override fun checkAccess(name: String, apiKey: String): AccessLevel? {
        val user = userManager.getByLoginName(name) ?: return null
        return if (user.apiKeyHash.isNotEmpty() && PasswordHelper.validatePassword(apiKey, user.apiKeyHash)) {
            user.accessLevel
        } else {
            null
        }
    }

    override fun getGroups(): List<GroupEntity> {
        val ret = ArrayList<GroupEntity>()
        for (vm in vmManager.namedVms) {
            val qualifiedIdentifier = vm.qualifiedIdentifier
            if (vm.isGroupNode && qualifiedIdentifier != VmIdentifier.ROOT_GROUP_IDENTIFIER) {
                ret.add(GroupEntity(qualifiedIdentifier.toString(), qualifiedIdentifier.type == VmType.POOL))
            }
        }
        ret.sortBy { it.name }
        return ret
    }

    override fun getVms(groupName: String?, connected: Boolean): List<String> {
        val group = getGroup(groupName)
        val groupIdentifier = group.qualifiedIdentifier

        val ret = ArrayList<String>()
        when {
            group.type == VmType.POOL -> {
                for (vm in vmManager.getConnectedPooledVms(group)) {
                    ret.add(vm.hierarchyPath + vm.formattedInstanceId)
                }
            }

            connected -> {
                for (connection in vmManager.currentConnections) {
                    if (connection.vm.type == VmType.NAMED && connection.vm.isIncluded(groupIdentifier)) {
                        ret.add(connection.vm.hierarchyPath)
                    }
                }
            }

            else -> {
                for (vm in vmManager.namedVms) {
                    if (!vm.isGroupNode && vm.isIncluded(groupIdentifier)) {
                        ret.add(vm.hierarchyPath)
                    }
                }
            }
        }
        ret.sort()
        return ret
    }

    private fun getGroup(groupName: String?): VM {
        val group = if (groupName.isNullOrEmpty()) {
            vmManager.rootGroupVM
        } else {
            vmManager.namedVms.firstOrNull { it.isGroupNode && it.hierarchyPath == groupName }
        }
        return group ?: throw RestException("group $groupName not found", HttpStatus.NOT_FOUND)
    }

    override fun getCallTree(
        vmName: String?,
        groupName: String?,
        interval: TransactionTreeInterval,
        startTime: Long,
        mergePolicies: Boolean
    ): TransactionTreeExport {
        val cursor = transactionProvider.getTransactionTreeCursor(
            getVM(vmName, groupName), interval, TransactionDataType.TRANSACTION, startTime, TimeRequirement.NEAREST_START_TIME
        )
        val export = TransactionTreeExport(DataType.CALL_TREE, transactionProvider.getCallTree(cursor, mergePolicies).transactionTree)
        return RestHelper.addTransactionProperties(export, cursor)
    }

    override fun getHotSpots(
        vmName: String?,
        groupName: String?,
        interval: TransactionTreeInterval,
        startTime: Long,
        mergePolicies: Boolean
    ): TransactionTreeExport {
        val cursor = transactionProvider.getTransactionTreeCursor(
            getVM(vmName, groupName), interval, TransactionDataType.TRANSACTION, startTime, TimeRequirement.NEAREST_START_TIME
        )
        val export = TransactionTreeExport(DataType.HOT_SPOTS, transactionProvider.getHotspots(cursor, mergePolicies).transactionTree)
        return RestHelper.addTransactionProperties(export, cursor)
    }

    override fun getOverdue(
        vmName: String?,
        groupName: String?,
        interval: TransactionTreeInterval,
        startTime: Long
    ): TransactionTreeExport {
        val cursor = transactionProvider.getTransactionTreeCursor(
            getVM(vmName, groupName), interval, TransactionDataType.OVERDUE, startTime, TimeRequirement.NEAREST_START_TIME
        )
        val export = TransactionTreeExport(DataType.OVERDUE, transactionProvider.getHotspots(cursor, false).transactionTree)
        return RestHelper.addTransactionProperties(export, cursor)
    }

    override fun getTelemetryDescriptors(): List<TelemetryDescriptor> = restTelemetryProvider.getTelemetryDescriptors()

    override fun getTelemetry(
        vmName: String?,
        groupName: String?,
        telemetryName: String,
        telemetryInterval: TelemetryInterval,
        endTime: Long
    ): TelemetryExport =
        restTelemetryProvider.getTelemetryExport(getVM(vmName, groupName), telemetryName, telemetryInterval, endTime)

    override fun triggerBackup() {
        try {
            backupHandler.backup()
        } catch (e: BackupException) {
            throw RestException(e.message, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    private fun getVM(vmName: String?, groupName: String?): VM {
        if (vmName != null) {
            val namedVms = vmManager.namedVms
            var vm = namedVms.firstOrNull { !it.isGroupNode && it.hierarchyPath == vmName }
            if (vm == null) {
                for (namedVm in namedVms) {
                    if (namedVm.type == VmType.POOL && vmName.startsWith(namedVm.hierarchyPath)) {
                        vm = vmManager.getConnectedPooledVms(namedVm)
                            .firstOrNull { (it.hierarchyPath + it.formattedInstanceId) == vmName }
                        if (vm != null) {
                            break
                        }
                    }
                }
            }
            return vm ?: throw RestException("vm not found: $vmName", HttpStatus.NOT_FOUND)
        } else if (groupName != null) {
            return vmManager.namedVms.firstOrNull { it.isGroupNode && it.hierarchyPath == groupName }
                ?: throw RestException("group not found: $groupName", HttpStatus.NOT_FOUND)
        } else {
            return vmManager.rootGroupVM
        }
    }
}
