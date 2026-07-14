package dev.jvmguard.server

import com.install4j.runtime.launcher.Launcher
import dev.jvmguard.agent.comm.JvmGuardKeyManager
import dev.jvmguard.collector.connection.SslManager
import dev.jvmguard.common.JvmGuardProperties
import dev.jvmguard.common.helper.PasswordHelper
import org.springframework.stereotype.Component
import java.io.File
import kotlin.system.exitProcess

@Component
class WebServerSupport(
    private val sslManager: SslManager,
    private val properties: JvmGuardProperties,
) {

    data class SslConfig(
        val keystoreFile: File,
        val keystorePassword: String,
        val keystoreType: String,
        val selfSigned: Boolean,
    )

    fun createSslConfig(): SslConfig? {
        var selfSigned = false

        var keystoreName = properties.keystoreName
        val keystorePassword: String
        val keyStoreFile: File?
        if (keystoreName.isEmpty() && sslManager.getFile(DEFAULT_CERTIFICATE_FILE).exists()) {
            keystoreName = DEFAULT_CERTIFICATE_FILE
        }
        if (keystoreName.isEmpty()) {
            selfSigned = true
            keyStoreFile = getSelfSignedKeystore()
            keystorePassword = JvmGuardKeyManager.JVMGUARD
        } else {
            keyStoreFile = sslManager.getFile(keystoreName)
            keystorePassword = PasswordHelper.deobfuscate(properties.keystorePassword).orEmpty()
        }

        if (keyStoreFile == null || !keyStoreFile.isFile) {
            LOGGER.info("Could not find keystore file ({}). Falling back to HTTP.", keyStoreFile)
            return null
        }
        if (keystorePassword.isEmpty() && Launcher.isService()) {
            LOGGER.error("You have to specify a keystore password in config/application.yaml or with the system property jvmguard.keystorePassword when using ssl in service mode.")
            exitProcess(1)
        }
        LOGGER.info("Using HTTPS with keystore file {}", keyStoreFile.absolutePath)
        val name = keyStoreFile.name
        val keystoreType = if (name.endsWith(".pkcs") || name.endsWith(".pkcs12") || name.endsWith(".pfx")) "PKCS12" else "JKS"
        return SslConfig(keyStoreFile, keystorePassword, keystoreType, selfSigned)
    }

    private fun getSelfSignedKeystore(): File? {
        val file = sslManager.getFile(SELF_SIGNED_KS)
        if (!file.isFile) {
            try {
                sslManager.generateWebCertificate(file)
            } catch (e: Exception) {
                LOGGER.error("could not generate self-signed certificate", e)
                return null
            }
        }
        return file
    }

    companion object {
        private val LOGGER = ServerMain.LOGGER
        private const val SELF_SIGNED_KS = "self_signed.ks"
        private const val DEFAULT_CERTIFICATE_FILE = "web.pkcs12"
    }
}
