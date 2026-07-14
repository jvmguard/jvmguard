package dev.jvmguard.mcp.tool

import dev.jvmguard.mcp.McpError
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigInteger
import javax.management.ObjectName

class McpMBeanValuesTest {

    @Test
    fun coercesNumbersFromJsonNumbersAndStrings() {
        assertEquals(5, McpMBeanValues.coerce("int", 5))
        assertEquals(5, McpMBeanValues.coerce("int", "5"))
        assertEquals(5L, McpMBeanValues.coerce("long", 5))
        assertEquals(2.5, McpMBeanValues.coerce("double", 2.5))
        assertEquals(2.5f, McpMBeanValues.coerce("float", "2.5"))
        assertEquals(7.toByte(), McpMBeanValues.coerce("byte", 7))
        assertEquals(BigInteger("42"), McpMBeanValues.coerce("java.math.BigInteger", "42"))
    }

    @Test
    fun coercesBooleansStringsCharsAndObjectNames() {
        assertEquals(true, McpMBeanValues.coerce("boolean", true))
        assertEquals(false, McpMBeanValues.coerce("java.lang.Boolean", "false"))
        assertEquals("hi", McpMBeanValues.coerce("java.lang.String", "hi"))
        assertEquals('x', McpMBeanValues.coerce("char", "x"))
        assertEquals(ObjectName.getInstance("java.lang:type=Memory"),
            McpMBeanValues.coerce("javax.management.ObjectName", "java.lang:type=Memory"))
    }

    @Test
    fun nullIsAllowedForBoxedButNotPrimitiveTypes() {
        assertNull(McpMBeanValues.coerce("java.lang.String", null))
        assertNull(McpMBeanValues.coerce("java.lang.Integer", null))
        assertThrows(McpError::class.java) { McpMBeanValues.coerce("int", null) }
    }

    @Test
    fun coercesOneDimensionalSimpleTypeArraysFromJsonLists() {
        assertArrayEquals(arrayOf(1, 2, 3), McpMBeanValues.coerce("[I", listOf(1, 2, 3)) as Array<*>)
        assertArrayEquals(arrayOf(1L, 2L), McpMBeanValues.coerce("[J", listOf("1", 2)) as Array<*>)
        assertArrayEquals(arrayOf("a", "b"), McpMBeanValues.coerce("[Ljava.lang.String;", listOf("a", "b")) as Array<*>)
        assertArrayEquals(arrayOf(true, false), McpMBeanValues.coerce("[Z", listOf(true, false)) as Array<*>)
        assertNull(McpMBeanValues.coerce("[I", null))
    }

    @Test
    fun rejectsMultiDimAndCompositeArraysAndNonListInput() {
        assertThrows(McpError::class.java) { McpMBeanValues.coerce("[[I", listOf(listOf(1))) }
        assertThrows(McpError::class.java) {
            McpMBeanValues.coerce("[Ljavax.management.openmbean.CompositeData;", listOf(mapOf("a" to 1)))
        }
        assertThrows(McpError::class.java) { McpMBeanValues.coerce("[I", "1,2,3") }
    }

    @Test
    fun unsupportedTypesAndBadValuesRaiseMcpError() {
        assertThrows(McpError::class.java) { McpMBeanValues.coerce("javax.management.openmbean.CompositeData", mapOf("a" to 1)) }
        assertThrows(McpError::class.java) { McpMBeanValues.coerce("int", "not-a-number") }
        assertThrows(McpError::class.java) { McpMBeanValues.coerce("boolean", "maybe") }
    }
}
