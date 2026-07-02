package com.jvmguard.ui.views.setup

import com.github.mvysny.karibudsl.v10.*
import com.jvmguard.common.helper.PasswordHelper
import com.jvmguard.data.config.GroupConfig
import com.jvmguard.ui.components.ErrorDialog
import com.jvmguard.ui.components.PasswordResult
import com.jvmguard.ui.components.PasswordRules
import com.jvmguard.ui.components.Validators
import com.jvmguard.ui.server.Sessions
import com.jvmguard.ui.server.UserSession
import com.jvmguard.ui.views.login.AccountSetupView
import com.jvmguard.ui.views.login.LoginView
import com.jvmguard.ui.views.vms.VmsView
import com.vaadin.flow.component.Key
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.PasswordField
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.auth.AnonymousAllowed

@AnonymousAllowed
@Route("install")
@PageTitle("jvmguard: Set up jvmguard")
class InstallWizardView : VerticalLayout(), BeforeEnterObserver {

    private lateinit var loginName: TextField
    private lateinit var fullName: TextField
    private lateinit var email: TextField
    private lateinit var password: PasswordField
    private lateinit var confirm: PasswordField
    private lateinit var require2fa: Checkbox

    private var passwordPlaintext = ""
    private var built = false

    override fun beforeEnter(event: BeforeEnterEvent) {
        Sessions.captureMock(event.location.queryParameters)
        if (!Sessions.isNewInstallation()) {
            event.forwardTo(LoginView::class.java, event.location.queryParameters)
            return
        }
        if (!built) {
            build()
            built = true
        }
    }

    private fun build() {
        addClassName("jvmguard-login-page")
        setSizeFull()

        verticalLayout {
            addClassNames("jvmguard-login-card", "aura-surface")
            isPadding = false
            isSpacing = false
            width = "420px"

            div { addClassName("jvmguard-login-logo") }
            h2("Set up jvmguard") { addClassName("jvmguard-login-title") }
            span("Create the administrator account to get started.") { addClassName("jvmguard-login-subtitle") }

            loginName = textField("Username") { testId = ID_NAME }
            fullName = textField("Full name (optional)")
            email = textField("Email (optional)") { testId = ID_EMAIL }
            password = passwordField("Password") { testId = ID_PASSWORD }
            confirm = passwordField("Confirm password") { testId = ID_CONFIRM }
            require2fa = checkBox("Require two-factor authentication for all users") {
                value = true
                testId = ID_2FA
                addClassName("jvmguard-settings-gap-before")
            }
            button("Create account") {
                testId = ID_SUBMIT
                addThemeVariants(ButtonVariant.PRIMARY)
                addClickListener { submit() }
                addClickShortcut(Key.ENTER)
            }
        }
    }

    private fun submit() {
        if (validate()) {
            finish()
        }
    }

    private fun validate(): Boolean {
        var valid = true
        if (loginName.value.trim().length !in 2..25) {
            loginName.isInvalid = true
            loginName.errorMessage = "Enter 2 to 25 characters."
            valid = false
        } else {
            loginName.isInvalid = false
        }
        if (email.value.isNotBlank() && !Validators.isValidEmail(email.value)) {
            email.isInvalid = true
            email.errorMessage = "Enter a valid email address."
            valid = false
        } else {
            email.isInvalid = false
        }
        when (val result = PasswordRules.validate(password, confirm, required = true)) {
            is PasswordResult.Valid -> passwordPlaintext = result.plaintext
            else -> valid = false
        }
        return valid
    }

    private fun finish() {
        val name = loginName.value.trim()
        try {
            Sessions.setupService().createInitialUser(
                name,
                fullName.value.trim(),
                email.value.trim(),
                PasswordHelper.createHash(passwordPlaintext),
                require2fa.value,
                GroupConfig.createDefault(),
            )
            val session = UserSession(Sessions.loginService().login(name, passwordPlaintext, null))
            Sessions.setCurrent(session)
            val target = if (session.forcedSetupRequired()) AccountSetupView::class.java else VmsView::class.java
            ui.ifPresent { it.navigate(target) }
        } catch (e: Exception) {
            ErrorDialog("Could not complete setup", e.message ?: e.toString(), null).open()
        }
    }

    companion object {
        const val ID_NAME = "setup-username"
        const val ID_EMAIL = "setup-email"
        const val ID_PASSWORD = "setup-password"
        const val ID_CONFIRM = "setup-confirm"
        const val ID_2FA = "setup-2fa"
        const val ID_SUBMIT = "setup-submit"
    }
}
