package com.jvmguard.data.vmdata

import com.jvmguard.agent.config.VmType

class VM(
    val type: VmType,
    var id: Long,
    var instanceId: Long,
    var rawName: String,
    groupName: String,
    var hostName: String,
    var port: Int,
) : Comparable<VM> {

    constructor(type: VmType, id: Long, instanceId: Long, name: String, groupName: String) :
            this(type, id, instanceId, name, groupName, "", 0)

    var groupName: String = groupName
        set(value) {
            field = sanitizeGroupId(value)
        }

    private var displayName: String? = null

    var name: String
        get() {
            if (type == VmType.POOLED) {
                if (displayName == null) {
                    displayName = "$hostName:$port [$formattedInstanceId]"
                }
                return displayName!!
            } else {
                return rawName
            }
        }
        set(value) {
            rawName = value
        }

    val isGroupNode: Boolean
        get() = type.isGroupNode

    override fun toString(): String = verbose

    private fun sanitizeGroupId(groupId: String): String =
        groupId.split("/")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("/")

    override fun compareTo(other: VM): Int {
        // This sort order is important for the UI
        val typeResult = type.databaseId - other.type.databaseId
        return if (typeResult != 0) {
            typeResult
        } else {
            val groupResult = groupName.compareTo(other.groupName)
            if (groupResult != 0) {
                groupResult
            } else {
                name.compareTo(other.name)
            }
        }
    }

    fun isIncluded(identifier: VmIdentifier): Boolean = qualifiedIdentifier.isIncluded(identifier)

    val formattedInstanceId: String
        get() = Integer.toHexString(instanceId.hashCode())

    val verbose: String
        get() {
            val hierarchyPath = displayHierarchyPath
            return when (type) {
                VmType.NAMED, VmType.POOLED -> "VM \"$hierarchyPath\""
                VmType.GROUP -> if (hierarchyPath.isEmpty()) "root group" else "VM group $hierarchyPath"
                VmType.POOL -> "VM pool $hierarchyPath"
            }
        }

    val hierarchyPath: String
        get() = if (groupName.isEmpty()) rawName else "$groupName/$rawName"

    val displayHierarchyPath: String
        get() = if (groupName.isEmpty()) name else "$groupName/$name"

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val vm = other as VM
        return id == vm.id
    }

    override fun hashCode(): Int = id.hashCode()

    val parentIdentifier: VmIdentifier
        get() = VmIdentifier(groupName, type.parentType)

    val qualifiedIdentifier: VmIdentifier
        get() = VmIdentifier(hierarchyPath, type)

    val unqualifiedIdentifier: VmIdentifier
        get() = VmIdentifier(rawName, type)
}
