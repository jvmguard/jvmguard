package dev.jvmguard.integration.tests.jvmguard.mbean

import dev.jvmguard.agent.config.telemetry.MBeanLineConfig
import dev.jvmguard.agent.config.telemetry.MBeanTelemetryConfig
import dev.jvmguard.agent.config.telemetry.TelemetryUnit
import dev.jvmguard.agent.mbean.MBeanData
import dev.jvmguard.integration.JvmGuardTest
import dev.jvmguard.integration.Controller
import dev.jvmguard.integration.TestServerConnection
import dev.jvmguard.integration.TestVmManager
import dev.jvmguard.integration.tests.jvmguard.mbean.mxbeans.Complex1Sub
import dev.jvmguard.integration.tests.jvmguard.mbean.standard.StandardComponent
import dev.jvmguard.data.config.GroupConfig
import dev.jvmguard.data.vmdata.VM
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*
import javax.management.AttributeChangeNotification
import javax.management.ObjectName
import javax.management.ReflectionException
import javax.management.openmbean.*
import org.junit.jupiter.api.Tag

@Tag("citest")
@Suppress("UNCHECKED_CAST")
class MBeanTest : JvmGuardTest() {

    override fun modifyInitialRootConfig(rootConfig: GroupConfig) {
        rootConfig.telemetrySettings.mbeanTelemetries.add(MBeanTelemetryConfig("mbean tel1", TelemetryUnit.BYTES, 0, true, false).apply {
            lines.add(MBeanLineConfig("dev.jvmguard.test:type=Child", "Complex/test1", ""))
        })
    }

    override fun connect(vmManager: TestVmManager, serverConnection: TestServerConnection, controller: Controller) {
        val vm = waitForConnection(serverConnection, listOf("JVM")).first()
        sleep(1000 * 10)
        val mBeanNames = serverConnection.getMBeanNames(vm, false).filter { it.startsWith(MBeanWorkload.COM_JVMGUARD_TEST) }
        assertEqual(
            mBeanNames.sorted(),
            listOf(
                MBeanWorkload.CHILD_BEAN_NAME,
                MBeanWorkload.DYNAMIC_BEAN_NAME,
                MBeanWorkload.STANDARD_BEAN_NAME,
                MBeanWorkload.PARENT_BEAN_NAME,
                MBeanWorkload.TEST2_BEAN_NAME
            ).sorted()
        )

        checkDynamic(serverConnection, vm)
        checkStandard(serverConnection, vm)
        checkTest2(serverConnection, vm)
        checkTest1(serverConnection, vm)
    }

