package dev.jvmguard.ui.views.account

import dev.jvmguard.common.helper.PasswordHelper
import dev.jvmguard.data.user.User
import dev.jvmguard.data.user.UserType
import dev.jvmguard.ui.components.PasswordResult
import dev.jvmguard.ui.components.PasswordRules
import dev.jvmguard.ui.components.Validators
import dev.jvmguard.ui.server.Sessions
import dev.jvmguard.ui.server.t
import dev.jvmguard.ui.shell.MainLayout
import dev.jvmguard.ui.views.settings.AbstractAccountSectionView
import dev.jvmguard.ui.views.settings.settingsSection
import com.vaadin.flow.component.AttachEvent
import com.vaadin.flow.component.formlayout.FormLayout
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.EmailField
import com.vaadin.flow.component.textfield.PasswordField
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.Route
import jakarta.annotation.security.PermitAll

@PermitAll
@Route(value = "account/profile", layout = MainLayout::class)
class AccountProfileView : AbstractAccountSectionView() {

    private val fullName = TextField(t("account.profile.fullName")).apply {
        setWidthFull()
        testId = ID_FULL_NAME
    }
    private val email = EmailField(t("account.profile.email")).apply {
        isClearButtonVisible = true
        setWidthFull()
        testId = ID_EMAIL
    }

    private val currentPassword = passwordField(t("account.password.current"), ID_CURRENT_PW)
    private val newPassword = passwordField(t("account.password.new"), ID_NEW_PW)
    private val confirmPassword = passwordField(t("account.password.confirmNew"), ID_CONFIRM_PW)

    private val isOidc = Sessions.current()?.user?.userType == UserType.OIDC

    private val use2faEnabled = Sessions.current()?.serverConnection?.getGlobalConfig(false)?.use2fa == true
    private var twoFactorSection: TwoFactorSection? = null

    init {
        if (isOidc) {
            val user = Sessions.current()!!.user
            val ssoInfo = VerticalLayout(
                Span(t("account.sso.signedInVia", user.ssoIssuer)).apply { style.set("font-weight", "bold") },
                Span(t("account.sso.email", user.loginName)),
            ).apply { isPadding = false; isSpacing = true }
            user.fullName.takeIf { it.isNotBlank() }?.let { ssoInfo.add(Span(t("account.sso.name", it))) }
            add(settingsSection(t("account.sso.section"), ssoInfo))
        } else {
            val passwordHint = Span(t("account.password.hint")).apply { addClassName("jvmguard-field-hint") }
            add(
                settingsSection(t("nav.account.profile"), FormLayout(fullName, email)),
                settingsSection(t("account.password.section"), VerticalLayout(currentPassword, newPassword, confirmPassword, passwordHint).apply {
                    isPadding = false
                    isSpacing = true
                }),
            )
            if (use2faEnabled) {
                twoFactorSection = TwoFactorSection().also { add(it) }
            }
        }
    }

    override fun onAttach(attachEvent: AttachEvent) {
        super.onAttach(attachEvent)
        twoFactorSection?.refresh()
    }

    @Suppress("DuplicatedCode")
    override fun bind(binder: Binder<User>) {
        if (isOidc) {
            return
        }
        binder.forField(fullName).bind({ it.fullName }, { u, value -> u.fullName = value })
        binder.forField(email)
            .withValidator(Validators.optionalEmail())
            .bind({ it.email }, { u, value -> u.email = value })
    }

    override fun applyToDraft() {
        super.applyToDraft()
        if (!isOidc) {
            checkPassword(apply = true)
        }
    }

    override fun isValid(): Boolean {
        if (isOidc) {
            return true
        }
        val passwordOk = checkPassword(apply = false)
        return binder.validate().isOk && passwordOk
    }

    private fun checkPassword(apply: Boolean): Boolean {
        val user = Sessions.peekAccountDraft()?.user ?: return true
        return when (val result = PasswordRules.validate(newPassword, confirmPassword, currentPassword, user.passwordHash)) {
            is PasswordResult.Invalid -> false
            is PasswordResult.Unchanged -> true
            is PasswordResult.Valid -> {
                if (apply) {
                    user.passwordHash = PasswordHelper.createHash(result.plaintext)
                }
                true
            }
        }
    }

    private fun passwordField(label: String, id: String): PasswordField =
        PasswordField(label).apply {
            setWidthFull()
            testId = id
            addValueChangeListener { Sessions.accountDraft().markDirty() }
        }

    companion object {
        const val ID_FULL_NAME = "account-full-name"
        const val ID_EMAIL = "account-email"
        const val ID_CURRENT_PW = "account-current-password"
        const val ID_NEW_PW = "account-new-password"
        const val ID_CONFIRM_PW = "account-confirm-password"
    }
}
