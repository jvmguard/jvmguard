package com.jvmguard.common.util

import java.beans.IntrospectionException
import java.beans.Introspector
import java.beans.PropertyDescriptor
import java.lang.reflect.InvocationTargetException

object BeanUtil {

    fun copyValues(source: Any, target: Any, excludedPropertyNames: List<String> = emptyList()) {
        val sourceProperties: Array<PropertyDescriptor>
        val targetProperties: Array<PropertyDescriptor>
        try {
            sourceProperties = Introspector.getBeanInfo(source.javaClass).propertyDescriptors
            targetProperties = Introspector.getBeanInfo(target.javaClass).propertyDescriptors
        } catch (ex: IntrospectionException) {
            throw RuntimeException(ex)
        }

        for (property in targetProperties) {
            if (excludedPropertyNames.contains(property.name)) {
                continue
            }
            val writeTargetMethod = property.writeMethod ?: continue
            val readSourceMethod = findReadMethod(property.name, sourceProperties) ?: continue

            try {
                val value = readSourceMethod.invoke(source)
                writeTargetMethod.invoke(target, value)
            } catch (ex: IllegalAccessException) {
                throw RuntimeException(ex)
            } catch (ex: InvocationTargetException) {
                throw RuntimeException(ex)
            }
        }
    }

    private fun findReadMethod(name: String, properties: Array<PropertyDescriptor>) =
        properties.firstOrNull { it.name == name }?.readMethod
}
