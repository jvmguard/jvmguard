package com.jvmguard.ui.views.login

import com.jvmguard.common.helper.PasswordHelper
import com.jvmguard.data.user.User
import com.jvmguard.ui.components.ErrorDialog
import com.jvmguard.ui.components.PasswordResult
import com.jvmguard.ui.components.PasswordRules
import com.jvmguard.ui.components.TwoFactorEnroller
import com.jvmguard.ui.server.SecurityBridge
import com.jvmguard.ui.server.Sessions
import com.jvmguard.ui.server.TwoFactor
import com.jvmguard.ui.views.vms.VmsView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.PasswordField
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.PermitAll

@PermitAll
@Route("setup")
@PageTitle("jvmguard: Account setup")
class AccountSetupView : VerticalLayout(), BeforeEnterObserver {

    private val newPassword = PasswordField("New password").apply {
        setWidthFull()
        testId = ID_NEW_PW
    }
    private val confirmPassword = PasswordField("Confirm new password").apply {
        setWidthFull()
        testId = ID_CONFIRM_PW
    }
    private var enroller: TwoFactorEnroller? = null

    private var needPassword = false
    private var needEnroll = false
    private var built = false

    override fun beforeEnter(event: BeforeEnterEvent) {
        val session = Sessions.current()
        if (session == null) {
            event.forwardTo(LoginView::class.java, event.location.queryParameters)
            return
        }
        if (!session.forcedSetupRequired()) {
            event.forwardTo(VmsView::class.java)
            return
        }
        if (!built) {
            build(session.user)
            built = true
        }
    }

    private fun build(user: User?) {
        val globalUse2fa = Sessions.current()?.serverConnection?.getGlobalConfig(false)?.use2fa == true
        needPassword = user?.isMustChangePassword == true
        needEnroll = user != null && TwoFactor.enrollmentRequired(user, globalUse2fa)

        addClassName("jvmguard-login-page")
        setSizeFull()

        val card = VerticalLayout().apply {
            addClassNames("jvmguard-login-card", "aura-surface")
            isPadding = true
            isSpacing = true
            width = "420px"
            testId = ID_VIEW
            add(H2("Complete your account setup").apply { addClassName("jvmguard-login-title") })
        }
        if (needPassword) {
            card.add(
                Span("You must choose a new password.").apply { addClassName("jvmguard-login-subtitle") },
                newPassword, confirmPassword,
            )
        }
        if (needEnroll) {
            card.add(Span("Set up two-factor authentication.").apply { addClassName("jvmguard-login-subtitle") })
            enroller = TwoFactorEnroller(user?.loginName.orEmpty()).also { card.add(it) }
        }
        val finish = Button("Finish") { finish() }.apply {
            addThemeVariants(ButtonVariant.PRIMARY)
            testId = ID_FINISH
        }
        val logout = Button("Log out") { logout() }.apply { testId = ID_LOGOUT }
        card.add(finish, logout)
        add(card)
    }

    private fun finish() {
        val connection = Sessions.current()?.serverConnection ?: return
        val user = connection.user

        if (needPassword) {
            when (val result = PasswordRules.validate(newPassword, confirmPassword, required = true)) {
                is PasswordResult.Valid -> user.passwordHash = PasswordHelper.createHash(result.plaintext)
                else -> return
            }
        }
        if (needEnroll) {
            val secretHex = enroller?.verifiedSecretHex() ?: return
            user.encryptedTotpSecret = connection.encryptTotpSecret(secretHex)
            user.isUse2fa = true
            user.isReset2fa = false
        }
        user.isMustChangePassword = false

        try {
            connection.saveSelf(user)
        } catch (e: Exception) {
            ErrorDialog("Could not complete setup", e.message ?: e.toString(), null).open()
            return
        }
        Sessions.current()?.markSetupComplete()
        UI.getCurrent().navigate(VmsView::class.java)
    }

    private fun logout() {
        SecurityBridge.logout()
    }

    companion object {
        const val ID_VIEW = "account-setup"
        const val ID_NEW_PW = "setup-new-password"
        const val ID_CONFIRM_PW = "setup-confirm-password"
        const val ID_FINISH = "setup-finish"
        const val ID_LOGOUT = "setup-logout"
    }
}
