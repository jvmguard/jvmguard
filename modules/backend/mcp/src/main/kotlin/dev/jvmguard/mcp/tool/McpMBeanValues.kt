package dev.jvmguard.mcp.tool

import dev.jvmguard.mcp.McpError
import java.math.BigDecimal
import java.math.BigInteger
import javax.management.ObjectName

object McpMBeanValues {

    private val PRIMITIVE_NAMES = setOf("boolean", "byte", "char", "short", "int", "long", "float", "double")

    fun coerce(typeName: String, raw: Any?): Any? {
        if (typeName.startsWith("[")) {
            return coerceArray(typeName, raw)
        }
        if (raw == null) {
            if (typeName in PRIMITIVE_NAMES) {
                throw McpError("A value is required for the primitive type '$typeName'.")
            }
            return null
        }
        try {
            return when (typeName) {
                "boolean", "java.lang.Boolean" -> asBoolean(raw)
                "byte", "java.lang.Byte" -> asLong(raw).toByte()
                "short", "java.lang.Short" -> asLong(raw).toShort()
                "int", "java.lang.Integer" -> asLong(raw).toInt()
                "long", "java.lang.Long" -> asLong(raw)
                "float", "java.lang.Float" -> asDouble(raw).toFloat()
                "double", "java.lang.Double" -> asDouble(raw)
                "char", "java.lang.Character" -> raw.toString().singleOrNull()
                    ?: throw McpError("Expected a single character for type '$typeName'.")
                "java.lang.String" -> raw.toString()
                "java.math.BigInteger" -> BigInteger(raw.toString().trim())
                "java.math.BigDecimal" -> BigDecimal(raw.toString().trim())
                "javax.management.ObjectName" -> ObjectName.getInstance(raw.toString())
                else -> throw McpError(
                    "Unsupported value type '$typeName'. Supported: booleans, numbers, characters, strings, " +
                            "BigInteger/BigDecimal and ObjectName.",
                )
            }
        } catch (e: McpError) {
            throw e
        } catch (e: Exception) {
            throw McpError("Cannot convert \"$raw\" to '$typeName': ${e.message}")
        }
    }

    private fun coerceArray(typeName: String, raw: Any?): Array<Any?>? {
        if (raw == null) {
            return null
        }
        if (typeName.startsWith("[[")) {
            throw McpError("Multi-dimensional arrays are not supported.")
        }
        val elementType = arrayElementType(typeName)
        val list = raw as? List<*>
            ?: throw McpError("Expected a JSON array for type '$typeName'.")
        return list.map { coerce(elementType, it) }.toTypedArray()
    }

    private fun arrayElementType(typeName: String): String =
        when (typeName[1]) {
            'Z' -> "boolean"
            'B' -> "byte"
            'C' -> "char"
            'S' -> "short"
            'I' -> "int"
            'J' -> "long"
            'F' -> "float"
            'D' -> "double"
            'L' -> typeName.substring(2, typeName.length - 1)
            else -> throw McpError("Unsupported array type '$typeName'.")
        }

    private fun asBoolean(raw: Any): Boolean = when (raw) {
        is Boolean -> raw
        is String -> raw.trim().toBooleanStrictOrNull() ?: throw McpError("Expected true or false, got \"$raw\".")
        else -> throw McpError("Expected a boolean, got \"$raw\".")
    }

    private fun asLong(raw: Any): Long = when (raw) {
        is Number -> raw.toLong()
        is String -> raw.trim().toLong()
        else -> throw McpError("Expected a number, got \"$raw\".")
    }

    private fun asDouble(raw: Any): Double = when (raw) {
        is Number -> raw.toDouble()
        is String -> raw.trim().toDouble()
        else -> throw McpError("Expected a number, got \"$raw\".")
    }
}
