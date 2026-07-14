package dev.jvmguard.collector.api

import dev.jvmguard.agent.data.MethodInfo
import dev.jvmguard.agent.mbean.MBeanData
import dev.jvmguard.agent.mbean.MBeanModificationData
import dev.jvmguard.agent.mbean.MBeanOperationData
import dev.jvmguard.data.config.triggers.actions.RecordJfrAction
import dev.jvmguard.data.config.triggers.actions.RecordJpsAction
import dev.jvmguard.data.dashboard.Group
import dev.jvmguard.data.user.User
import dev.jvmguard.data.vmdata.*
import java.io.File
import javax.management.MBeanAttributeInfo
import javax.management.MBeanOperationInfo

interface VmManager {

    val currentConnections: List<Connection>
    val namedVms: Collection<VM>
    val rootGroupVM: VM
    val agentKeystore: File

    fun getConnection(vm: VM): AgentConnection

    fun getVmDataHolders(vmFilter: VmFilter, sparkLineRange: SparkLineRange, telemetryTypes: Collection<TelemetryType>): Group<VmDataHolder>
    fun getGroupVmDataHolder(
        vmFilter: VmFilter,
        vmIdentifier: VmIdentifier?,
        sparkLineRange: SparkLineRange,
        telemetryTypes: Collection<TelemetryType>
    ): VmDataHolder

    fun getVmDataHolder(vmFilter: VmFilter, vm: VM, sparkLineRange: SparkLineRange, telemetryTypes: Collection<TelemetryType>): VmDataHolder

    fun getPackageStats(vm: VM): Map<String, Int>
    fun getClassNames(vm: VM, allClasses: Boolean): Collection<String>
    fun getMethods(className: String, vm: VM): Collection<MethodInfo>

    fun runGC(vm: VM)
    fun heapDump(vm: VM, user: User)
    fun threadDump(vm: VM, user: User)
    fun recordJps(vm: VM, user: User, recordJpsAction: RecordJpsAction)
    fun recordJfr(vm: VM, user: User, recordJfrAction: RecordJfrAction)

    fun deleteVM(vm: VM): Boolean

    fun getVms(ids: LongArray): Collection<VM>

    fun getConnectedPooledVms(pool: VM): Collection<VM>

    fun terminate(vm: VM, closeConnection: Boolean)

    fun getMBeanNames(vm: VM, createPlatformServer: Boolean): Collection<String>
    fun getMBeanData(vm: VM, name: String, fetchStructure: Boolean, fetchValues: Boolean): MBeanData
    fun invokeMBeanOperation(vm: VM, name: String, operationInfo: MBeanOperationInfo, parameters: Array<Any?>): MBeanOperationData
    fun setMBeanAttribute(vm: VM, name: String, attributeInfo: MBeanAttributeInfo, value: Any?): MBeanModificationData
}
