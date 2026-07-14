package dev.jvmguard.common.helper

import dev.jvmguard.common.Loggers
import dev.jvmguard.common.JvmGuardProperties
import dev.jvmguard.data.config.SmtpConfig
import dev.jvmguard.data.config.SmtpConfig.Encryption
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMessage.RecipientType
import org.eclipse.angus.mail.util.MailSSLSocketFactory
import org.springframework.stereotype.Component
import java.security.GeneralSecurityException
import java.util.*

@Component
class MailService(private val properties: JvmGuardProperties) {

    fun smtpRetrySeconds(): Int = properties.mailRetrySeconds

    fun sendMail(recipient: String, subject: String, content: String, smtpConfig: SmtpConfig, shortTimeout: Boolean) {
        val message = MimeMessage(getSession(smtpConfig, shortTimeout)).apply {
            addRecipient(RecipientType.TO, InternetAddress(recipient))
            addFrom(arrayOf(InternetAddress(smtpConfig.fromEmail)))
            if (CharsetHelper.isAsciiOnly(subject)) {
                setSubject(subject)
            } else {
                setSubject(subject, "UTF-8")
            }
            if (CharsetHelper.isAsciiOnly(content)) {
                setText(content)
            } else {
                setText(content, "UTF-8")
            }
        }
        Transport.send(message)
    }

    private fun getSession(smtpConfig: SmtpConfig, shortTimeout: Boolean): Session {
        val sessionProperties = Properties().apply {
            setProperty("mail.smtp.from", smtpConfig.fromEmail)
            setProperty("mail.smtp.submitter", smtpConfig.fromEmail)
            setProperty("mail.smtp.host", smtpConfig.host)
            setProperty("mail.smtp.port", smtpConfig.port.toString())
            setProperty("mail.smtp.connectiontimeout", if (shortTimeout) "10000" else properties.smtpConnectionTimeout)
            setProperty("mail.smtp.timeout", if (shortTimeout) "10000" else properties.smtpTimeout)
            setProperty("mail.smtp.writetimeout", if (shortTimeout) "10000" else properties.smtpWriteTimeout)
        }

        if (smtpConfig.encryption != Encryption.NONE) {
            try {
                val socketFactory = MailSSLSocketFactory()
                if (properties.isSmtpTrustAllHosts) {
                    socketFactory.isTrustAllHosts = true
                }
                sessionProperties[smtpConfig.encryption.javaMailProperty] = "true"
                sessionProperties["mail.smtp.ssl.socketFactory"] = socketFactory
            } catch (e: GeneralSecurityException) {
                Loggers.SERVER.warn("Could not configure mail SSL socket factory", e)
            }
        }

        return if (smtpConfig.isAuthenticate) {
            sessionProperties.setProperty("mail.smtp.auth", "true")
            Session.getInstance(sessionProperties, MailAuthenticator(smtpConfig.userName, smtpConfig.password))
        } else {
            Session.getInstance(sessionProperties)
        }
    }

    private class MailAuthenticator(username: String, password: String) : jakarta.mail.Authenticator() {
        private val authentication = PasswordAuthentication(username, password)

        override fun getPasswordAuthentication(): PasswordAuthentication = authentication
    }
}