    private fun checkTest1(serverConnection: TestServerConnection, vm: VM) {
        var mBeanData = serverConnection.getMBeanData(vm, MBeanWorkload.CHILD_BEAN_NAME, true, true)

        var operationInfo = mBeanData.beanInfo.operations.first { it.name == "add" }
        var compositeData = serverConnection.invokeMBeanOperation(
            vm,
            MBeanWorkload.CHILD_BEAN_NAME,
            operationInfo,
            arrayOf<Any?>(40, 10, arrayOf<Any?>("hallo1", "hallo2"), intArrayOf(1000, 2000, 3000))
        ).returnValue
        assertTrue(compositeData != null)
        assertEqual(getCompositeType(compositeData).typeName, Complex1Sub::class.java.name)
        assertEqual(getCompositeType(compositeData).keySet(), sortedSetOf("sub1", "sub2"))
        assertEqual(getCompositeValues(compositeData)[0], 40 + 10 + 1000)
        assertEqual(getCompositeValues(compositeData)[1], "2 hallo1")

        compositeData = serverConnection.invokeMBeanOperation(
            vm,
            MBeanWorkload.CHILD_BEAN_NAME,
            operationInfo,
            arrayOf<Any?>(50, 10, arrayOf<Any?>("hallo1", "hallo2"), arrayOf<Any?>(1100, 2000, 3000))
        ).returnValue
        assertTrue(compositeData != null)
        assertEqual(getCompositeType(compositeData).typeName, Complex1Sub::class.java.name)
        assertEqual(getCompositeType(compositeData).keySet(), sortedSetOf("sub1", "sub2"))
        assertEqual(getCompositeValues(compositeData)[0], 50 + 10 + 1100)
        assertEqual(getCompositeValues(compositeData)[1], "2 hallo1")

        operationInfo = mBeanData.beanInfo.operations.first { it.name == "add2" }
        val objectArray = serverConnection.invokeMBeanOperation(
            vm,
            MBeanWorkload.CHILD_BEAN_NAME,
            operationInfo,
            arrayOf<Any?>(arrayOf<Any?>(arrayOf<Any?>("hallo1", "hallo2")), arrayOf<Any?>(intArrayOf(1000, 2000, 3000), intArrayOf(1001, 2001, 3001)))
        ).returnValue
        assertEqual(objectArray, arrayOf<Any>(1000, 2000, 3000))

        operationInfo = mBeanData.beanInfo.operations.first { it.name == "add3" }
        assertEqual(operationInfo.descriptor?.getFieldValue(OPEN_TYPE), SimpleType.STRING)
        assertEqual(
            operationInfo.signature.map { it.descriptor?.getFieldValue(OPEN_TYPE) },
            listOf(
                SimpleType.BOOLEAN,
                SimpleType.BYTE,
                SimpleType.SHORT,
                SimpleType.CHARACTER,
                SimpleType.INTEGER,
                SimpleType.FLOAT,
                SimpleType.LONG,
                SimpleType.DOUBLE,
                SimpleType.STRING,
                SimpleType.BIGDECIMAL,
                SimpleType.BIGINTEGER,
                SimpleType.DATE,
                SimpleType.STRING
            )
        )
        assertEqual(operationInfo.signature.map { it.name }, ((0..12).map { "p$it" }))
        val parameter = arrayOf<Any?>(
            true,
            java.lang.Byte.valueOf("2"),
            java.lang.Short.valueOf("3"),
            '4',
            5,
            6.toFloat(),
            7L,
            8.0,
            "9",
            BigDecimal("10.01"),
            BigInteger("11"),
            Date(1000000),
            null
        )
        val string = serverConnection.invokeMBeanOperation(vm, MBeanWorkload.CHILD_BEAN_NAME, operationInfo, parameter).returnValue as String
        try {
            assertEqual(string, "ret " + parameter.joinToString(""))
        } catch (_: AssertionError) {
            assertEqual(string.replace("GMT+01:00", "CET"), "ret " + parameter.joinToString(""))
        }

        var values = mBeanData.values
        val attributes = mBeanData.beanInfo.attributes

        val compositeType = mBeanData.beanInfo.attributes[findIndex(mBeanData, "Measure")].descriptor.getFieldValue(OPEN_TYPE) as CompositeType
        checkMeasureBase(compositeType)
        compositeData = values[findIndex(mBeanData, "Measure")]
        checkMeasureFull(getCompositeType(compositeData), SimpleType.DOUBLE)
        assertEqual(getCompositeValues(compositeData)[0], "double")
        assertEqual(getCompositeValues(compositeData)[1], "ms")
        assertEqual(getCompositeValues(compositeData)[2], 3.3)

        assertEqual(values[findIndex(mBeanData, "Parent", SimpleType.OBJECTNAME)], ObjectName.getInstance(MBeanWorkload.PARENT_BEAN_NAME))

        assertEqual(
            serverConnection.setMBeanAttribute(
                vm,
                MBeanWorkload.CHILD_BEAN_NAME,
                attributes.find { it.name == "Parent" },
                ObjectName.getInstance(MBeanWorkload.CHILD_BEAN_NAME)
            ).errorMessage, null
        )
        mBeanData = serverConnection.getMBeanData(vm, MBeanWorkload.CHILD_BEAN_NAME, true, true)
        values = mBeanData.values
        assertEqual(values[findIndex(mBeanData, "Parent", SimpleType.OBJECTNAME)], ObjectName.getInstance(MBeanWorkload.CHILD_BEAN_NAME))

        assertEqual(values[findIndex(mBeanData, "LongNull", SimpleType.LONG)], null)
        assertEqual(values[findIndex(mBeanData, "ComplexNull")], null)
        checkComplexOne(mBeanData.beanInfo.attributes[findIndex(mBeanData, "ComplexNull")].descriptor.getFieldValue(OPEN_TYPE) as CompositeType)

        checkComplexOne(mBeanData.beanInfo.attributes[findIndex(mBeanData, "Complex")].descriptor.getFieldValue(OPEN_TYPE) as CompositeType)
        checkComplexOneValues(values[findIndex(mBeanData, "Complex")] as Array<Any?>)

        assertEqual(values[findIndex(mBeanData, "ShortString", SimpleType.STRING)], "short")
        assertTrue((values[findIndex(mBeanData, "LongString", SimpleType.STRING)] as String).startsWith("1234567890123"))
        assertEqual((values[findIndex(mBeanData, "LongString", SimpleType.STRING)] as String).length, 80000)
        assertEqual(
            values[findIndex(mBeanData, "PrimitiveArray2", ArrayType<Any>(1, ArrayType<Any>(SimpleType.LONG, true)))],
            arrayOf<Any>(arrayOf<Any>(1L, 2L, 3L), arrayOf<Any>(3L, 4L, 5L))
        )
        assertEqual(values[findIndex(mBeanData, "ComplexArray")], arrayOf<Any>(arrayOf<Any?>(1L, "test"), arrayOf<Any?>(2L, "test2")))
        assertEqual(values[findIndex(mBeanData, "ComplexList")], values[findIndex(mBeanData, "ComplexArray")])
        checkComplexSub(
            (mBeanData.beanInfo.attributes[findIndex(
                mBeanData,
                "ComplexArray"
            )].descriptor.getFieldValue(OPEN_TYPE) as ArrayType<*>).elementOpenType as CompositeType
        )
        checkComplexSub(
            (mBeanData.beanInfo.attributes[findIndex(
                mBeanData,
                "ComplexList"
            )].descriptor.getFieldValue(OPEN_TYPE) as ArrayType<*>).elementOpenType as CompositeType
        )

        var tabularType = mBeanData.beanInfo.attributes[findIndex(mBeanData, "Map5")].descriptor.getFieldValue(OPEN_TYPE) as TabularType
        assertEqual(tabularType.indexNames, listOf("key"))
        assertEqual(tabularType.rowType.keySet(), sortedSetOf("key", "value"))
        checkMeasureBase(tabularType.rowType.getType("key") as CompositeType)
        checkComplexOne(tabularType.rowType.getType("value") as CompositeType)

        assertEqual((values[findIndex(mBeanData, "Map5")] as Array<Any?>).size, 1)
        compositeData = ((values[findIndex(mBeanData, "Map5")] as Array<Any?>)[0] as Array<Any?>)[0]
        checkMeasureFull(getCompositeType(compositeData), SimpleType.LONG)
        checkComplexOneValues(((values[findIndex(mBeanData, "Map5")] as Array<Any?>)[0] as Array<Any?>)[1] as Array<Any?>)

        tabularType = mBeanData.beanInfo.attributes[findIndex(mBeanData, "Map4")].descriptor.getFieldValue(OPEN_TYPE) as TabularType
        assertEqual(tabularType.indexNames, listOf("key"))
        assertEqual(tabularType.rowType.keySet(), sortedSetOf("key", "value"))
        assertEqual(tabularType.rowType.getType("key"), SimpleType.STRING)
        checkComplexSub(tabularType.rowType.getType("value") as CompositeType)

        val map = values[findIndex(mBeanData, "Map4")] as Array<Any?>
        assertEqual(map.size, 2)
        assertEqual((map.first { (it as Array<Any?>)[0] == "test" } as Array<Any?>)[1], arrayOf<Any?>(1L, "test"))
        assertEqual((map.first { (it as Array<Any?>)[0] == "test2" } as Array<Any?>)[1], arrayOf<Any?>(2L, "test2"))
    }

