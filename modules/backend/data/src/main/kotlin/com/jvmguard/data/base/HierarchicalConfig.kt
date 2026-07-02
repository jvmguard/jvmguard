package com.jvmguard.data.base

import com.jvmguard.agent.config.base.Hierarchical

abstract class HierarchicalConfig(private var hierarchyPath: String) : StoredConfig(), Hierarchical {

    override fun getHierarchyPath(): String = hierarchyPath

    override fun setHierarchyPath(hierarchyPath: String) {
        val trimmed = hierarchyPath.trim()
        val oldValue = this.hierarchyPath
        this.hierarchyPath = trimmed
        fireChanged(oldValue, trimmed)
    }
}
