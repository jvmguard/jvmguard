package dev.jvmguard.server

import dev.jvmguard.common.JvmGuardProperties
import org.apache.catalina.valves.RemoteIpValve
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory
import org.springframework.boot.web.server.Ssl
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.stereotype.Component

@Component
class WebServerCustomizer(
    private val webServerSupport: WebServerSupport,
    private val properties: JvmGuardProperties,
) : WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

    override fun customize(factory: TomcatServletWebServerFactory) {
        if (properties.isUseHttps) {
            webServerSupport.createSslConfig()?.let { factory.ssl = toSsl(it) }
        }

        factory.addConnectorCustomizers({ it.setProperty("server", "") })

        if (properties.isReverseProxy) {
            LOGGER.info("Setting up web server for reverse proxy")
            factory.addEngineValves(RemoteIpValve())
        }
    }

    private fun toSsl(sslConfig: WebServerSupport.SslConfig): Ssl = Ssl().apply {
        isEnabled = true
        keyStore = sslConfig.keystoreFile.absolutePath
        keyStorePassword = sslConfig.keystorePassword
        keyStoreType = sslConfig.keystoreType
    }

    companion object {
        private val LOGGER = ServerMain.LOGGER
    }
}
