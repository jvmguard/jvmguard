package com.jvmguard.ui.views.settings

import com.jvmguard.data.config.GlobalConfig
import com.jvmguard.data.config.SmtpConfig
import com.jvmguard.data.config.SmtpConfig.Encryption
import com.jvmguard.data.user.Roles
import com.jvmguard.ui.components.EnumSelect
import com.jvmguard.ui.components.Validators
import com.jvmguard.ui.shell.MainLayout
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.textfield.EmailField
import com.vaadin.flow.component.textfield.IntegerField
import com.vaadin.flow.component.textfield.PasswordField
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed

@RolesAllowed(Roles.ADMIN)
@Route(value = "settings/email", layout = MainLayout::class)
@PageTitle("jvmguard: Settings")
class SmtpSettingsView : AbstractSettingsSectionView() {

    private val fromEmail = EmailField("Sender email").apply {
        setWidthFull()
        testId = ID_FROM
    }
    private val host = TextField("SMTP host").apply {
        setWidthFull()
        testId = ID_HOST
    }
    private val port = IntegerField("SMTP port").apply {
        min = 1
        max = 65535
        width = "8rem"
        testId = ID_PORT
    }
    private val encryption = EnumSelect("Encryption", Encryption::class.java) { it.toString() }
    private val authenticate = Checkbox("Authenticate with the SMTP server").apply {
        addClassName("jvmguard-settings-gap-before")
        testId = ID_AUTHENTICATE
        addValueChangeListener { updateAuthEnabled() }
    }
    private val userName = TextField("Username").apply {
        setWidthFull()
        testId = ID_USER
    }
    private val password = PasswordField("Password").apply {
        setWidthFull()
        testId = ID_PASSWORD
    }
    private val testMail = Button("Send test email") { sendTestMail() }.apply {
        addClassName("jvmguard-settings-gap-before")
        testId = ID_TEST
    }

    init {
        add(settingsSection("E-Mail", fromEmail, host, port, encryption, authenticate, userName, password, testMail))
        updateAuthEnabled()
    }

    @Suppress("DuplicatedCode")
    override fun bind(binder: Binder<GlobalConfig>) {
        binder.forField(fromEmail)
            .withValidator(Validators.optionalEmail())
            .bind({ it.smtpConfig.fromEmail }, { config, value -> config.smtpConfig.fromEmail = value })
        binder.forField(host)
            .bind({ it.smtpConfig.host }, { config, value -> config.smtpConfig.host = value })
        binder.forField(port)
            .asRequired("Enter a port.")
            .bind({ it.smtpConfig.port }, { config, value -> config.smtpConfig.port = value })
        binder.forField(encryption)
            .bind({ it.smtpConfig.encryption }, { config, value -> config.smtpConfig.encryption = value })
        binder.forField(authenticate)
            .bind({ it.smtpConfig.isAuthenticate }, { config, value -> config.smtpConfig.isAuthenticate = value })
        binder.forField(userName)
            .bind({ it.smtpConfig.userName }, { config, value -> config.smtpConfig.userName = value })
        binder.forField(password)
            .bind({ it.smtpConfig.password }, { config, value -> config.smtpConfig.password = value })
    }

    private fun updateAuthEnabled() {
        userName.isEnabled = authenticate.value
        password.isEnabled = authenticate.value
    }

    private fun sendTestMail() {
        if (!binder.validate().isOk) {
            return
        }
        val testConfig = SmtpConfig().apply {
            fromEmail = this@SmtpSettingsView.fromEmail.value.orEmpty()
            host = this@SmtpSettingsView.host.value.orEmpty()
            port = this@SmtpSettingsView.port.value ?: SmtpConfig().port
            encryption = this@SmtpSettingsView.encryption.value
            isAuthenticate = this@SmtpSettingsView.authenticate.value
            userName = this@SmtpSettingsView.userName.value.orEmpty()
            password = this@SmtpSettingsView.password.value.orEmpty()
        }
        SendTestMailDialog(testConfig).open()
    }

    companion object {
        const val ID_FROM = "settings-smtp-from"
        const val ID_HOST = "settings-smtp-host"
        const val ID_PORT = "settings-smtp-port"
        const val ID_AUTHENTICATE = "settings-smtp-authenticate"
        const val ID_USER = "settings-smtp-user"
        const val ID_PASSWORD = "settings-smtp-password"
        const val ID_TEST = "settings-smtp-test"
    }
}
