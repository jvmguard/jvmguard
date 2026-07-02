package com.jvmguard.collector.telemetry

import com.jvmguard.agent.config.telemetry.MBeanTelemetryConfig

class MBeanTelemetryIdentifier(config: MBeanTelemetryConfig) {
    private val name: String = config.name

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        return name == (other as MBeanTelemetryIdentifier).name
    }

    override fun hashCode(): Int = name.hashCode()
}
