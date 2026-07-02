package com.jvmguard.ui.views.account

import com.jvmguard.common.helper.PasswordHelper
import com.jvmguard.data.user.User
import com.jvmguard.ui.components.PasswordResult
import com.jvmguard.ui.components.PasswordRules
import com.jvmguard.ui.components.Validators
import com.jvmguard.ui.server.Sessions
import com.jvmguard.ui.shell.MainLayout
import com.jvmguard.ui.views.settings.AbstractAccountSectionView
import com.jvmguard.ui.views.settings.settingsSection
import com.vaadin.flow.component.formlayout.FormLayout
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.EmailField
import com.vaadin.flow.component.textfield.PasswordField
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.PermitAll

@PermitAll
@Route(value = "account/profile", layout = MainLayout::class)
@PageTitle("jvmguard: Account")
class AccountProfileView : AbstractAccountSectionView() {

    private val fullName = TextField("Full name").apply {
        setWidthFull()
        testId = ID_FULL_NAME
    }
    private val email = EmailField("Email").apply {
        isClearButtonVisible = true
        setWidthFull()
        testId = ID_EMAIL
    }

    private val currentPassword = passwordField("Current password", ID_CURRENT_PW)
    private val newPassword = passwordField("New password", ID_NEW_PW)
    private val confirmPassword = passwordField("Confirm new password", ID_CONFIRM_PW)

    init {
        val passwordHint = Span("Leave blank to keep your current password.").apply { addClassName("jvmguard-field-hint") }
        add(
            settingsSection("User information", FormLayout(fullName, email)),
            settingsSection("Change password", VerticalLayout(currentPassword, newPassword, confirmPassword, passwordHint).apply {
                isPadding = false
                isSpacing = true
            }),
        )
    }

    @Suppress("DuplicatedCode")
    override fun bind(binder: Binder<User>) {
        binder.forField(fullName).bind({ it.fullName }, { u, value -> u.fullName = value })
        binder.forField(email)
            .withValidator(Validators.optionalEmail())
            .bind({ it.email }, { u, value -> u.email = value })
    }

    override fun applyToDraft() {
        super.applyToDraft()
        checkPassword(apply = true)
    }

    override fun isValid(): Boolean {
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
