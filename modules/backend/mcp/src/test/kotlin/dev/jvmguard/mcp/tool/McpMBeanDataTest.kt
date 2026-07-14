package dev.jvmguard.mcp.tool

import dev.jvmguard.mbean.common.CompositeDataWithType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import javax.management.ImmutableDescriptor
import javax.management.MBeanAttributeInfo
import javax.management.MBeanInfo
import javax.management.MBeanOperationInfo
import javax.management.MBeanParameterInfo
import javax.management.openmbean.CompositeType
import javax.management.openmbean.OpenType
import javax.management.openmbean.SimpleType
import javax.management.openmbean.TabularType

class McpMBeanDataTest {

    @Test
    fun compositeDecodesToNestedMap() {
        val storeInfoType = CompositeType(
            "StoreInfo", "StoreInfo",
            arrayOf("name", "openStores"),
            arrayOf("name", "openStores"),
            arrayOf<OpenType<*>>(SimpleType.STRING, SimpleType.INTEGER),
        )
        // Values are positional to the (sorted) key set: name, openStores.
        val value = CompositeDataWithType(storeInfoType, arrayOf<Any?>("Cymbal Boutique", 3))

        val decoded = McpMBeanData.decodeAttribute(attribute("StoreInfo"), value)

        assertEquals(mapOf("name" to "Cymbal Boutique", "openStores" to 3), decoded)
    }

    @Test
    fun stringArrayDecodesToList() {
        val decoded = McpMBeanData.decodeAttribute(attribute("Regions"), arrayOf<Any?>("US", "EU", "APAC"))

        assertEquals(listOf("US", "EU", "APAC"), decoded)
    }

    @Test
    fun simpleKeyTabularDecodesToMap() {
        val rowType = CompositeType(
            "row", "row",
            arrayOf("key", "value"),
            arrayOf("key", "value"),
            arrayOf<OpenType<*>>(SimpleType.STRING, SimpleType.INTEGER),
        )
        val tabularType = TabularType("revenueByRegion", "revenueByRegion", rowType, arrayOf("key"))
        val rows = arrayOf<Any?>(
            CompositeDataWithType(rowType, arrayOf<Any?>("US", 184000)),
            CompositeDataWithType(rowType, arrayOf<Any?>("EU", 121500)),
        )

        val decoded = McpMBeanData.decodeAttribute(attribute("revenueByRegion", tabularType), rows)

        assertEquals(mapOf("US" to 184000, "EU" to 121500), decoded)
    }

    @Test
    fun operationSignaturesAreCompact() {
        val beanInfo = MBeanInfo(
            "dev.jvmguard.demo.DemoService", "",
            emptyArray(),
            null,
            arrayOf(
                MBeanOperationInfo(
                    "listStores", "",
                    arrayOf(MBeanParameterInfo("count", "int", "")),
                    "java.util.List", MBeanOperationInfo.INFO,
                ),
                MBeanOperationInfo("refreshStats", "", emptyArray(), "void", MBeanOperationInfo.ACTION),
            ),
            null,
        )

        assertEquals(listOf("listStores(int)", "refreshStats()"), McpMBeanData.operationSignatures(beanInfo))
    }

    private fun attribute(name: String, openType: OpenType<*>? = null): MBeanAttributeInfo {
        val descriptor = openType?.let { ImmutableDescriptor(arrayOf("openType"), arrayOf<Any>(it)) }
        return MBeanAttributeInfo(name, "java.lang.Object", "", true, false, false, descriptor)
    }
}
