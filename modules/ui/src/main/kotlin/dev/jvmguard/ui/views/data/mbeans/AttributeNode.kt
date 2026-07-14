package dev.jvmguard.ui.views.data.mbeans

import dev.jvmguard.mbean.common.CompositeDataWithType
import dev.jvmguard.mbean.common.MBeanHelper
import javax.management.Descriptor
import javax.management.MBeanAttributeInfo
import javax.management.MBeanInfo
import javax.management.openmbean.*

class AttributeNode(
    val name: String,
    val value: Any?,
    val openType: OpenType<*>?,
    val attributeInfo: MBeanAttributeInfo? = null,
) {

    val children = mutableListOf<AttributeNode>()

    fun addChild(treeItem: AttributeNode) {
        children.add(treeItem)
    }

    fun formattedValue(): String = AttributeValueHelper.formatValue(value, openType)

    fun isPlaceholder(): Boolean =
        AttributeValueHelper.isPlaceholderText(value, openType) || AttributeValueHelper.isArray(value)

    /** A scalar string value, which can be shown in full via the detail dialog when it doesn't fit. */
    fun isStringValue(): Boolean {
        val value = this.value ?: return false
        if (AttributeValueHelper.isArray(value)) {
            return false
        }
        return (openType == null && value is String) || openType == SimpleType.STRING
    }

    val writable: Boolean
        get() = attributeInfo?.isWritable == true && AttributeValueHelper.isEditable(openType)

    companion object {

        fun buildTree(beanInfo: MBeanInfo, values: List<Any?>): AttributeNode {
            val rootNode = AttributeNode("", null, null)
            for (pair in sortedAttributeValuePairs(beanInfo, values)) {
                val attributeInfo = pair.attributeInfo
                var value = pair.value
                var openType = openTypeOf(attributeInfo.descriptor)
                val compositeData = value as? CompositeDataWithType
                if (compositeData != null) {
                    openType = compositeData.compositeType
                    value = compositeData.values
                } else if (openType == null) {
                    openType = OpenTypeHelper.getFromStandardType(attributeInfo.type)
                }
                val treeItem = AttributeNode(attributeInfo.name, value, openType, attributeInfo)
                rootNode.addChild(treeItem)
                addChildren(value, openType, treeItem)
            }
            return rootNode
        }

        fun buildReturnValueTree(descriptor: Descriptor?, value: Any?): AttributeNode {
            val rootItem = AttributeNode("", null, null)
            var openType = openTypeOf(descriptor)
            var resolvedValue = value
            val compositeData = value as? CompositeDataWithType
            if (compositeData != null) {
                openType = compositeData.compositeType
                resolvedValue = compositeData.values
            }
            val treeItem = AttributeNode("Return value", resolvedValue, openType)
            rootItem.addChild(treeItem)
            addChildren(resolvedValue, openType, treeItem)
            return rootItem
        }

        private fun sortedAttributeValuePairs(beanInfo: MBeanInfo, values: List<Any?>): List<AttributeValuePair> =
            beanInfo.attributes
                .mapIndexed { index, attributeInfo -> AttributeValuePair(attributeInfo, values[index]) }
                .sortedBy { it.name }

        private fun openTypeOf(descriptor: Descriptor?): OpenType<*>? {
            if (descriptor == null) {
                return null
            }
            val fieldNames = descriptor.fieldNames
            if (fieldNames == null || fieldNames.size != 1 || fieldNames[0] != "openType") {
                return null
            }
            return descriptor.getFieldValue(fieldNames[0]) as? OpenType<*>
        }

        private fun addChildren(parentValue: Any?, parentOpenType: OpenType<*>?, parentItem: AttributeNode) {
            if (parentValue !is Array<*>) {
                return
            }
            @Suppress("UNCHECKED_CAST")
            val values = parentValue as Array<Any?>
            when (parentOpenType) {
                is ArrayType<*> -> addArray(values, parentOpenType.elementOpenType, parentItem)
                is CompositeType -> addComposite(values, parentOpenType, parentItem)
                is TabularType -> {
                    val rowType = parentOpenType.rowType
                    if (MBeanHelper.isSimpleKeyMap(parentOpenType)) {
                        addMap(values, rowType, ArrayList(rowType.keySet()), parentOpenType.indexNames, parentItem)
                    } else {
                        addArray(values, rowType, parentItem)
                    }
                }

                else -> addArray(values, null, parentItem)
            }
        }

        private fun addChild(name: String, value: Any?, openType: OpenType<*>?, parentItem: AttributeNode) {
            var resolvedValue = value
            var resolvedOpenType = openType
            val compositeData = value as? CompositeDataWithType
            if (compositeData != null) {
                resolvedOpenType = compositeData.compositeType
                resolvedValue = compositeData.values
            }
            val treeItem = AttributeNode(name, resolvedValue, resolvedOpenType)
            parentItem.addChild(treeItem)
            addChildren(resolvedValue, resolvedOpenType, treeItem)
        }

        private fun addArray(values: Array<Any?>, openType: OpenType<*>?, parentItem: AttributeNode) {
            for (i in values.indices) {
                addChild("[$i]", values[i], openType, parentItem)
            }
        }

        private fun addComposite(values: Array<Any?>, compositeType: CompositeType, parentItem: AttributeNode) {
            for ((index, key) in compositeType.keySet().withIndex()) {
                if (index > values.size - 1) {
                    break
                }
                addChild(key, values[index], compositeType.getType(key), parentItem)
            }
        }

        private fun addMap(
            values: Array<Any?>,
            rowType: CompositeType,
            keys: List<String>,
            indexNames: List<String>,
            parentItem: AttributeNode,
        ) {
            val indexPosition = keys.indexOf(indexNames[0])
            val valuePosition = 1 - indexPosition
            val valueType = rowType.getType(keys[valuePosition])

            val mapEntries = mutableListOf<MapEntry>()
            for (value in values) {
                val unwrapped = (value as? CompositeDataWithType)?.values ?: value
                if (unwrapped !is Array<*>) {
                    continue
                }
                if (unwrapped.size < indexPosition - 1 || unwrapped.size < valuePosition - 1) {
                    continue
                }
                mapEntries.add(MapEntry(unwrapped[indexPosition].toString(), unwrapped[valuePosition]))
            }
            mapEntries.sortBy { it.name }
            for (mapEntry in mapEntries) {
                addChild(mapEntry.name, mapEntry.value, valueType, parentItem)
            }
        }
    }
}

private class MapEntry(val name: String, val value: Any?)

private class AttributeValuePair(val attributeInfo: MBeanAttributeInfo, val value: Any?) {
    val name: String
        get() = attributeInfo.name ?: ""
}
