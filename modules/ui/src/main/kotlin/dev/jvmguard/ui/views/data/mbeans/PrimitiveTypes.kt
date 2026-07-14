package dev.jvmguard.ui.views.data.mbeans

object PrimitiveTypes {

    val ALL_PRIMITIVE_NAMES = setOf(
        "boolean", "byte", "char", "short", "int", "long", "float", "double",
    )

    private val WRAPPER_TO_PRIMITIVE = mapOf(
        "java.lang.Boolean" to "boolean",
        "java.lang.Byte" to "byte",
        "java.lang.Character" to "char",
        "java.lang.Short" to "short",
        "java.lang.Integer" to "int",
        "java.lang.Long" to "long",
        "java.lang.Float" to "float",
        "java.lang.Double" to "double",
        "java.lang.Void" to "void",
    )

    fun unwrapName(wrapperTypeName: String): String? = WRAPPER_TO_PRIMITIVE[wrapperTypeName]
}
