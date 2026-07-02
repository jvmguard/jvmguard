package com.jvmguard.data.vmdata

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.jvmguard.agent.config.VmType
import com.jvmguard.common.helper.GroupHelper
import java.io.Serializable
import javax.annotation.concurrent.Immutable

@Immutable
open class VmIdentifier @JsonCreator constructor(
    @param:JsonProperty("name") val name: String,
    @param:JsonProperty("type") val type: VmType,
) : Comparable<VmIdentifier>, Serializable {

    fun toUnqualified(): VmIdentifier {
        val lastSlash = name.lastIndexOf('/')
        return if (lastSlash == -1) {
            this
        } else {
            VmIdentifier(name.substring(lastSlash + 1), type)
        }
    }

    fun toQualified(parentPath: String?): VmIdentifier {
        return if (parentPath.isNullOrEmpty() || name.indexOf('/') > -1) {
            this
        } else {
            VmIdentifier("$parentPath/$name", type)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val that = other as VmIdentifier
        if (name != that.name) {
            return false
        }
        if (type != that.type) {
            return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }

    override fun toString(): String = name

    override fun compareTo(other: VmIdentifier): Int {
        val nameCompare = name.compareTo(other.name)
        return if (nameCompare == 0) {
            type.databaseId - other.type.databaseId
        } else {
            nameCompare
        }
    }

    val isRoot: Boolean
        get() = type == VmType.GROUP && name.isEmpty()

    val parent: VmIdentifier?
        get() {
            if (isRoot) {
                return null
            }
            val parentName = GroupHelper.getParentHierarchyPath(name)
            return if (parentName == null) {
                ROOT_GROUP_IDENTIFIER
            } else {
                VmIdentifier(parentName, type.parentType)
            }
        }

    fun isChild(parent: VmIdentifier): Boolean {
        if (parent.type.isGroupNode) {
            if (parent.isRoot) {
                return !isRoot
            } else if (name.startsWith(parent.name + "/")) {
                val parentType = type.parentType
                if (parent.type == parentType) {
                    return true
                }
                // might be a pooled vm and a parent of the pool
                if (parentType == VmType.POOL) {
                    val poolPath = this.parent!!.name
                    return poolPath.startsWith(parent.name + "/")
                }
            }
        }
        return false
    }

    fun isIncluded(parent: VmIdentifier): Boolean {
        if (parent.isRoot) {
            return true
        }
        if (parent == this) {
            return true
        }
        return isChild(parent)
    }

    companion object {
        val ROOT_GROUP_IDENTIFIER: VmIdentifier = VmIdentifier(GroupHelper.ROOT_GROUP_ID, VmType.GROUP)
    }
}
