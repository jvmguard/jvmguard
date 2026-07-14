package dev.jvmguard.server

import dev.jvmguard.common.JvmGuardConfig
import dev.jvmguard.common.JvmGuardDirectories
import dev.jvmguard.common.JvmGuardProperties
import org.springframework.boot.EnvironmentPostProcessor
import org.springframework.boot.SpringApplication
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource

// Runs after the external/classpath application.yaml has been loaded but before the logging system is initialized.
// It binds the jvmguard configuration, resolves the installation directories and publishes the bootstrap properties that
// Spring Boot itself needs (server.port, logging.config), so that no jvmguard configuration is read before the Spring
// environment exists.
class JvmGuardEnvironmentPostProcessor : EnvironmentPostProcessor {

    override fun postProcessEnvironment(environment: ConfigurableEnvironment, application: SpringApplication) {
        val properties = Binder.get(environment).bind("jvmguard", JvmGuardProperties::class.java)
            .orElseGet(::JvmGuardProperties)
        JvmGuardConfig.setProperties(properties)

        val dataDirectoryExplicit = System.getProperty("jvmguard.dataDirectory") != null
        val directories =
            JvmGuardDirectories.init(properties.dataDirectory, JvmGuardConfig.isIntegrationTest, dataDirectoryExplicit)

        val bootstrap = mutableMapOf<String, Any>(
            "server.port" to properties.httpPort,
            // The H2 file location is only known once the data directory has been resolved. Publish it as a
            // property; the spring.datasource.url template in application.yaml references it via a placeholder.
            "jvmguard.databaseDirectory" to directories.databaseDirectory.absolutePath.replace('\\', '/'),
            // Restrict Vaadin's classpath scan (for @Route/AppShell/@JsModule) to the UI package so startup
            // does not walk the whole classpath.
            "vaadin.allowed-packages" to "dev/jvmguard/web",
        )
        // The packaged dist has logback.xml next to the launcher. When running from sources (dev/e2e) it
        // is absent, so fall back to Spring Boot's default console logging.
        val logbackFile = directories.logbackFile
        if (logbackFile.isFile) {
            bootstrap["logging.config"] = logbackFile.absolutePath
        }
        environment.propertySources.addLast(MapPropertySource("jvmguardBootstrap", bootstrap))
    }
}