    private fun checkComplexOneValues(values: Array<Any?>) {
        assertEqual(values.size, 8)

        var map = values[0] as Array<Any?>
        assertEqual(map.size, 2)

        var entry = map.first { ((it as Array<Any?>)[0] as Array<Any?>)[0] == 1L } as Array<Any?>
        assertEqual(entry[0], arrayOf<Any?>(1L, "keya1"))
        assertEqual(entry[1], arrayOf<Any?>(1L, "a1"))
        entry = map.first { ((it as Array<Any?>)[0] as Array<Any?>)[0] == 2L } as Array<Any?>
        assertEqual(entry[0], arrayOf<Any?>(2L, "keya2"))
        assertEqual(entry[1], arrayOf<Any?>(2L, "a2"))

        val compositeData = values[1]!!
        checkMeasureFull(getCompositeType(compositeData), SimpleType.LONG)
        assertEqual(getCompositeValues(compositeData), arrayOf<Any?>("long", "bytes", 33L))

        map = values[2] as Array<Any?>
        assertEqual(map.size, 2)
        entry = map.first { (it as Array<Any?>)[0] == "keya1" } as Array<Any?>
        assertEqual(entry[1], arrayOf<Any?>(1L, "a1"))
        entry = map.first { (it as Array<Any?>)[0] == "keya2" } as Array<Any?>
        assertEqual(entry[1], arrayOf<Any?>(2L, "a2"))

        assertEqual(values[3], arrayOf<Any?>(1L, "single"))
        assertEqual(values[4], arrayOf<Any>(arrayOf<Any?>(1L, "a1"), arrayOf<Any?>(2L, "a2")))
        assertEqual(values[5], values[4])
        assertEqual(values[6], 1L)
        assertEqual(values[7], arrayOf<Any>(1L, 10L, 100L))
    }


    private fun checkMeasureFull(compositeType: CompositeType, valueType: SimpleType<*>) {
        assertEqual(compositeType.keySet(), sortedSetOf("description", "units", "value"))
        assertEqual(compositeType.getType("description"), SimpleType.STRING)
        assertEqual(compositeType.getType("units"), SimpleType.STRING)
        assertEqual(compositeType.getType("value"), valueType)
    }

    private fun checkComplexOne(compositeType: CompositeType) {
        assertEqual(compositeType.keySet(), sortedSetOf("test1", "test2", "sub", "subArray", "subList", "doubleMap", "singleMap", "measure"))
        assertEqual(compositeType.getType("test1"), SimpleType.LONG)
        assertEqual(compositeType.getType("test2"), ArrayType<Any>(SimpleType.LONG, true))
        checkComplexSub(compositeType.getType("sub") as CompositeType)
        var arrayType = compositeType.getType("subArray") as ArrayType<*>
        assertEqual(arrayType.dimension, 1)
        checkComplexSub(arrayType.elementOpenType as CompositeType)
        arrayType = compositeType.getType("subList") as ArrayType<*>
        assertEqual(arrayType.dimension, 1)
        checkComplexSub(arrayType.elementOpenType as CompositeType)

        var tabularType = compositeType.getType("doubleMap") as TabularType
        assertEqual(tabularType.indexNames, listOf("key"))
        assertEqual(tabularType.rowType.keySet(), sortedSetOf("key", "value"))
        checkComplexSub(tabularType.rowType.getType("key") as CompositeType)
        checkComplexSub(tabularType.rowType.getType("value") as CompositeType)

        tabularType = compositeType.getType("singleMap") as TabularType
        assertEqual(tabularType.indexNames, listOf("key"))
        assertEqual(tabularType.rowType.keySet(), sortedSetOf("key", "value"))
        assertEqual(tabularType.rowType.getType("key"), SimpleType.STRING)
        checkComplexSub(tabularType.rowType.getType("value") as CompositeType)

        checkMeasureBase(compositeType.getType("measure") as CompositeType)
    }

