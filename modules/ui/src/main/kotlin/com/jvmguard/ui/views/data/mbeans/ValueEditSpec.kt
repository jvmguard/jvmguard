package com.jvmguard.ui.views.data.mbeans

import javax.management.openmbean.OpenType
import javax.management.openmbean.SimpleType

class ValueEditSpec(
    val openType: OpenType<*>?,
    val type: String,
    val caption: String,
    val initialValue: Any?,
) {

    fun isSimpleType(simpleType: SimpleType<*>): Boolean {
        if (openType == simpleType) {
            return true
        }
        val typeName = simpleType.typeName
        return type == typeName || type == PrimitiveTypes.unwrapName(typeName)
    }

    val isNullable: Boolean
        get() = type !in PrimitiveTypes.ALL_PRIMITIVE_NAMES

    override fun toString(): String =
        "ValueEditSpec{openType=$openType, type='$type', caption='$caption', initialValue=$initialValue}"
}
