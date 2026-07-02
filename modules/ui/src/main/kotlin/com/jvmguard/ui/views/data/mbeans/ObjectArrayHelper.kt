package com.jvmguard.ui.views.data.mbeans

import java.math.BigDecimal
import java.math.BigInteger
import javax.management.MalformedObjectNameException
import javax.management.ObjectName

object ObjectArrayHelper {

    private const val SEPARATOR_CHAR = ';'
    private val EMPTY_STRING_MARKER = String(charArrayOf(0.toChar()))

    fun convertToArray(value: String?, componentClass: Class<*>, allowNullValues: Boolean): Array<Any?>? = when {
        value == null -> null
        value.trim().isEmpty() -> arrayOfNulls(0)
        else -> splitValue(value, componentClass)
            .map { singleValue ->
                try {
                    toObject(singleValue, componentClass, allowNullValues)
                } catch (_: NumberFormatException) {
                    throw MBeanConversionException("Could not convert \"$singleValue\" to ${componentClass.name}")
                }
            }
            .toTypedArray()
    }

    fun convertToString(value: Array<Any?>?): String? {
        if (value == null) {
            return null
        }
        if (value.isEmpty()) {
            return ""
        }
        val buffer = StringBuilder()
        for (i in value.indices) {
            val singleValue = value[i]
            if (i > 0) {
                buffer.append(";")
            }
            if (singleValue != null) {
                val string = singleValue.toString()
                val needsQuote = string.trim().isEmpty() || string.indexOf(SEPARATOR_CHAR) > -1
                if (needsQuote) {
                    buffer.append('"')
                }
                buffer.append(string.replace("\"", "\"\""))
                if (needsQuote) {
                    buffer.append('"')
                }
            } else if (i == value.size - 1) {
                // null element at the end requires an additional ; because in splitValue the last ; is stripped
                buffer.append(";")
            }
        }
        return buffer.toString()
    }

    private fun splitValue(value: String, componentClass: Class<*>): List<String> {
        val singleValues = mutableListOf<String>()
        var startIndex = 0
        val lastIndex = value.length - 1
        var inQuotes = false
        for (currentIndex in value.indices) {
            if (value[currentIndex] == '"') {
                inQuotes = !inQuotes
            }
            if (currentIndex == lastIndex) {
                // one trailing ; is stripped to allow arrays with a single null element
                val offset = if (value[lastIndex] == SEPARATOR_CHAR) 1 else 0
                singleValues.add(unquote(value.substring(startIndex, lastIndex - offset + 1), componentClass))
            } else if (value[currentIndex] == SEPARATOR_CHAR && !inQuotes) {
                singleValues.add(unquote(value.substring(startIndex, currentIndex), componentClass))
                startIndex = currentIndex + 1
            }
        }
        return singleValues
    }

    private fun unquote(string: String, componentClass: Class<*>): String {
        var result = string.trim()
        if (componentClass == String::class.java && result == "\"\"") {
            return EMPTY_STRING_MARKER
        }
        if (result.startsWith("\"")) {
            result = result.substring(1)
        }
        if (result.endsWith("\"")) {
            result = result.substring(0, result.length - 1)
        }
        result = result.replace("\"\"", "\"")
        if (componentClass != String::class.java) {
            result = result.trim()
        }
        return result
    }

    private fun toObject(value: String, componentClass: Class<*>, allowNullValues: Boolean): Any? = when {
        value.isEmpty() ->
            if (allowNullValues) null else throw MBeanConversionException("Null values not allowed for primitive arrays")

        componentClass == Boolean::class.javaObjectType -> value.toBoolean()
        componentClass == Byte::class.javaObjectType -> value.toByte()
        componentClass == Char::class.javaObjectType -> if (value.isEmpty()) 0.toChar() else value[0]
        componentClass == Short::class.javaObjectType -> value.toShort()
        componentClass == Int::class.javaObjectType -> value.toInt()
        componentClass == Long::class.javaObjectType -> value.toLong()
        componentClass == Float::class.javaObjectType -> value.toFloat()
        componentClass == Double::class.javaObjectType -> value.toDouble()
        componentClass == BigInteger::class.java -> BigInteger(value)
        componentClass == BigDecimal::class.java -> BigDecimal(value)
        componentClass == ObjectName::class.java ->
            try {
                ObjectName(value)
            } catch (e: MalformedObjectNameException) {
                throw MBeanConversionException(e.message ?: "Malformed object name")
            }

        componentClass == String::class.java -> if (value == EMPTY_STRING_MARKER) "" else value
        else -> value
    }
}
