package dev.jvmguard.ui.views.settings

import dev.jvmguard.data.config.SmtpConfig
import dev.jvmguard.ui.components.Notifications
import dev.jvmguard.ui.components.JvmGuardDialog
import dev.jvmguard.ui.server.Sessions
import dev.jvmguard.ui.server.runInBackground
import dev.jvmguard.ui.server.t
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.EmailField

class SendTestMailDialog(private val smtpConfig: SmtpConfig) : JvmGuardDialog() {

    private val recipient = EmailField(t("settings.email.test.recipient")).apply {
        isClearButtonVisible = true
        setWidthFull()
        testId = ID_RECIPIENT
        value = currentUserEmail()
    }

    init {
        headerTitle = t("settings.email.test.title")
        width = "30rem"

        val hint = Span(t("settings.email.test.hint", SUBJECT))
            .apply { addClassName("jvmguard-field-hint") }
        add(VerticalLayout(hint, recipient).apply {
            isPadding = false
            isSpacing = true
        })

        lateinit var send: Button
        send = confirmFooter(t("settings.email.test.send"), ID_SEND) { send(send) }
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
                    Notifications.show(t("settings.email.test.sent"))
                    close()
                } else {
                    sendButton.isEnabled = true
                    Notifications.show(t("settings.email.test.failed", error.message))
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
