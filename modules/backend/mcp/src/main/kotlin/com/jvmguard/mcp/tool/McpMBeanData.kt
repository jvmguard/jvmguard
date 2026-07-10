package com.jvmguard.mcp.tool

import com.jvmguard.mbean.common.CompositeDataWithType
import com.jvmguard.mbean.common.MBeanHelper
import javax.management.Descriptor
import javax.management.MBeanAttributeInfo
import javax.management.MBeanInfo
import javax.management.openmbean.ArrayType
import javax.management.openmbean.CompositeType
import javax.management.openmbean.OpenType
import javax.management.openmbean.TabularType

object McpMBeanData {

    fun decodeAttribute(attribute: MBeanAttributeInfo, value: Any?): Any? =
        decode(value, openTypeOf(attribute.descriptor))

    fun operationSignatures(beanInfo: MBeanInfo): List<String> =
        beanInfo.operations.orEmpty().map { operation ->
            val params = operation.signature.orEmpty().joinToString(",") { it.type.substringAfterLast('.') }
            "${operation.name}($params)"
        }

    private fun decode(value: Any?, openType: OpenType<*>?): Any? {
        if (value == null) {
            return null
        }
        if (value is CompositeDataWithType) {
            return decodeComposite(value.values, value.compositeType)
        }
        if (value is Array<*>) {
            return when (openType) {
                is CompositeType -> decodeComposite(value, openType)
                is TabularType ->
                    if (MBeanHelper.isSimpleKeyMap(openType)) {
                        decodeSimpleKeyMap(value, openType)
                    } else {
                        value.map { decode(it, openType.rowType) }
                    }

                is ArrayType<*> -> value.map { decode(it, openType.elementOpenType) }
                else -> value.map { decode(it, null) }
            }
        }
        return scalar(value)
    }

    private fun decodeComposite(values: Array<*>, compositeType: CompositeType): Map<String, Any?> {
        val map = LinkedHashMap<String, Any?>()
        for ((index, key) in compositeType.keySet().withIndex()) {
            if (index > values.size - 1) {
                break
            }
            map[key] = decode(values[index], compositeType.getType(key))
        }
        return map
    }

    private fun decodeSimpleKeyMap(rows: Array<*>, tabularType: TabularType): Map<String, Any?> {
        val rowType = tabularType.rowType
        val keys = ArrayList(rowType.keySet())
        val indexPosition = keys.indexOf(tabularType.indexNames[0])
        val valuePosition = 1 - indexPosition
        val valueType = rowType.getType(keys[valuePosition])

        val map = LinkedHashMap<String, Any?>()
        for (row in rows) {
            val cells = (row as? CompositeDataWithType)?.values ?: row
            if (cells !is Array<*> || cells.size <= indexPosition || cells.size <= valuePosition) {
                continue
            }
            map[cells[indexPosition].toString()] = decode(cells[valuePosition], valueType)
        }
        return map
    }

    private fun scalar(value: Any): Any =
        when (value) {
            is Number, is Boolean, is String -> value
            else -> value.toString()
        }

    private fun openTypeOf(descriptor: Descriptor?): OpenType<*>? {
        val fieldNames = descriptor?.fieldNames
        if (fieldNames == null || fieldNames.size != 1 || fieldNames[0] != "openType") {
            return null
        }
        return descriptor.getFieldValue(fieldNames[0]) as? OpenType<*>
    }
}
