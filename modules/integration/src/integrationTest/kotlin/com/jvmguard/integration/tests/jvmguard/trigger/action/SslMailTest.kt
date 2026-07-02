package com.jvmguard.integration.tests.jvmguard.trigger.action

import com.jvmguard.integration.util.mail.SslSmtpServer
import com.jvmguard.data.config.SmtpConfig

class SslMailTest : MailTest() {
    override fun modifyEncryption(smtpConfig: SmtpConfig) {
        smtpConfig.encryption = SmtpConfig.Encryption.SSL
        smtpConfig.userName = obfuscate(SslSmtpServer.USER)!!
        smtpConfig.password = obfuscate(SslSmtpServer.PASSWORD)!!
        smtpConfig.isAuthenticate = true
    }
}