    private fun checkMeasureBase(compositeType: CompositeType) {
        assertEqual(compositeType.keySet(), sortedSetOf("units"))
        assertEqual(compositeType.getType("units"), SimpleType.STRING)
    }

    private fun checkComplexSub(compositeType: CompositeType) {
        assertEqual(compositeType.keySet(), sortedSetOf("sub1", "sub2"))
        assertEqual(compositeType.getType("sub1"), SimpleType.LONG)
        assertEqual(compositeType.getType("sub2"), SimpleType.STRING)
    }


    private fun checkTest2(serverConnection: TestServerConnection, vm: VM) {
        var mBeanData = serverConnection.getMBeanData(vm, MBeanWorkload.TEST2_BEAN_NAME, true, true)
        var values = mBeanData.values
        val attributes = mBeanData.beanInfo.attributes
        assertEqual(attributes.size, 26)

        assertEqual(values[findIndex(mBeanData, "BooleanVal", SimpleType.BOOLEAN)], true)
        assertEqual(values[findIndex(mBeanData, "ByteVal", SimpleType.BYTE)], 3)
        assertEqual(values[findIndex(mBeanData, "CharVal", SimpleType.CHARACTER)], 'A')
        assertEqual(values[findIndex(mBeanData, "ShortVal", SimpleType.SHORT)], 3)
        assertEqual(values[findIndex(mBeanData, "IntVal", SimpleType.INTEGER)], 4)
        assertEqual(values[findIndex(mBeanData, "FloatVal", SimpleType.FLOAT)], 5.5.toFloat())
        assertEqual(values[findIndex(mBeanData, "LongVal", SimpleType.LONG)], 6)
        assertEqual(values[findIndex(mBeanData, "DoubleVal", SimpleType.DOUBLE)], 7.7)
        assertEqual(values[findIndex(mBeanData, "String", SimpleType.STRING)], "test")
        assertEqual(values[findIndex(mBeanData, "Date", SimpleType.DATE)], Date(10000))
        assertEqual(values[findIndex(mBeanData, "BigInteger", SimpleType.BIGINTEGER)], BigInteger("123"))
        assertEqual(values[findIndex(mBeanData, "BigDecimal", SimpleType.BIGDECIMAL)], BigDecimal("123.33"))

        assertEqual(values[findIndex(mBeanData, "BooleanVal2", SimpleType.BOOLEAN)], true)
        assertEqual(values[findIndex(mBeanData, "ByteVal2", SimpleType.BYTE)], 3)
        assertEqual(values[findIndex(mBeanData, "CharVal2", SimpleType.CHARACTER)], 'A')
        assertEqual(values[findIndex(mBeanData, "ShortVal2", SimpleType.SHORT)], 3)
        assertEqual(values[findIndex(mBeanData, "IntVal2", SimpleType.INTEGER)], 4)
        assertEqual(values[findIndex(mBeanData, "FloatVal2", SimpleType.FLOAT)], 5.5.toFloat())
        assertEqual(values[findIndex(mBeanData, "LongVal2", SimpleType.LONG)], 6)
        assertEqual(values[findIndex(mBeanData, "DoubleVal2", SimpleType.DOUBLE)], 7.7)

        assertEqual(values[findIndex(mBeanData, "IntArray", ArrayType<Any>(SimpleType.INTEGER, true))], arrayOf<Any>(1, 2, 3))
        assertEqual(values[findIndex(mBeanData, "LongArray", ArrayType<Any>(SimpleType.LONG, true))], arrayOf<Any>(1L, 2L, 3L))
        assertEqual(values[findIndex(mBeanData, "DoubleArray", ArrayType<Any>(SimpleType.DOUBLE, true))], arrayOf<Any>(1.1, 2.2, 3.3))
        assertEqual(values[findIndex(mBeanData, "StringArray", ArrayType<Any>(1, SimpleType.STRING))], arrayOf<Any?>("test1", null, "test2"))
        assertEqual(
            values[findIndex(mBeanData, "BigDecimalArray", ArrayType<Any>(1, SimpleType.BIGDECIMAL))],
            arrayOf<Any>(BigDecimal("1.1"), BigDecimal("2.2"))
        )
        assertEqual(values[findIndex(mBeanData, "LongArray2", ArrayType<Any>(1, SimpleType.LONG))], arrayOf<Any?>(1L, null, 3L))

        assertEqual(attributes.first { it.name == "LongArray2" }.type, "[Ljava.lang.Long;")
        assertEqual(attributes.first { it.name == "LongArray" }.type, "[J")
        assertEqual(attributes.first { it.name == "DoubleVal2" }.type, Double::class.javaObjectType.name)
        assertEqual(attributes.first { it.name == "DoubleVal" }.type, Double::class.java.name)

        assertEqual(serverConnection.setMBeanAttribute(vm, MBeanWorkload.TEST2_BEAN_NAME, attributes.find { it.name == "LongArray2" }, null).errorMessage, null)
        assertEqual(serverConnection.setMBeanAttribute(vm, MBeanWorkload.TEST2_BEAN_NAME, attributes.find { it.name == "LongArray" }, null).errorMessage, null)
        assertEqual(
            serverConnection.setMBeanAttribute(
                vm,
                MBeanWorkload.TEST2_BEAN_NAME,
                attributes.find { it.name == "LongArray" },
                arrayOf<Any?>(1L, null, 3L)
            ).errorMessage, "java.lang.IllegalArgumentException"
        )

        assertEqual(serverConnection.setMBeanAttribute(vm, MBeanWorkload.TEST2_BEAN_NAME, attributes.find { it.name == "DoubleVal2" }, null).errorMessage, null)
        assertEqual(
            serverConnection.setMBeanAttribute(vm, MBeanWorkload.TEST2_BEAN_NAME, attributes.find { it.name == "DoubleVal" }, null).errorMessage,
            "Invalid value for attribute DoubleVal: null"
        ) {
            println(serverConnection.setMBeanAttribute(vm, MBeanWorkload.TEST2_BEAN_NAME, attributes.find { it.name == "DoubleVal" }, null).stackTrace)
        }

        mBeanData = serverConnection.getMBeanData(vm, MBeanWorkload.TEST2_BEAN_NAME, true, true)
        values = mBeanData.values
        assertEqual(values[findIndex(mBeanData, "DoubleVal", SimpleType.DOUBLE)], 7.7)
        assertEqual(values[findIndex(mBeanData, "DoubleVal2", SimpleType.DOUBLE)], null)
        assertEqual(values[findIndex(mBeanData, "LongArray2")], null)
        assertEqual(values[findIndex(mBeanData, "LongArray")], null)

        assertEqual(
            serverConnection.setMBeanAttribute(
                vm,
                MBeanWorkload.TEST2_BEAN_NAME,
                attributes.find { it.name == "LongArray2" },
                arrayOf<Any>()
            ).errorMessage, null
        )
        assertEqual(
            serverConnection.setMBeanAttribute(
                vm,
                MBeanWorkload.TEST2_BEAN_NAME,
                attributes.find { it.name == "LongArray" },
                arrayOf<Any>()
            ).errorMessage, null
        )

        mBeanData = serverConnection.getMBeanData(vm, MBeanWorkload.TEST2_BEAN_NAME, true, true)
        values = mBeanData.values

        assertEqual(values[findIndex(mBeanData, "LongArray2")], arrayOf<Any>())
        assertEqual(values[findIndex(mBeanData, "LongArray")], arrayOf<Any>())

        assertEqual(serverConnection.setMBeanAttribute(vm, MBeanWorkload.TEST2_BEAN_NAME, attributes.find { it.name == "BooleanVal" }, false).errorMessage, null)
        assertEqual(
            serverConnection.setMBeanAttribute(vm, MBeanWorkload.TEST2_BEAN_NAME, attributes.find { it.name == "ByteVal" }, 5.toByte()).errorMessage,
            null
        )
        assertEqual(serverConnection.setMBeanAttribute(vm, MBeanWorkload.TEST2_BEAN_NAME, attributes.find { it.name == "CharVal" }, 'B').errorMessage, null)
        assertEqual(
            serverConnection.setMBeanAttribute(vm, MBeanWorkload.TEST2_BEAN_NAME, attributes.find { it.name == "ShortVal" }, 13.toShort()).errorMessage,
            null
        )
        assertEqual(serverConnection.setMBeanAttribute(vm, MBeanWorkload.TEST2_BEAN_NAME, attributes.find { it.name == "IntVal" }, 14).errorMessage, null)
        assertEqual(
            serverConnection.setMBeanAttribute(
                vm,
                MBeanWorkload.TEST2_BEAN_NAME,
                attributes.find { it.name == "FloatVal" },
                15.5.toFloat()
            ).errorMessage, null
        )
        assertEqual(serverConnection.setMBeanAttribute(vm, MBeanWorkload.TEST2_BEAN_NAME, attributes.find { it.name == "LongVal" }, 16L).errorMessage, null)
        assertEqual(serverConnection.setMBeanAttribute(vm, MBeanWorkload.TEST2_BEAN_NAME, attributes.find { it.name == "DoubleVal" }, 17.7).errorMessage, null)
        assertEqual(serverConnection.setMBeanAttribute(vm, MBeanWorkload.TEST2_BEAN_NAME, attributes.find { it.name == "String" }, "test2").errorMessage, null)
        assertEqual(serverConnection.setMBeanAttribute(vm, MBeanWorkload.TEST2_BEAN_NAME, attributes.find { it.name == "Date" }, Date(20000)).errorMessage, null)
        assertEqual(
            serverConnection.setMBeanAttribute(
                vm,
                MBeanWorkload.TEST2_BEAN_NAME,
                attributes.find { it.name == "BigInteger" },
                BigInteger("234")
            ).errorMessage, null
        )
        assertEqual(
            serverConnection.setMBeanAttribute(
                vm,
                MBeanWorkload.TEST2_BEAN_NAME,
                attributes.find { it.name == "BigDecimal" },
                BigDecimal("344.444")
            ).errorMessage, null
        )

        assertEqual(
            serverConnection.setMBeanAttribute(vm, MBeanWorkload.TEST2_BEAN_NAME, attributes.find { it.name == "BooleanVal2" }, false).errorMessage,
            null
        )
        assertEqual(
            serverConnection.setMBeanAttribute(vm, MBeanWorkload.TEST2_BEAN_NAME, attributes.find { it.name == "ByteVal2" }, 6.toByte()).errorMessage,
            null
        )
        assertEqual(serverConnection.setMBeanAttribute(vm, MBeanWorkload.TEST2_BEAN_NAME, attributes.find { it.name == "CharVal2" }, 'C').errorMessage, null)
        assertEqual(
            serverConnection.setMBeanAttribute(vm, MBeanWorkload.TEST2_BEAN_NAME, attributes.find { it.name == "ShortVal2" }, 14.toShort()).errorMessage,
            null
        )
        assertEqual(serverConnection.setMBeanAttribute(vm, MBeanWorkload.TEST2_BEAN_NAME, attributes.find { it.name == "IntVal2" }, 15).errorMessage, null)
        assertEqual(
            serverConnection.setMBeanAttribute(
                vm,
                MBeanWorkload.TEST2_BEAN_NAME,
                attributes.find { it.name == "FloatVal2" },
                16.5.toFloat()
            ).errorMessage, null
        )
        assertEqual(serverConnection.setMBeanAttribute(vm, MBeanWorkload.TEST2_BEAN_NAME, attributes.find { it.name == "LongVal2" }, 17L).errorMessage, null)
        assertEqual(serverConnection.setMBeanAttribute(vm, MBeanWorkload.TEST2_BEAN_NAME, attributes.find { it.name == "DoubleVal2" }, 18.7).errorMessage, null)

        assertEqual(
            serverConnection.setMBeanAttribute(
                vm,
                MBeanWorkload.TEST2_BEAN_NAME,
                attributes.find { it.name == "IntArray" },
                arrayOf<Any?>(3, 4, 5)
            ).errorMessage, null
        )
        assertEqual(
            serverConnection.setMBeanAttribute(
                vm,
                MBeanWorkload.TEST2_BEAN_NAME,
                attributes.find { it.name == "LongArray" },
                arrayOf<Any?>(6L)
            ).errorMessage, null
        )
        assertEqual(
            serverConnection.setMBeanAttribute(
                vm,
                MBeanWorkload.TEST2_BEAN_NAME,
                attributes.find { it.name == "DoubleArray" },
                arrayOf<Any?>(3.3, 3.3, 3.3, 4.4)
            ).errorMessage, null
        )
        assertEqual(
            serverConnection.setMBeanAttribute(
                vm,
                MBeanWorkload.TEST2_BEAN_NAME,
                attributes.find { it.name == "StringArray" },
                arrayOf<Any?>("test22222", null)
            ).errorMessage, null
        )
        assertEqual(
            serverConnection.setMBeanAttribute(
                vm,
                MBeanWorkload.TEST2_BEAN_NAME,
                attributes.find { it.name == "BigDecimalArray" },
                arrayOf<Any?>(BigDecimal("555.5555"))
            ).errorMessage, null
        )
        assertEqual(
            serverConnection.setMBeanAttribute(
                vm,
                MBeanWorkload.TEST2_BEAN_NAME,
                attributes.find { it.name == "LongArray2" },
                arrayOf<Any?>(null, 3L, 300L)
            ).errorMessage, null
        )

        mBeanData = serverConnection.getMBeanData(vm, MBeanWorkload.TEST2_BEAN_NAME, true, true)
        values = mBeanData.values

        assertEqual(values[findIndex(mBeanData, "BooleanVal", SimpleType.BOOLEAN)], false)
        assertEqual(values[findIndex(mBeanData, "ByteVal", SimpleType.BYTE)], 5)
        assertEqual(values[findIndex(mBeanData, "CharVal", SimpleType.CHARACTER)], 'B')
        assertEqual(values[findIndex(mBeanData, "ShortVal", SimpleType.SHORT)], 13)
        assertEqual(values[findIndex(mBeanData, "IntVal", SimpleType.INTEGER)], 14)
        assertEqual(values[findIndex(mBeanData, "FloatVal", SimpleType.FLOAT)], 15.5.toFloat())
        assertEqual(values[findIndex(mBeanData, "LongVal", SimpleType.LONG)], 16)
        assertEqual(values[findIndex(mBeanData, "DoubleVal", SimpleType.DOUBLE)], 17.7)
        assertEqual(values[findIndex(mBeanData, "String", SimpleType.STRING)], "test2")
        assertEqual(values[findIndex(mBeanData, "Date", SimpleType.DATE)], Date(20000))
        assertEqual(values[findIndex(mBeanData, "BigInteger", SimpleType.BIGINTEGER)], BigInteger("234"))
        assertEqual(values[findIndex(mBeanData, "BigDecimal", SimpleType.BIGDECIMAL)], BigDecimal("344.444"))

        assertEqual(values[findIndex(mBeanData, "BooleanVal2", SimpleType.BOOLEAN)], false)
        assertEqual(values[findIndex(mBeanData, "ByteVal2", SimpleType.BYTE)], 6)
        assertEqual(values[findIndex(mBeanData, "CharVal2", SimpleType.CHARACTER)], 'C')
        assertEqual(values[findIndex(mBeanData, "ShortVal2", SimpleType.SHORT)], 14)
        assertEqual(values[findIndex(mBeanData, "IntVal2", SimpleType.INTEGER)], 15)
        assertEqual(values[findIndex(mBeanData, "FloatVal2", SimpleType.FLOAT)], 16.5.toFloat())
        assertEqual(values[findIndex(mBeanData, "LongVal2", SimpleType.LONG)], 17)
        assertEqual(values[findIndex(mBeanData, "DoubleVal2", SimpleType.DOUBLE)], 18.7)

        assertEqual(values[findIndex(mBeanData, "IntArray", ArrayType<Any>(SimpleType.INTEGER, true))], arrayOf<Any?>(3, 4, 5))
        assertEqual(values[findIndex(mBeanData, "LongArray", ArrayType<Any>(SimpleType.LONG, true))], arrayOf<Any?>(6L))
        assertEqual(values[findIndex(mBeanData, "DoubleArray", ArrayType<Any>(SimpleType.DOUBLE, true))], arrayOf<Any?>(3.3, 3.3, 3.3, 4.4))
        assertEqual(values[findIndex(mBeanData, "StringArray", ArrayType<Any>(1, SimpleType.STRING))], arrayOf<Any?>("test22222", null))
        assertEqual(values[findIndex(mBeanData, "BigDecimalArray", ArrayType<Any>(1, SimpleType.BIGDECIMAL))], arrayOf<Any?>(BigDecimal("555.5555")))
        assertEqual(values[findIndex(mBeanData, "LongArray2", ArrayType<Any>(1, SimpleType.LONG))], arrayOf<Any?>(null, 3L, 300L))

    }

