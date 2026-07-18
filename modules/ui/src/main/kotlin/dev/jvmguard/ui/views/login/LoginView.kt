package dev.jvmguard.ui.views.login

import com.github.mvysny.karibudsl.v10.*
import dev.jvmguard.connector.api.SsoLoginError
import dev.jvmguard.ui.server.JvmGuardPrincipal
import com.vaadin.flow.component.Component
import dev.jvmguard.ui.server.Sessions
import dev.jvmguard.ui.server.UserSession
import dev.jvmguard.ui.views.setup.InstallWizardView
import dev.jvmguard.ui.views.vms.VmsView
import com.vaadin.flow.component.Key
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.html.Image
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.PasswordField
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.VaadinService
import com.vaadin.flow.server.VaadinServletRequest
import com.vaadin.flow.server.auth.AnonymousAllowed
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.context.SecurityContextHolder

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
            errorMessage = Span().apply {
                testId = ID_ERROR
                isVisible = false
                style.set("color", "var(--aura-red-text)")
                style.set("text-align", "center")
                style.set("font-size", "var(--vaadin-font-size-xs)")
            }
            add(errorMessage)
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

            val ssoProviders = Sessions.loginService().enabledSsoProviders()
            if (ssoProviders.isNotEmpty()) {
                div {
                    addClassName("jvmguard-login-divider")
                    span("OR") { addClassName("jvmguard-login-divider-label") }
                }
                ssoProviders.forEach { provider ->
                    val icon: Component = if (provider.google) {
                        Image("icons/google.svg", "").apply { addClassName("jvmguard-sso-icon") }
                    } else {
                        VaadinIcon.SIGN_IN.create().apply { addClassName("jvmguard-sso-icon") }
                    }
                    val href = "/oauth2/authorization/${provider.registrationId}"
                    add(Button("Sign in with ${provider.displayName}", icon).apply {
                        addClassName("jvmguard-sso-button")
                        addClickListener { ui.ifPresent { it.page.setLocation(href) } }
                    })
                }
            }
        }
    }

    override fun beforeEnter(event: BeforeEnterEvent) {
        // After an SSO callback, Spring Security has authenticated the user but the Vaadin UserSession
        // hasn't been created yet. The redirect chain lands here (LoginView) because no session exists.
        // Detect the SSO principal and create the session, then forward to the main view.
        val principal = SecurityContextHolder.getContext().authentication?.principal
        if (principal is JvmGuardPrincipal && Sessions.current() == null) {
            principal.serverConnection?.let { connection ->
                Sessions.setCurrent(UserSession(connection))
                event.forwardTo(VmsView::class.java)
                return
            }
        }

        val httpRequest = (VaadinService.getCurrentRequest() as? VaadinServletRequest)?.httpServletRequest
        val errorCode = event.location.queryParameters.parameters["ssoError"]?.firstOrNull()
            ?: httpRequest?.session?.getAttribute("ssoError") as? String
        errorCode?.let { httpRequest?.session?.removeAttribute("ssoError") }
        errorCode?.let { code -> SsoLoginError.fromCode(code)?.let { showError(it.message) } }

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
        errorMessage.text = message.substringBefore(". ") + if (message.contains(". ")) "." else ""
        errorMessage.isVisible = true
    }

    companion object {
        const val ID_USERNAME = "login-username"
        const val ID_PASSWORD = "login-password"
        const val ID_AUTHCODE = "login-authcode"
        const val ID_SUBMIT = "login-submit"
        const val ID_ERROR = "login-error"
    }
}
