package com.jvmguard.ui.views.data.mbeans

import com.jvmguard.ui.JvmGuardBrowserlessTest
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.textfield.TextField
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import javax.management.ObjectName
import javax.management.openmbean.ArrayType
import javax.management.openmbean.SimpleType

class MBeanValueFieldTest : JvmGuardBrowserlessTest() {

    private fun field(openType: javax.management.openmbean.OpenType<*>, type: String, initial: Any?) =
        MBeanValueField(ValueEditSpec(openType, type, "value", initial))

    private fun MBeanValueField.type(text: String) = (component as TextField).apply { value = text }

    @Test
    fun parsesIntegers() {
        val field = field(SimpleType.INTEGER, "java.lang.Integer", 7)
        field.type("42")
        assertEquals(42, field.readValue())
    }

    @Test
    fun parsesLongsAndDoubles() {
        assertEquals(9000000000L, field(SimpleType.LONG, "java.lang.Long", 0L).apply { type("9000000000") }.readValue())
        assertEquals(3.5, field(SimpleType.DOUBLE, "java.lang.Double", 0.0).apply { type("3.5") }.readValue())
    }

    @Test
    fun reportsUnparseableNumbers() {
        val field = field(SimpleType.INTEGER, "java.lang.Integer", 0)
        field.type("not-a-number")
        assertThrows(MBeanConversionException::class.java) { field.readValue() }
    }

    @Test
    fun booleansUseACheckbox() {
        val field = field(SimpleType.BOOLEAN, "java.lang.Boolean", true)
        assertEquals(true, (field.component as Checkbox).value)
        field.component.value = false
        assertEquals(false, field.readValue())
    }

    @Test
    fun stringsPassThroughAndEmptyStaysEmpty() {
        assertEquals("hello", field(SimpleType.STRING, "java.lang.String", "x").apply { type("hello") }.readValue())
        assertEquals("", field(SimpleType.STRING, "java.lang.String", "x").apply { type("") }.readValue())
    }

    @Test
    fun nullableNonStringEmptyBecomesNull() {
        assertNull(field(SimpleType.INTEGER, "java.lang.Integer", 1).apply { type("") }.readValue())
    }

    @Test
    fun primitiveEmptyIsRejected() {
        val field = field(SimpleType.INTEGER, "int", 1)
        field.type("")
        assertThrows(MBeanConversionException::class.java) { field.readValue() }
    }

    @Test
    fun parsesObjectNames() {
        val field = field(SimpleType.OBJECTNAME, ObjectName::class.java.name, null)
        field.type("java.lang:type=Memory")
        assertEquals(ObjectName("java.lang:type=Memory"), field.readValue())
    }

    @Test
    fun stringArraysRoundTripViaSemicolons() {
        val arrayType = ArrayType<Array<String>>(1, SimpleType.STRING)
        val field = field(arrayType, "[Ljava.lang.String;", arrayOf("a", "b"))
        assertEquals("a;b", (field.component as TextField).value)
        field.type("x;y;z")
        assertEquals(listOf("x", "y", "z"), (field.readValue() as Array<*>).toList())
    }
}