    private fun checkStandard(serverConnection: TestServerConnection, vm: VM) {
        var mBeanData = serverConnection.getMBeanData(vm, MBeanWorkload.STANDARD_BEAN_NAME, true, true)
        assertEqual(mBeanData.beanInfo.notifications.map { it.name }, listOf(AttributeChangeNotification::class.java.name))

        assertEqual(mBeanData.beanInfo.attributes.map { it.name }.sorted(), listOf("ExplicitComposite", "Int", "Standard", "String"))

        assertEqual(mBeanData.values.size, 4)
        assertEqual(mBeanData.values[findIndex(mBeanData, "Int")], 3)
        assertEqual(mBeanData.values[findIndex(mBeanData, "Standard")], "StandardComponent toString")
        assertEqual(mBeanData.values[findIndex(mBeanData, "String")], "test1")
        val compositeData = mBeanData.values[findIndex(mBeanData, "ExplicitComposite")]
        assertEqual(getCompositeType(compositeData).keySet(), sortedSetOf("description", "value"))
        assertEqual(getCompositeType(compositeData).typeName, "some.class")
        assertEqual(getCompositeType(compositeData).description, "some description")
        assertEqual(getCompositeType(compositeData).getDescription("value"), "double value")
        assertEqual(getCompositeType(compositeData).getType("value"), SimpleType.DOUBLE)
        assertEqual(getCompositeValues(compositeData)[0], "explicit")
        assertEqual(getCompositeValues(compositeData)[1], 105.55)

        var attributeInfo = mBeanData.beanInfo.attributes.first { it.name == "String" }
        assertEqual(attributeInfo.isWritable, false)
        assertEqual(attributeInfo.isReadable, true)
        assertEqual(attributeInfo.type, String::class.java.name)

        attributeInfo = mBeanData.beanInfo.attributes.first { it.name == "Standard" }
        assertEqual(attributeInfo.isWritable, false)
        assertEqual(attributeInfo.isReadable, true)
        assertEqual(attributeInfo.type, StandardComponent::class.java.name)

        attributeInfo = mBeanData.beanInfo.attributes.first { it.name == "Int" }
        assertEqual(attributeInfo.isWritable, true)
        assertEqual(attributeInfo.isReadable, true)
        assertEqual(attributeInfo.type, Int::class.java.name)

        serverConnection.setMBeanAttribute(vm, MBeanWorkload.STANDARD_BEAN_NAME, attributeInfo, 10)
        mBeanData = serverConnection.getMBeanData(vm, MBeanWorkload.STANDARD_BEAN_NAME, true, true)

        assertEqual(mBeanData.values[findIndex(mBeanData, "Int")], 10)
        assertEqual(mBeanData.values[findIndex(mBeanData, "Standard")], "StandardComponent toString")
        assertEqual(mBeanData.values[findIndex(mBeanData, "String")], "test1")

        var operationInfo = mBeanData.beanInfo.operations.first { it.name == "add" && it.signature[1].type == StandardComponent::class.java.name }
        assertTrue(operationInfo != null)
        val errorMessage = serverConnection.invokeMBeanOperation(vm, MBeanWorkload.STANDARD_BEAN_NAME, operationInfo, arrayOf<Any?>(10, null)).errorMessage
        if (vmConfig.isAtLeastJava(15)) {
            assertEqual(
                errorMessage,
                """Cannot invoke "dev.jvmguard.integration.tests.jvmguard.mbean.standard.StandardComponent.getVal()" because "y" is null"""
            )
        } else {
            assertEqual(errorMessage, NullPointerException::class.java.name)
        }
        assertTrue(
            serverConnection.invokeMBeanOperation(vm, MBeanWorkload.STANDARD_BEAN_NAME, operationInfo, arrayOf<Any?>(10, null)).stackTrace.startsWith(
                NullPointerException::class.java.name
            )
        )

        operationInfo = mBeanData.beanInfo.operations.first { it.name == "add" && it.signature[1].type == Int::class.java.name }
        assertTrue(operationInfo != null)
        val ret = serverConnection.invokeMBeanOperation(vm, MBeanWorkload.STANDARD_BEAN_NAME, operationInfo, arrayOf<Any?>(10, 20)).returnValue
        assertEqual(ret, 30)

        var message = serverConnection.invokeMBeanOperation(vm, MBeanWorkload.CHILD_BEAN_NAME, operationInfo, arrayOf<Any?>(10, null)).errorMessage
        assertTrue(message.startsWith("Signature mismatch for operation add")) {
            println(message)
        }
        message = serverConnection.invokeMBeanOperation(vm, MBeanWorkload.CHILD_BEAN_NAME, operationInfo, arrayOf<Any?>(10, null)).stackTrace
        assertTrue(message.startsWith(ReflectionException::class.java.name)) {
            println(message)
        }
    }

