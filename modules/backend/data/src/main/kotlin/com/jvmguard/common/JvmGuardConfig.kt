package com.jvmguard.common

object JvmGuardConfig {

    val isIntegrationTest: Boolean = java.lang.Boolean.getBoolean("jvmguard.integrationTest")

    @Volatile
    private var properties: JvmGuardProperties = JvmGuardProperties()

    fun setProperties(properties: JvmGuardProperties) {
        this.properties = properties
    }

    fun properties(): JvmGuardProperties = properties
}
