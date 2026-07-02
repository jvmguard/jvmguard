package com.jvmguard.integration.util.mail

import com.jvmguard.data.config.SmtpConfig.Encryption
import org.subethamail.smtp.MessageHandlerFactory
import org.subethamail.smtp.auth.EasyAuthenticationHandlerFactory
import org.subethamail.smtp.auth.LoginFailedException
import org.subethamail.smtp.server.SMTPServer
import java.io.FileInputStream
import java.net.InetAddress
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

object SslSmtpServer {

    const val USER = "user"
    const val PASSWORD = "password"

    fun create(
        messageHandlerFactory: MessageHandlerFactory,
        port: Int,
        authentication: Boolean,
        encryption: Encryption,
    ): SMTPServer {
        val keyStorePassphrase = "password".toCharArray()
        val ksKeys = KeyStore.getInstance("JKS")
        FileInputStream(System.getProperty("test.keyStore")).use { keyStoreInput ->
            ksKeys.load(keyStoreInput, keyStorePassphrase)
        }
        val keyManagerFactory = KeyManagerFactory.getInstance("SunX509").apply { init(ksKeys, keyStorePassphrase) }
        val trustManagerFactory = TrustManagerFactory.getInstance("SunX509").apply { init(ksKeys) }
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(keyManagerFactory.keyManagers, trustManagerFactory.trustManagers, null)
        }

        return SMTPServer.Builder().apply {
            port(port)
            bindAddress(InetAddress.getByName("localhost"))
            hostName("localhost")
            if (authentication) {
                requireAuth()
            }
            messageHandlerFactory(messageHandlerFactory)
            authenticationHandlerFactory(EasyAuthenticationHandlerFactory { username, password, _ ->
                if (username != USER || password != PASSWORD) {
                    throw LoginFailedException()
                }
            })
            when (encryption) {
                Encryption.STARTTLS -> requireTLS(true).enableTLS(true)
                Encryption.SSL -> serverSocketFactory(sslContext)
                else -> {}
            }
            startTlsSocketFactory(sslContext)
        }.build()
    }
}
