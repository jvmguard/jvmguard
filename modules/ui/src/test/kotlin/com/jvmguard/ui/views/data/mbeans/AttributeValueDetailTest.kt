package com.jvmguard.ui.views.data.mbeans

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import javax.management.openmbean.OpenType
import javax.management.openmbean.SimpleType

class AttributeValueDetailTest {

    private fun node(value: Any?, openType: OpenType<*>?) = AttributeNode("attr", value, openType)

    // Every scalar string gets a "show" affordance regardless of length; visibility is driven by client-side cell overflow.
    @Test
    fun stringValuesGetAShowAffordance() {
        assertNotNull(valueDetailButton(node("short", SimpleType.STRING)))
        assertNotNull(valueDetailButton(node("x".repeat(200), SimpleType.STRING)))
        assertNotNull(valueDetailButton(node("plain", null)), "a String with no open type also qualifies")
    }

    @Test
    fun nonStringAndArrayValuesHaveNoDetail() {
        assertNull(valueDetailButton(node(42, SimpleType.INTEGER)))
        assertNull(valueDetailButton(node(arrayOf("x".repeat(200)), null)))
    }
}