    private fun checkDynamic(serverConnection: TestServerConnection, vm: VM) {
        var mBeanData = serverConnection.getMBeanData(vm, MBeanWorkload.DYNAMIC_BEAN_NAME, true, true)
        assertEqual(mBeanData.beanInfo.attributes.map { it.name }.sorted(), listOf("State", "Table"))
        assertEqual(mBeanData.values.size, 2)
        assertEqual(mBeanData.values[findIndex(mBeanData, "State")], "none")
        assertEqual(
            (mBeanData.values[findIndex(mBeanData, "Table")] as Array<*>).apply { sortBy { (it as Array<*>).last() as Comparable<Comparable<*>> } },
            arrayOf<Any>(arrayOf<Any?>("base", 1, "base1"), arrayOf<Any?>("base", 2, "base2"), arrayOf<Any?>("sub", 1, "sub1"), arrayOf<Any?>("sub", 2, "sub2"))
        )
        val tabularType = mBeanData.beanInfo.attributes[1].descriptor?.getFieldValue(OPEN_TYPE) as TabularType
        assertEqual(tabularType.typeName, "tabType1")
        assertEqual(tabularType.indexNames, listOf("column1", "column2"))
        assertEqual(tabularType.rowType.keySet().sorted(), listOf("column1", "column2", "column3"))
        assertEqual(tabularType.rowType.getType("column1"), SimpleType.STRING)
        assertEqual(tabularType.rowType.getType("column2"), SimpleType.INTEGER)
        assertEqual(tabularType.rowType.getType("column3"), SimpleType.STRING)

        serverConnection.setMBeanAttribute(vm, MBeanWorkload.DYNAMIC_BEAN_NAME, mBeanData.beanInfo.attributes.find {
            it.name == "State"
        }, "changed")
        mBeanData = serverConnection.getMBeanData(vm, MBeanWorkload.DYNAMIC_BEAN_NAME, true, true)
        assertEqual(mBeanData.values[findIndex(mBeanData, "State")], "changed")

        assertEqual(mBeanData.beanInfo.operations.map { it.name }.sorted(), listOf("operation1"))
        val operationInfo = mBeanData.beanInfo.operations.first()
        assertTrue(operationInfo.descriptor?.getFieldValue(OPEN_TYPE) == null)
        assertEqual(operationInfo.signature.map { it.descriptor?.getFieldValue(OPEN_TYPE) }, listOf(null))
        assertEqual(operationInfo.returnType, Int::class.java.name)
        assertEqual(operationInfo.signature.map { it.type }, listOf(Int::class.java.name))
        assertEqual(operationInfo.signature.map { it.name }, listOf("param1"))
        assertEqual(operationInfo.signature.map { it.description }, listOf("param1 descr"))

        assertEqual(serverConnection.invokeMBeanOperation(vm, MBeanWorkload.DYNAMIC_BEAN_NAME, operationInfo, arrayOf<Any?>(10)).returnValue, 20)
    }


    private fun getCompositeType(compositeData: Any): CompositeType {
        return compositeData.javaClass.methods.first { it.returnType == CompositeType::class.java }.invoke(compositeData) as CompositeType
    }

    @Suppress("UNCHECKED_CAST")
    private fun getCompositeValues(compositeData: Any): Array<Any?> {
        return compositeData.javaClass.methods.first { it.returnType == Array<Any?>(0) { null }.javaClass }.invoke(compositeData) as Array<Any?>
    }
}

const val OPEN_TYPE = "openType"

fun findIndex(mBeanData: MBeanData, name: String, openType: OpenType<*>? = null) =
    mBeanData.beanInfo.attributes.withIndex().find { (_, attributeInfo) ->
        attributeInfo.name == name &&
                (openType == null || openType == attributeInfo.descriptor?.getFieldValue(OPEN_TYPE))
    }?.index ?: -1

