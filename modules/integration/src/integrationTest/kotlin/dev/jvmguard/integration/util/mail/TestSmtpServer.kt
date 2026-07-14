package dev.jvmguard.integration.util.mail

import dev.jvmguard.data.config.SmtpConfig.Encryption
import jakarta.mail.MessagingException
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import org.subethamail.smtp.MessageContext
import org.subethamail.smtp.MessageHandler
import org.subethamail.smtp.MessageHandlerFactory
import org.subethamail.smtp.RejectException
import org.subethamail.smtp.server.SMTPServer
import java.io.InputStream
import java.util.Collections
import java.util.Properties

class TestSmtpServer(port: Int, private val authentication: Boolean, encryption: Encryption) : MessageHandlerFactory {

    private val server: SMTPServer = SslSmtpServer.create(this, port, authentication, encryption)

    val messages: MutableList<TestMessage> = Collections.synchronizedList(ArrayList())

    private val session: Session = Session.getDefaultInstance(Properties())

    init {
        server.start()
    }

    override fun create(context: MessageContext): MessageHandler {
        return object : MessageHandler {
            private var from: String? = null
            private val to = ArrayList<String>()

            override fun from(from: String) {
                this.from = from
                println(context.authenticationHandler)
                if (authentication && context.authenticationHandler.isEmpty) {
                    throw RejectException("authentication required")
                }
            }

            override fun recipient(recipient: String) {
                to.add(recipient)
            }

            override fun data(data: InputStream): String? {
                try {
                    val message = MimeMessage(session, data)
                    messages.add(TestMessage(requireNotNull(from), to, message))
                } catch (_: MessagingException) {
                    throw RejectException("could not parse mail")
                }
                return null
            }

            override fun done() {
            }
        }
    }
}
