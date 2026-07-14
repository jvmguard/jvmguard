package dev.jvmguard.ui.views.data.mbeans

import dev.jvmguard.mbean.common.MBeanNameComponents

sealed class MBeanNode(val name: String) {

    val children = mutableListOf<MBeanNode>()

    fun addChild(treeItem: MBeanNode) {
        children.add(treeItem)
    }

    private fun sortRecursively(comparator: Comparator<MBeanNode>) {
        children.sortWith(comparator)
        children.forEach { it.sortRecursively(comparator) }
    }

    companion object {

        fun buildTree(objectNames: Collection<String>): MBeanNode {
            val rootItem: MBeanNode = MBeanFolderNode("")
            for (objectName in objectNames) {
                var parentItem: MBeanNode = rootItem
                val pathComponents = MBeanNameComponents.getPathComponents(objectName)
                val componentCount = pathComponents.size
                for (i in 0 until componentCount) {
                    parentItem = getOrCreateItem(parentItem, pathComponents[i], objectName, i == componentCount - 1)
                }
            }
            rootItem.sortRecursively { node1, node2 ->
                when {
                    node1.javaClass == node2.javaClass -> node1.name.compareTo(node2.name)
                    node1 is MBeanFolderNode -> -1
                    else -> 1
                }
            }
            return rootItem
        }

        private fun getOrCreateItem(
            parentItem: MBeanNode,
            priorityEntry: MBeanNameComponents.PriorityEntry,
            objectName: String,
            leaf: Boolean,
        ): MBeanNode {
            val name = createName(priorityEntry.key, priorityEntry.value)
            if (!leaf) {
                for (treeItem in parentItem.children) {
                    if (treeItem is MBeanFolderNode && treeItem.name == name) {
                        return treeItem
                    }
                }
            }
            val treeItem = if (leaf) MBeanLeafNode(name, objectName) else MBeanFolderNode(name)
            parentItem.addChild(treeItem)
            return treeItem
        }

        private fun createName(key: String?, value: String?): String {
            val name = value ?: ""
            return if (key.isNullOrEmpty()) name else "$name [$key]"
        }
    }
}

class MBeanFolderNode(name: String) : MBeanNode(name)

class MBeanLeafNode(name: String, val objectName: String) : MBeanNode(name)
