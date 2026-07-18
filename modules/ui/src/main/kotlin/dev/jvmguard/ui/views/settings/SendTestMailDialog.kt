package dev.jvmguard.ui.views.settings

import dev.jvmguard.data.config.SmtpConfig
import dev.jvmguard.ui.components.Notifications
import dev.jvmguard.ui.components.JvmGuardDialog
import dev.jvmguard.ui.server.Sessions
import dev.jvmguard.ui.server.runInBackground
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.EmailField

class SendTestMailDialog(private val smtpConfig: SmtpConfig) : JvmGuardDialog() {

    private val recipient = EmailField("Recipient").apply {
        isClearButtonVisible = true
        setWidthFull()
        testId = ID_RECIPIENT
        value = currentUserEmail()
    }

    init {
        headerTitle = "Send test email"
        width = "30rem"

        val hint = Span("An email with the subject \"$SUBJECT\" is sent to check the SMTP configuration.")
            .apply { addClassName("jvmguard-field-hint") }
        add(VerticalLayout(hint, recipient).apply {
            isPadding = false
            isSpacing = true
        })

        lateinit var send: Button
        send = confirmFooter("Send email", ID_SEND) { send(send) }
    }

    private fun send(sendButton: Button) {
        val address = recipient.value.orEmpty()
        if (address.isEmpty() || recipient.isInvalid) {
            recipient.isInvalid = true
            return
        }
        sendButton.isEnabled = false
        val ui = UI.getCurrent()
        runInBackground {
            val error = try {
                Sessions.current()?.serverConnection?.sendTestMail(address, SUBJECT, BODY, smtpConfig)
                null
            } catch (e: Exception) {
                e
            }
            ui.access {
                if (error == null) {
                    Notifications.show("Test email sent. Please check your inbox.")
                    close()
                } else {
                    sendButton.isEnabled = true
                    Notifications.show("The test email could not be sent: ${error.message}")
                }
            }
        }
    }

    private fun currentUserEmail(): String =
        Sessions.current()?.user?.email.orEmpty()

    companion object {
        const val ID_RECIPIENT = "test-mail-recipient"
        const val ID_SEND = "test-mail-send"

        private const val SUBJECT = "jvmguard SMTP check"
        private const val BODY = "If you receive this email, the SMTP configuration in jvmguard is working."
    }
}
