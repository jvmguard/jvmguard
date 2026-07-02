package com.jvmguard.ui.views.login

import com.github.mvysny.karibudsl.v10.*
import com.jvmguard.ui.server.Sessions
import com.jvmguard.ui.server.UserSession
import com.jvmguard.ui.views.setup.InstallWizardView
import com.jvmguard.ui.views.vms.VmsView
import com.vaadin.flow.component.Key
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.PasswordField
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.auth.AnonymousAllowed
import org.springframework.security.core.AuthenticationException

@AnonymousAllowed
@Route("login")
@PageTitle("jvmguard: Log in")
class LoginView : VerticalLayout(), BeforeEnterObserver {

    internal lateinit var userName: TextField
    internal lateinit var password: PasswordField
    internal lateinit var authCode: TextField
    internal lateinit var loginButton: Button
    private lateinit var errorMessage: Span

    private val use2fa = Sessions.loginService().isUse2fa()

    init {
        addClassName("jvmguard-login-page")
        setSizeFull()

        verticalLayout {
            addClassNames("jvmguard-login-card", "aura-surface")
            isPadding = false
            isSpacing = false
            width = "360px"

            div { addClassName("jvmguard-login-logo") }
            h2("Welcome to jvmguard") { addClassName("jvmguard-login-title") }
            span("Please log in to your account") { addClassName("jvmguard-login-subtitle") }
            userName = textField("Username") { testId = ID_USERNAME }
            password = passwordField("Password") { testId = ID_PASSWORD }
            authCode = textField("Authenticator code") {
                testId = ID_AUTHCODE
                isVisible = use2fa
                width = "10rem"
            }
            loginButton = button("Log in") {
                testId = ID_SUBMIT
                addThemeVariants(ButtonVariant.PRIMARY)
                addClickListener { doLogin() }
                addClickShortcut(Key.ENTER)
            }
            errorMessage = span { isVisible = false }
        }
    }

    override fun beforeEnter(event: BeforeEnterEvent) {
        Sessions.captureMock(event.location.queryParameters)
        if (Sessions.isNewInstallation()) {
            event.forwardTo(InstallWizardView::class.java, event.location.queryParameters)
        }
    }

    private fun doLogin() {
        errorMessage.isVisible = false
        val code = if (use2fa) authCode.value?.takeIf { it.isNotBlank() } else null
        try {
            val connection = Sessions.loginService().login(userName.value, password.value, code)
            val session = UserSession(connection)
            Sessions.setCurrent(session)
            val target = if (session.forcedSetupRequired()) AccountSetupView::class.java else VmsView::class.java
            ui.ifPresent { it.navigate(target) }
        } catch (_: AuthenticationException) {
            showError(if (use2fa) "Invalid user name, password, or authenticator code." else "Invalid user name or password.")
        }
    }

    private fun showError(message: String) {
        errorMessage.text = message
        errorMessage.isVisible = true
    }

    companion object {
        const val ID_USERNAME = "login-username"
        const val ID_PASSWORD = "login-password"
        const val ID_AUTHCODE = "login-authcode"
        const val ID_SUBMIT = "login-submit"
    }
}
