package com.jvmguard.mcp.tool

import com.jvmguard.mcp.McpError
import com.jvmguard.mcp.McpJson
import com.jvmguard.mcp.McpToolContext
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification
import io.modelcontextprotocol.spec.McpSchema.Tool

class SetMbeanAttributeTool(ctx: McpToolContext) : McpTool(ctx) {

    companion object {
        const val NAME = "set_mbean_attribute"
    }

    override fun createSpecification(): SyncToolSpecification {
        val tool = Tool.builder(
            NAME,
            objectSchema(
                mapOf(
                    "vm" to stringProperty("VM hierarchy path (a pool path resolves to one connected member)."),
                    "name" to stringProperty("MBean object name (from list_mbeans)."),
                    "attribute" to stringProperty("Attribute name to set (must be writable; see get_mbean_data)."),
                    "value" to anyProperty(
                        "New value. Give it as the natural JSON type (number, boolean, string, or a JSON array for an " +
                                "array attribute); it is converted to the attribute's declared type. Pass null to clear " +
                                "a nullable attribute."
                    ),
                ),
                listOf("vm", "name", "attribute", "value"),
            ),
        ).description(
            "Set a writable attribute on an MBean in the target VM. Requires profiler access. Supports scalar types " +
                    "(numbers, booleans, characters, strings, BigInteger/BigDecimal, ObjectName) and one-dimensional " +
                    "arrays of those; composites and tabular data cannot be written."
        ).annotations(action("Set MBean attribute")).build()
        return SyncToolSpecification(tool) { _, request ->
            val args = request.arguments()
            val vmPath = args["vm"] as String
            val name = args["name"] as String
            val attributeName = args["attribute"] as String
            val rawValue = args["value"]
            ctx.withConnection { conn ->
                val vm = VmResolver.resolveLiveVm(conn, vmPath)
                ctx.requireMbeanMutationAllowed(vm)
                val beanInfo = conn.getMBeanData(vm, name, true, false)?.beanInfo
                    ?: throw McpError("MBean not found: $name")
                val attribute = beanInfo.attributes.orEmpty().firstOrNull { it.name == attributeName }
                    ?: throw McpError(
                        "Attribute '$attributeName' not found on $name. " +
                                "Writable attributes: ${beanInfo.attributes.orEmpty().filter { it.isWritable }.joinToString(", ") { it.name }}."
                    )
                if (!attribute.isWritable) {
                    throw McpError("Attribute '$attributeName' on $name is not writable.")
                }
                val result = conn.setMBeanAttribute(vm, name, attribute, McpMBeanValues.coerce(attribute.type, rawValue))
                result.errorMessage?.let { throw McpError("Failed to set '$attributeName' on $name: $it") }
                jsonResult(
                    McpJson.write(
                        mapOf("status" to "ok", "objectName" to name, "attribute" to attributeName, "value" to rawValue)
                    )
                )
            }
        }
    }
}

class InvokeMbeanOperationTool(ctx: McpToolContext) : McpTool(ctx) {

    companion object {
        const val NAME = "invoke_mbean_operation"
    }

    override fun createSpecification(): SyncToolSpecification {
        val tool = Tool.builder(
            NAME,
            objectSchema(
                mapOf(
                    "vm" to stringProperty("VM hierarchy path (a pool path resolves to one connected member)."),
                    "name" to stringProperty("MBean object name (from list_mbeans)."),
                    "operation" to stringProperty("Operation name to invoke (see the 'operations' from get_mbean_data)."),
                    "parameters" to anyArrayProperty(
                        "Positional arguments, each as its natural JSON type (use a nested JSON array for an array " +
                                "parameter). Omit or pass [] for a no-arg operation."
                    ),
                    "signature" to stringProperty(
                        "Only needed to disambiguate overloaded operations: the exact signature such as \"add(int,int)\"."
                    ),
                ),
                listOf("vm", "name", "operation"),
            ),
        ).description(
            "Invoke an operation on an MBean in the target VM and return its result. Requires profiler access. " +
                    "Parameters support scalar types (numbers, booleans, characters, strings, BigInteger/BigDecimal, " +
                    "ObjectName) and one-dimensional arrays of those."
        ).annotations(action("Invoke MBean operation")).build()
        return SyncToolSpecification(tool) { _, request ->
            val args = request.arguments()
            val vmPath = args["vm"] as String
            val name = args["name"] as String
            val operationName = args["operation"] as String
            val rawParameters = (args["parameters"] as? List<*>) ?: emptyList<Any?>()
            val signature = (args["signature"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
            ctx.withConnection { conn ->
                val vm = VmResolver.resolveLiveVm(conn, vmPath)
                ctx.requireMbeanMutationAllowed(vm)
                val beanInfo = conn.getMBeanData(vm, name, true, false)?.beanInfo
                    ?: throw McpError("MBean not found: $name")
                val byName = beanInfo.operations.orEmpty().filter { it.name == operationName }
                if (byName.isEmpty()) {
                    throw McpError(
                        "Operation '$operationName' not found on $name. " +
                                "Available: ${McpMBeanData.operationSignatures(beanInfo).joinToString(", ")}."
                    )
                }
                val candidates = byName.filter { it.signature.orEmpty().size == rawParameters.size }
                val operation = when {
                    signature != null -> byName.firstOrNull { McpMBeanData.signatureOf(it) == signature }
                        ?: throw McpError("No overload of '$operationName' matches signature \"$signature\".")
                    candidates.size == 1 -> candidates.first()
                    candidates.isEmpty() -> throw McpError(
                        "No overload of '$operationName' takes ${rawParameters.size} parameter(s). " +
                                "Signatures: ${byName.map { McpMBeanData.signatureOf(it) }.joinToString(", ")}."
                    )
                    else -> throw McpError(
                        "'$operationName' is overloaded; pass 'signature' to choose one of: " +
                                "${candidates.map { McpMBeanData.signatureOf(it) }.joinToString(", ")}."
                    )
                }
                val parameters = operation.signature.orEmpty()
                    .mapIndexed { index, param -> McpMBeanValues.coerce(param.type, rawParameters[index]) }
                    .toTypedArray()
                val result = conn.invokeMBeanOperation(vm, name, operation, parameters)
                result.errorMessage?.let { throw McpError("Operation '$operationName' on $name failed: $it") }
                jsonResult(
                    McpJson.write(
                        buildMap {
                            put("status", "ok")
                            put("objectName", name)
                            put("operation", McpMBeanData.signatureOf(operation))
                            if (operation.returnType != "void") {
                                put("returnValue", McpMBeanData.decodeReturnValue(operation, result.returnValue))
                            }
                        }
                    )
                )
            }
        }
    }
}
