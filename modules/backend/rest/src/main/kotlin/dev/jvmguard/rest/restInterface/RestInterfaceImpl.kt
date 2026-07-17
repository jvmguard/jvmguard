package dev.jvmguard.rest.restInterface

import dev.jvmguard.agent.config.VmType
import dev.jvmguard.collector.api.TransactionProvider
import dev.jvmguard.collector.api.VmManager
import dev.jvmguard.collector.util.BackupHandler
import dev.jvmguard.collector.util.BackupHandler.BackupException
import dev.jvmguard.common.Loggers
import dev.jvmguard.common.config.ConfigManager
import dev.jvmguard.common.export.TelemetryExport
import dev.jvmguard.common.export.TransactionTreeExport
import dev.jvmguard.common.export.TransactionTreeExport.DataType
import dev.jvmguard.common.helper.GroupHelper
import dev.jvmguard.common.helper.PasswordHelper
import dev.jvmguard.data.transactions.TimeRequirement
import dev.jvmguard.data.transactions.TransactionDataType
import dev.jvmguard.data.transactions.TransactionTreeInterval
import dev.jvmguard.data.user.AccessLevel
import dev.jvmguard.data.user.UserManager
import dev.jvmguard.data.vmdata.TelemetryInterval
import dev.jvmguard.data.vmdata.VM
import dev.jvmguard.data.vmdata.VmIdentifier
import dev.jvmguard.rest.RestHelper
import dev.jvmguard.rest.entity.GroupEntity
import dev.jvmguard.rest.entity.TelemetryDescriptor
import dev.jvmguard.rest.provider.RestException
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import javax.security.auth.login.CredentialException

@Component
class RestInterfaceImpl(
    private val userManager: UserManager,
    private val configManager: ConfigManager,
    private val vmManager: VmManager,
    private val restTelemetryProvider: RestTelemetryProvider,
    private val transactionProvider: TransactionProvider,
    private val backupHandler: BackupHandler
) : RestInterface {

    override fun checkAccess(name: String, apiKey: String): AccessLevel? {
        val user = userManager.getByLoginName(name) ?: return null
        if (user.apiKeyHash.isEmpty() || !PasswordHelper.validatePassword(apiKey, user.apiKeyHash)) {
            return null
        }
        if (PasswordHelper.needsRehash(user.apiKeyHash)) {
            user.apiKeyHash = PasswordHelper.createHash(apiKey)
            try {
                userManager.store(user)
            } catch (e: CredentialException) {
                Loggers.SERVER.warn("Could not re-hash API key for {}", name, e)
            }
        }
        return user.accessLevel
    }

    override fun getGroups(): List<GroupEntity> {
        val roots = currentGroupRoots()
        val ret = ArrayList<GroupEntity>()
        for (vm in vmManager.namedVms) {
            val qualifiedIdentifier = vm.qualifiedIdentifier
            if (vm.isGroupNode && qualifiedIdentifier != VmIdentifier.ROOT_GROUP_IDENTIFIER && isInScope(qualifiedIdentifier, roots)) {
                ret.add(GroupEntity(qualifiedIdentifier.toString(), qualifiedIdentifier.type == VmType.POOL))
            }
        }
        ret.sortBy { it.name }
        return ret
    }

    override fun getVms(groupName: String?, connected: Boolean): List<String> {
        val roots = currentGroupRoots()
        val group = getGroup(groupName)
        if (groupName != null) {
            checkScope(group.qualifiedIdentifier, roots)
        }
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
                    if (connection.vm.type == VmType.NAMED && connection.vm.isIncluded(groupIdentifier) && isInScope(connection.vm.qualifiedIdentifier, roots)) {
                        ret.add(connection.vm.hierarchyPath)
                    }
                }
            }

            else -> {
                for (vm in vmManager.namedVms) {
                    if (!vm.isGroupNode && vm.isIncluded(groupIdentifier) && isInScope(vm.qualifiedIdentifier, roots)) {
                        ret.add(vm.hierarchyPath)
                    }
                }
            }
        }
        ret.sort()
        return ret
    }

    private fun currentGroupRoots(): List<VmIdentifier>? {
        val loginName = SecurityContextHolder.getContext().authentication?.name ?: return emptyList()
        val user = userManager.getByLoginName(loginName) ?: return emptyList()
        if (user.accessLevel == AccessLevel.ADMIN) {
            return null
        }
        return configManager.getGroupRoots(user.groupNames)
    }

    private fun isInScope(identifier: VmIdentifier, roots: List<VmIdentifier>?): Boolean =
        roots == null || GroupHelper.checkAgainstGroupRoots(identifier, roots)

    private fun checkScope(identifier: VmIdentifier, roots: List<VmIdentifier>?) {
        if (!isInScope(identifier, roots)) {
            throw RestException("access to $identifier is not allowed", HttpStatus.FORBIDDEN)
        }
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
        val vm = resolveVM(vmName, groupName)
        checkScope(vm.qualifiedIdentifier, currentGroupRoots())
        return vm
    }

    private fun resolveVM(vmName: String?, groupName: String?): VM {
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
