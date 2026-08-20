package dev.jvmguard.ui.views.settings

import dev.jvmguard.common.helper.GroupHelper
import dev.jvmguard.common.helper.PasswordHelper
import dev.jvmguard.data.user.AccessLevel
import dev.jvmguard.data.user.User
import dev.jvmguard.data.user.UserType
import dev.jvmguard.ui.components.*
import dev.jvmguard.ui.server.t
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.combobox.MultiSelectComboBox
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.EmailField
import com.vaadin.flow.component.textfield.PasswordField
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.Binder

class UserEditDialog(
    private val user: User,
    private val isNew: Boolean,
    private val existingLoginNames: Set<String>,
    private val use2faEnabled: Boolean,
    private val groupPaths: List<String>,
    private val onSave: (User) -> Unit,
) : JvmGuardDialog() {

    private val binder = Binder(User::class.java)

    private val userType = EnumSelect(t("settings.users.edit.userType"), UserType::class.java).apply {
        addValueChangeListener { updateTypeVisibility() }
    }
    private val loginName = TextField(t("settings.users.edit.loginName")).apply {
        setWidthFull()
        testId = ID_LOGIN_NAME
    }
    private val ssoEmail = EmailField(t("settings.users.edit.ssoEmail")).apply {
        setWidthFull()
        testId = ID_SSO_EMAIL
    }
    private val ldapDn = TextField(t("settings.users.edit.ldapDn")).apply {
        setWidthFull()
        testId = ID_LDAP_DN
    }
    private val fullName = TextField(t("settings.users.edit.fullName")).apply {
        setWidthFull()
        testId = ID_FULL_NAME
    }
    private val email = EmailField(t("settings.users.edit.email")).apply {
        isClearButtonVisible = true
        setWidthFull()
        testId = ID_EMAIL
    }
    private val accessLevel = EnumSelect(t("shell.userInfo.accessLevel"), AccessLevel::class.java).apply {
        addValueChangeListener { updateGroupsEnabled() }
        testId = ID_ACCESS_LEVEL
    }
    private val associatedGroups = MultiSelectComboBox<String>(t("settings.users.edit.associatedGroups")).apply {
        setItems(listOf(GroupHelper.ROOT_GROUP_ID) + groupPaths)
        setItemLabelGenerator { if (it == GroupHelper.ROOT_GROUP_ID) ALL_GROUPS_LABEL else it }
        setWidthFull()
    }

    private val changePassword = Checkbox(t("settings.users.edit.changePassword")).apply {
        isVisible = !isNew
        addValueChangeListener { updatePasswordVisibility() }
        testId = ID_CHANGE_PASSWORD
    }
    private val newPassword = PasswordField(t(if (isNew) "settings.users.edit.password" else "settings.users.edit.newPassword")).apply {
        setWidthFull()
        testId = ID_PASSWORD
    }
    private val confirmPassword = PasswordField(t("settings.users.edit.confirmPassword")).apply {
        setWidthFull()
        testId = ID_CONFIRM_PASSWORD
    }
    private val mustChangePassword = Checkbox(t("settings.users.edit.mustChange")).apply {
        testId = ID_MUST_CHANGE
    }

    private val reset2fa = Checkbox(t("settings.users.edit.reset2fa")).apply { isVisible = use2faEnabled && !isNew }
    private val exemptFrom2fa = Checkbox(t("settings.users.edit.exempt2fa")).apply { isVisible = use2faEnabled }

    private val passwordSection = VerticalLayout(changePassword, newPassword, confirmPassword, mustChangePassword).apply {
        isPadding = false
        isSpacing = true
    }

    init {
        headerTitle = t(if (isNew) "settings.users.addUser" else "settings.users.editUser")
        width = "32rem"

        bind()
        binder.readBean(user)
        ssoEmail.value = user.loginName
        updateTypeVisibility()
        updateGroupsEnabled()
        updatePasswordVisibility()

        add(
            VerticalLayout(
                userType, loginName, ssoEmail, ldapDn, fullName, email, accessLevel,
                associatedGroups, passwordSection, reset2fa, exemptFrom2fa,
            ).apply {
                isPadding = false
                isSpacing = true
            })

        confirmFooter(t("common.save"), ID_SAVE) { save() }
    }

    @Suppress("DuplicatedCode")
    private fun bind() {
        binder.forField(loginName)
            .asRequired(t("settings.users.edit.validation.loginName"))
            .withValidator({ it.length in 2..25 }, t("settings.users.edit.validation.loginLength"))
            .withValidator({ it !in existingLoginNames }, t("settings.users.edit.validation.loginTaken"))
            .bind({ it.loginName }, { u, value -> u.loginName = value })
        binder.forField(userType)
            .bind({ it.userType }, { u, value -> u.userType = value })
        binder.forField(ldapDn)
            .bind({ it.ldapDn }, { u, value -> u.ldapDn = value })
        binder.forField(fullName)
            .bind({ it.fullName }, { u, value -> u.fullName = value })
        binder.forField(email)
            .withValidator(Validators.optionalEmail())
            .bind({ it.email }, { u, value -> u.email = value })
        binder.forField(accessLevel)
            .bind({ it.accessLevel }, { u, value -> u.accessLevel = value })
        binder.forField(associatedGroups)
            .bind({ it.groupNames.toSet() }, { u, value -> u.groupNames = ArrayList(value) })
        binder.forField(mustChangePassword)
            .bind({ it.isMustChangePassword }, { u, value -> u.isMustChangePassword = value })
        binder.forField(reset2fa)
            .bind({ it.isReset2fa }, { u, value -> u.isReset2fa = value })
        binder.forField(exemptFrom2fa)
            .bind({ it.isExemptFrom2fa }, { u, value -> u.isExemptFrom2fa = value })
    }

    private fun isLocal(): Boolean = userType.value == UserType.LOCAL

    private fun isLdap(): Boolean = userType.value == UserType.LDAP

    private fun isOidc(): Boolean = userType.value == UserType.OIDC

    private fun updateTypeVisibility() {
        val local = isLocal()
        val ldap = isLdap()
        val oidc = isOidc()
        loginName.isVisible = !oidc
        ssoEmail.isVisible = oidc
        ldapDn.isVisible = ldap
        fullName.isVisible = local
        email.isVisible = local
        passwordSection.isVisible = local
        reset2fa.isVisible = use2faEnabled && !oidc && !isNew
        exemptFrom2fa.isVisible = use2faEnabled && !oidc
        updatePasswordVisibility()
    }

    private fun updateGroupsEnabled() {
        // VIEWER/ADMIN see every group, only PROFILER is restricted to an explicit set.
        associatedGroups.isEnabled = accessLevel.value?.isAllGroups == false
    }

    private fun updatePasswordVisibility() {
        val show = isLocal() && (isNew || changePassword.value)
        newPassword.isVisible = show
        confirmPassword.isVisible = show
    }

    private fun save() {
        newPassword.isInvalid = false
        confirmPassword.isInvalid = false
        if (isOidc()) {
            if (ssoEmail.isInvalid || ssoEmail.value.isNullOrBlank()) {
                return
            }
            if (ssoEmail.value in existingLoginNames) {
                ssoEmail.errorMessage = t("settings.users.edit.validation.loginTaken")
                ssoEmail.isInvalid = true
                return
            }
        }
        if (!binder.writeBeanIfValid(user) || !applyPassword()) {
            return
        }
        if (isOidc()) {
            user.loginName = ssoEmail.value
            user.email = user.loginName
        }
        onSave(user)
        close()
    }

    private fun applyPassword(): Boolean {
        if (!isLocal() || (!isNew && !changePassword.value)) {
            newPassword.isInvalid = false
            confirmPassword.isInvalid = false
            return true
        }
        return when (val result = PasswordRules.validate(newPassword, confirmPassword, required = true)) {
            is PasswordResult.Valid -> {
                user.passwordHash = PasswordHelper.createHash(result.plaintext)
                true
            }

            else -> false
        }
    }

    companion object {
        val ALL_GROUPS_LABEL: String get() = t("settings.users.allGroups")
        const val ID_LOGIN_NAME = "user-login-name"
        const val ID_SSO_EMAIL = "user-sso-email"
        const val ID_LDAP_DN = "user-ldap-dn"
        const val ID_FULL_NAME = "user-full-name"
        const val ID_EMAIL = "user-email"
        const val ID_ACCESS_LEVEL = "user-access-level"
        const val ID_CHANGE_PASSWORD = "user-change-password"
        const val ID_PASSWORD = "user-password"
        const val ID_CONFIRM_PASSWORD = "user-confirm-password"
        const val ID_MUST_CHANGE = "user-must-change-password"
        const val ID_SAVE = "user-save"
    }
}
