package com.jvmguard.integration.tests.jvmguard.trigger.action

import com.jvmguard.integration.util.mail.SslSmtpServer
import com.jvmguard.data.config.SmtpConfig

class StartTlsMailTest : MailTest() {
    override fun modifyEncryption(smtpConfig: SmtpConfig) {
        smtpConfig.encryption = SmtpConfig.Encryption.STARTTLS
        smtpConfig.userName = obfuscate(SslSmtpServer.USER)!!
        smtpConfig.password = obfuscate(SslSmtpServer.PASSWORD)!!
        smtpConfig.isAuthenticate = true
    }
}
