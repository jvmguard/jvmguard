package com.jvmguard.ui.views.data.mbeans

import com.jvmguard.ui.components.nameMatchesFilter
import com.vaadin.flow.component.treegrid.TreeGrid
import com.vaadin.flow.data.provider.hierarchy.TreeData
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider

fun populateMBeanTree(
    tree: TreeGrid<MBeanNode>,
    names: List<String>,
    query: String,
    useRegex: Boolean,
    matchCase: Boolean,
) {
    val filtered = if (query.isEmpty()) names else names.filter { nameMatchesFilter(it, query, useRegex, matchCase) }
    val root = MBeanNode.buildTree(filtered)
    val treeData = TreeData<MBeanNode>()
    addMBeanNodes(treeData, null, root.children)
    tree.setDataProvider(TreeDataProvider(treeData))
    if (query.isNotEmpty()) {
        tree.expandRecursively(treeData.rootItems, Int.MAX_VALUE)
    }
}

private fun addMBeanNodes(treeData: TreeData<MBeanNode>, parent: MBeanNode?, nodes: List<MBeanNode>) {
    for (node in nodes) {
        treeData.addItem(parent, node)
        addMBeanNodes(treeData, node, node.children)
    }
}
