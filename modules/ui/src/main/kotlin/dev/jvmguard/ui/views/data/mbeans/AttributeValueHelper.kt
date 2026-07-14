package dev.jvmguard.ui.views.data.mbeans

import java.text.SimpleDateFormat
import java.util.*
import javax.management.openmbean.*

object AttributeValueHelper {

    private val DATE_FORMAT = ThreadLocal.withInitial { SimpleDateFormat("yyyy-MM-dd HH:mm:ss") }

    fun formatValue(value: Any?, openType: OpenType<*>?): String = when {
        openType is ArrayType<*> -> arrayText(value)
        openType is CompositeType || openType is TabularType ->
            if (value == null) nullVerbose() else "[${replacedTypeName(openType)}]"

        isArray(value) -> arrayText(value)
        else -> verboseValue(value, openType)
    }

    fun isPlaceholderText(value: Any?, openType: OpenType<*>?): Boolean =
        value == null || (openType != null && openType !is SimpleType)

    fun isArray(value: Any?): Boolean = value != null && value.javaClass.isArray

    fun isEditable(openType: OpenType<*>?): Boolean = when (openType) {
        is SimpleType -> true
        is ArrayType<*> -> OpenTypeHelper.isEditable(openType)
        else -> false
    }

    private fun replacedTypeName(openType: OpenType<*>): String =
        if (openType is CompositeType && openType.typeName.startsWith(Map::class.java.name)) {
            "map entry"
        } else {
            openType.typeName
        }

    private fun nullVerbose(): String = "[null]"

    private fun verboseValue(value: Any?, openType: OpenType<*>?): String = when {
        value == null -> nullVerbose()
        openType == SimpleType.DATE && value is Date -> DATE_FORMAT.get().format(value)
        else -> value.toString()
    }

    private fun arrayLength(value: Any?): Int =
        if (isArray(value)) (value as Array<*>).size else 0

    private fun arrayText(value: Any?): String =
        if (value == null) nullVerbose() else "[${arrayLength(value)} elements]"
}
