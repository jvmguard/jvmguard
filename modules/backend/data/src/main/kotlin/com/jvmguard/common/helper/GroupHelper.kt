package com.jvmguard.common.helper

import com.jvmguard.common.util.StringUtil
import com.jvmguard.data.base.HierarchicalConfig
import com.jvmguard.data.config.GroupConfig
import com.jvmguard.data.vmdata.VmIdentifier

object GroupHelper {

    const val ROOT_GROUP_ID = ""

    fun getParentHierarchyPath(groupId: String): String? {
        val lastSlashIndex = groupId.lastIndexOf('/')
        return if (lastSlashIndex > -1) {
            groupId.substring(0, lastSlashIndex)
        } else {
            null
        }
    }

    fun getSimpleName(groupId: String): String {
        val lastSlashIndex = groupId.lastIndexOf('/')
        return if (lastSlashIndex > -1) {
            groupId.substring(lastSlashIndex + 1)
        } else {
            groupId
        }
    }

    fun findGroupConfigByGroupIdentifier(identifier: VmIdentifier?, allGroupConfigs: Collection<GroupConfig>): GroupConfig? =
        allGroupConfigs.firstOrNull { identifier != null && identifier == it.groupIdentifier }

    fun checkAgainstGroupRoots(groupConfig: GroupConfig, groupRoots: List<VmIdentifier>): Boolean =
        checkAgainstGroupRoots(groupConfig.groupIdentifier, groupRoots)

    fun checkAgainstGroupRoots(groupIdentifier: VmIdentifier, groupRoots: List<VmIdentifier>): Boolean =
        groupRoots.any { root ->
            VmIdentifier.ROOT_GROUP_IDENTIFIER == root || groupIdentifier == root || groupIdentifier.isChild(root)
        }

    fun sortByHierarchy(groupConfigs: MutableList<GroupConfig>) {
        groupConfigs.sortWith(
            Comparator.comparingInt<GroupConfig> { groupConfig ->
                if (groupConfig == null || groupConfig.isRoot) {
                    -1
                } else {
                    StringUtil.countChar(groupConfig.hierarchySeparatorChar, groupConfig.hierarchyPath)
                }
            }.thenComparing(HierarchicalConfig::getHierarchyPath)
        )
    }
}
