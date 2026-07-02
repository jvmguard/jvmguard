package com.jvmguard.connector.server.mock

import com.jvmguard.agent.mbean.MBeanData
import com.jvmguard.agent.mbean.MBeanModificationData
import com.jvmguard.agent.mbean.MBeanOperationData
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.management.ManagementFactory
import javax.management.*

/**
 * Serves the platform MBeans of the server process
 */
internal class MockMBeans {

    private val server = ManagementFactory.getPlatformMBeanServer()

    fun getMBeanNames(): List<String> {
        val names = server.queryNames(null, null)
        return names.map { it.canonicalName }
    }

    fun getMBeanData(name: String, fetchStructure: Boolean, fetchValues: Boolean): MBeanData {
        var beanInfo: MBeanInfo? = null
        var values: MutableList<Any?>? = null
        try {
            val objectName = ObjectName.getInstance(name)
            beanInfo = server.getMBeanInfo(objectName)
            if (fetchValues && beanInfo != null) {
                val attributes = beanInfo.attributes
                values = ArrayList(attributes.size)
                for (attribute in attributes) {
                    values.add(readAttribute(objectName, attribute))
                }
            }
        } catch (_: Throwable) {
        }
        return Data(if (fetchStructure) beanInfo else null, values)
    }

    fun setAttribute(name: String, attributeInfo: MBeanAttributeInfo, value: Any?): MBeanModificationData =
        try {
            server.setAttribute(ObjectName.getInstance(name), Attribute(attributeInfo.name, value))
            Modification(null, null)
        } catch (t: Throwable) {
            Modification(message(t), stackTrace(t))
        }

    fun invoke(name: String, operationInfo: MBeanOperationInfo, parameters: Array<Any?>): MBeanOperationData =
        try {
            val signatureInfo = operationInfo.signature
            val signature = Array(signatureInfo.size) { signatureInfo[it].type }
            val result = server.invoke(ObjectName.getInstance(name), operationInfo.name, parameters, signature)
            Operation(null, null, result)
        } catch (t: Throwable) {
            Operation(message(t), stackTrace(t), null)
        }

    private fun readAttribute(objectName: ObjectName, attribute: MBeanAttributeInfo): Any? {
        if (!attribute.isReadable) {
            return null
        }
        return try {
            val value = server.getAttribute(objectName, attribute.name)
            // The platform MBean server returns primitive arrays (e.g. long[]) raw, whereas a real
            // jvmguard agent serializes them as boxed Object[]. The shared AttributeValueHelper only
            // handles Object[], so drop raw primitive arrays here to keep the mock off that path.
            if (isPrimitiveArray(value)) null else value
        } catch (_: Throwable) {
            null
        }
    }

    private class Data(private val beanInfo: MBeanInfo?, private val values: List<Any?>?) : MBeanData {
        override fun getBeanInfo(): MBeanInfo? = beanInfo
        override fun getValues(): List<Any?>? = values
    }

    private class Modification(private val errorMessage: String?, private val stackTrace: String?) : MBeanModificationData {
        override fun getErrorMessage(): String? = errorMessage
        override fun getStackTrace(): String? = stackTrace
    }

    private class Operation(private val errorMessage: String?, private val stackTrace: String?, private val returnValue: Any?) : MBeanOperationData {
        override fun getErrorMessage(): String? = errorMessage
        override fun getStackTrace(): String? = stackTrace
        override fun getReturnValue(): Any? = returnValue
    }

    companion object {
        private fun isPrimitiveArray(value: Any?): Boolean =
            value != null && value.javaClass.isArray && value.javaClass.componentType.isPrimitive

        private fun message(t: Throwable): String = t.message ?: t.javaClass.name

        private fun stackTrace(t: Throwable): String {
            val writer = StringWriter()
            t.printStackTrace(PrintWriter(writer))
            return writer.toString()
        }
    }
}
