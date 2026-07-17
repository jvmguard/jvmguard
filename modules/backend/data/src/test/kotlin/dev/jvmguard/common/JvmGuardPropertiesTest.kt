package dev.jvmguard.common

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource

class JvmGuardPropertiesTest {

    @Test
    fun defaults() {
        val properties = JvmGuardProperties()
        assertEquals("data", properties.dataDirectory)
        assertEquals(8020, properties.httpPort)
        assertEquals(8847, properties.vmPort)
        assertFalse(properties.isUseHttps)
        assertEquals(100_000, properties.passwordIterations)
        assertEquals("", properties.keystoreName)
    }

    @Test
    fun bindOverrides() {
        val source = MapConfigurationPropertySource(
            mapOf(
                "jvmguard.httpPort" to "9090",
                "jvmguard.useHttps" to "true",
                "jvmguard.keystoreName" to "web.pkcs12",
                "jvmguard.telemetryStorage.10" to "72",
            )
        )
        val properties = Binder(source).bind("jvmguard", JvmGuardProperties::class.java).orElseGet { JvmGuardProperties() }

        assertEquals(9090, properties.httpPort)
        assertTrue(properties.isUseHttps)
        assertEquals("web.pkcs12", properties.keystoreName)
        assertEquals(8847, properties.vmPort) // untouched default
        assertEquals(72, properties.telemetryStorage[10]) // nested map key
    }
}
