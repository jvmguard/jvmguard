package com.jvmguard.ui.views.data.mbeans

import com.jvmguard.mbean.common.MBeanHelper
import javax.management.openmbean.ArrayType
import javax.management.openmbean.TabularType

object PathValidationHelper {

    fun validatePath(item: AttributeNode, parentOf: (AttributeNode) -> AttributeNode?): String? {
        if (!OpenTypeHelper.isNumberType(item.openType)) {
            return "Only number values can be selected"
        }
        var current: AttributeNode? = item
        while (current != null) {
            val openType = current.openType
            if (openType is TabularType) {
                if (!MBeanHelper.isSimpleKeyMap(openType)) {
                    return "Tabular types other than simple maps are not supported for custom telemetries"
                }
            } else if (openType is ArrayType<*>) {
                return "Arrays are not supported for custom telemetries"
            }
            current = parentOf(current)
        }
        return null
    }

    fun getSelectedPath(selectedItem: AttributeNode, parentOf: (AttributeNode) -> AttributeNode?): String {
        val buffer = StringBuilder()
        var parentItem: AttributeNode? = null
        for (item in pathItems(selectedItem, parentOf)) {
            if (buffer.isNotEmpty()) {
                buffer.append("/")
            }
            buffer.append(item.name.replace("\\", "\\\\").replace("/", "\\/"))
            val parentOpenType = parentItem?.openType
            if (parentOpenType != null && MBeanHelper.isSimpleKeyMap(parentOpenType)) {
                val tabularType = parentOpenType as TabularType
                val keys = ArrayList(tabularType.rowType.keySet())
                keys.remove(tabularType.indexNames[0])
                buffer.append('/').append(keys[0])
            }
            parentItem = item
        }
        return buffer.toString()
    }

    fun getSuggestedLineName(attributePath: String): String =
        lastComponent(attributePath).replace("\\/", "/").replace("\\\\", "\\")

    private fun pathItems(
        selectedItem: AttributeNode,
        parentOf: (AttributeNode) -> AttributeNode?,
    ): List<AttributeNode> {
        val selectedPath = mutableListOf<AttributeNode>()
        var item: AttributeNode? = selectedItem
        while (item != null) {
            selectedPath.add(item)
            item = parentOf(item)
        }
        selectedPath.reverse()
        return selectedPath
    }

    private fun lastComponent(attributePath: String): String {
        val index = attributePath.lastIndexOf('/')
        return if (index > -1) attributePath.substring(index + 1) else attributePath
    }
}
