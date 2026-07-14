package dev.jvmguard.ui.views.data.mbeans

import dev.jvmguard.common.Loggers
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*
import javax.management.ObjectName
import javax.management.openmbean.ArrayType
import javax.management.openmbean.OpenDataException
import javax.management.openmbean.OpenType
import javax.management.openmbean.SimpleType

object OpenTypeHelper {

    fun isEditable(arrayType: ArrayType<*>): Boolean =
        arrayType.dimension == 1 && arrayType.elementOpenType is SimpleType

    fun getDefaultValue(openType: OpenType<*>?): Any? = when (openType) {
        SimpleType.BOOLEAN -> false
        SimpleType.CHARACTER -> 'a'
        SimpleType.BYTE -> 0.toByte()
        SimpleType.SHORT -> 0.toShort()
        SimpleType.INTEGER -> 0
        SimpleType.LONG -> 0L
        SimpleType.FLOAT -> 0f
        SimpleType.DOUBLE -> 0.0
        SimpleType.BIGDECIMAL -> BigDecimal("0")
        SimpleType.OBJECTNAME -> null
        SimpleType.BIGINTEGER -> BigInteger("0")
        SimpleType.DATE -> Date()
        is ArrayType<*> -> if (isEditable(openType)) emptyArray<Any?>() else ""
        else -> ""
    }

    fun isNumberType(openType: OpenType<*>?): Boolean = openType is SimpleType && (
            openType == SimpleType.BYTE ||
                    openType == SimpleType.SHORT ||
                    openType == SimpleType.INTEGER ||
                    openType == SimpleType.LONG ||
                    openType == SimpleType.FLOAT ||
                    openType == SimpleType.DOUBLE ||
                    openType == SimpleType.BIGDECIMAL ||
                    openType == SimpleType.BIGINTEGER
            )

    fun getFromStandardType(type: String?): OpenType<*>? {
        if (type == null) {
            return null
        }
        var remaining: String = type
        var dimension = 0
        while (remaining.startsWith("[")) {
            dimension++
            remaining = remaining.substring(1)
        }
        if (dimension > 0) {
            val elementType = if (remaining.startsWith("L") && remaining.endsWith(";")) {
                standardElementType(remaining.substring(1, remaining.length - 1))
            } else {
                primitiveElementType(remaining)
            }
            if (elementType != null) {
                try {
                    return ArrayType<Any>(dimension, elementType)
                } catch (e: OpenDataException) {
                    Loggers.SERVER.warn("Could not build array type", e)
                }
            }
            return null
        }
        return standardElementType(remaining)
    }

    private fun primitiveElementType(type: String): OpenType<*>? {
        if (type.length == 1) {
            return when (type[0]) {
                'B' -> SimpleType.BOOLEAN
                'C' -> SimpleType.CHARACTER
                'D' -> SimpleType.DOUBLE
                'F' -> SimpleType.FLOAT
                'I' -> SimpleType.INTEGER
                'J' -> SimpleType.LONG
                'S' -> SimpleType.SHORT
                'Z' -> SimpleType.BOOLEAN
                else -> null
            }
        }
        return null
    }

    private fun standardElementType(type: String): OpenType<*>? = when {
        checkClass(type, Boolean::class.javaObjectType) || checkClass(type, java.lang.Boolean.TYPE) -> SimpleType.BOOLEAN
        checkClass(type, Byte::class.javaObjectType) || checkClass(type, java.lang.Byte.TYPE) -> SimpleType.BYTE
        checkClass(type, Char::class.javaObjectType) || checkClass(type, Character.TYPE) -> SimpleType.CHARACTER
        checkClass(type, Short::class.javaObjectType) || checkClass(type, java.lang.Short.TYPE) -> SimpleType.SHORT
        checkClass(type, Int::class.javaObjectType) || checkClass(type, Integer.TYPE) -> SimpleType.INTEGER
        checkClass(type, Long::class.javaObjectType) || checkClass(type, java.lang.Long.TYPE) -> SimpleType.LONG
        checkClass(type, Float::class.javaObjectType) || checkClass(type, java.lang.Float.TYPE) -> SimpleType.FLOAT
        checkClass(type, Double::class.javaObjectType) || checkClass(type, java.lang.Double.TYPE) -> SimpleType.DOUBLE
        checkClass(type, BigDecimal::class.java) -> SimpleType.BIGDECIMAL
        checkClass(type, BigInteger::class.java) -> SimpleType.BIGINTEGER
        checkClass(type, ObjectName::class.java) -> SimpleType.OBJECTNAME
        checkClass(type, Date::class.java) -> SimpleType.DATE
        checkClass(type, String::class.java) -> SimpleType.STRING
        checkClass(type, Void.TYPE) -> SimpleType.VOID
        else -> null
    }

    private fun checkClass(type: String, c: Class<*>): Boolean = c.name == type
}
