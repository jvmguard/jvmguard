package dev.jvmguard.ui.views.data.mbeans

import javax.management.Descriptor
import javax.management.MBeanOperationInfo
import javax.management.MBeanParameterInfo
import javax.management.openmbean.*

object MBeanOperations {

    fun verbose(operationInfo: MBeanOperationInfo): String {
        val buffer = StringBuilder()
        buffer.append(operationInfo.name)
        buffer.append('(')
        var first = true
        for (parameterInfo in operationInfo.signature) {
            if (first) {
                first = false
            } else {
                buffer.append(", ")
            }
            appendDescriptor(buffer, parameterInfo.descriptor, parameterInfo.type)
            buffer.append(' ')
            buffer.append(parameterInfo.name)
        }
        buffer.append(")")
        buffer.append(" → ")
        appendDescriptor(buffer, operationInfo.descriptor, operationInfo.returnType)
        return buffer.toString()
    }

    fun signatureSuffix(operationInfo: MBeanOperationInfo): String {
        val full = verbose(operationInfo)
        val name = operationInfo.name
        return if (full.startsWith(name)) full.substring(name.length) else full
    }

    fun valueEditSpecs(operationInfo: MBeanOperationInfo): List<ValueEditSpec> =
        operationInfo.signature.map { parameterInfo ->
            val openType = openTypeOf(parameterInfo)
            val caption = descriptorVerbose(parameterInfo)
            ValueEditSpec(openType, parameterInfo.type, caption, OpenTypeHelper.getDefaultValue(openType))
        }

    fun returnOpenType(operationInfo: MBeanOperationInfo): OpenType<*>? =
        typeValueOf(operationInfo.descriptor, operationInfo.returnType) as? OpenType<*>

    private fun openTypeOf(parameterInfo: MBeanParameterInfo): OpenType<*> {
        val typeValue = typeValueOf(parameterInfo.descriptor, parameterInfo.type)
        return typeValue as? OpenType<*> ?: throw NonOpenTypeException()
    }

    private fun descriptorVerbose(parameterInfo: MBeanParameterInfo): String {
        val buffer = StringBuilder()
        appendDescriptor(buffer, parameterInfo.descriptor, parameterInfo.type)
        buffer.append(' ')
        buffer.append(parameterInfo.name)
        return buffer.toString()
    }

    private fun appendDescriptor(buffer: StringBuilder, descriptor: Descriptor, standardType: String) {
        appendType(buffer, typeValueOf(descriptor, standardType))
    }

    private fun typeValueOf(descriptor: Descriptor, standardType: String): Any? {
        val fieldNames = descriptor.fieldNames
        return if (fieldNames != null && fieldNames.isNotEmpty()) {
            descriptor.getFieldValue(fieldNames[0])
        } else {
            OpenTypeHelper.getFromStandardType(standardType)
        }
    }

    private fun appendType(buffer: StringBuilder, type: Any?) {
        when (type) {
            is ArrayType<*> -> {
                appendType(buffer, type.elementOpenType)
                buffer.append("[]".repeat(type.dimension))
            }

            is TabularType -> buffer.append("[Tabular]")
            is CompositeType -> buffer.append("[Composite]")
            is SimpleType<*> -> buffer.append(primitiveName(type))
            else -> buffer.append("Object")
        }
    }

    private fun primitiveName(simpleType: SimpleType<*>): String {
        val typeName = simpleType.typeName
        return PrimitiveTypes.unwrapName(typeName) ?: typeName
    }
}
