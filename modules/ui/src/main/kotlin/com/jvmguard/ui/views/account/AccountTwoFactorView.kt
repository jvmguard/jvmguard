package com.jvmguard.ui.views.account

import com.jvmguard.data.user.User
import com.jvmguard.data.user.UserType
import com.jvmguard.ui.components.TwoFactorEnroller
import com.jvmguard.ui.server.Sessions
import com.jvmguard.ui.shell.MainLayout
import com.jvmguard.ui.views.settings.AbstractAccountSectionView
import com.jvmguard.ui.views.settings.settingsSection
import com.vaadin.flow.component.AttachEvent
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.PermitAll

@PermitAll
@Route(value = "account/two-factor", layout = MainLayout::class)
@PageTitle("jvmguard: Account")
class AccountTwoFactorView : AbstractAccountSectionView() {

    private val body = VerticalLayout().apply {
        isPadding = false
        isSpacing = false
        setWidthFull()
    }
    private var enroller: TwoFactorEnroller? = null
    private var enrolling = false

    init {
        add(body)
    }

    override fun bind(binder: Binder<User>) {}

    override fun onAttach(attachEvent: AttachEvent) {
        super.onAttach(attachEvent)
        render()
    }

    private fun render() {
        body.removeAll()
        if (isOidc()) {
            addInfoText("Two-factor authentication is handled by the single sign-on provider.")
            return
        }
        if (!globalUse2fa()) {
            addInfoText("Two-factor authentication is disabled globally.")
            return
        }
        if (enrolling) {
            renderEnrolling()
        } else {
            renderStatus()
        }
    }

    private fun addInfoText(text: String) {
        body.add(
            settingsSection(
                "Two-factor authentication",
                Span(text).apply { addClassName("jvmguard-field-hint") })
        )
    }

    private fun renderEnrolling() {
        val enrollerComponent = TwoFactorEnroller(Sessions.accountDraft().user.loginName).also { enroller = it }
        val use = Button("Use this authenticator") { stageEnrollment() }.apply {
            addThemeVariants(ButtonVariant.PRIMARY)
            testId = ID_USE
        }
        val cancel = Button("Cancel") { enrolling = false; render() }
        body.add(settingsSection("Set up authenticator", enrollerComponent, actionRow(use, cancel)))
    }

    private fun renderStatus() {
        val draft = Sessions.accountDraft()
        val exempt = draft.user.isExemptFrom2fa
        val enabled = draft.pendingUse2fa ?: draft.user.isUse2fa
        val status = Span(
            when {
                enabled && exempt -> "Two-factor authentication is enabled (optional for your account)."
                enabled -> "Two-factor authentication is enabled."
                else -> "Two-factor authentication is not enabled (optional for your account)."
            },
        )
        val actions = mutableListOf<Button>()
        if (enabled) {
            actions += Button("Reconfigure authenticator") { enrolling = true; render() }.apply { testId = ID_RECONFIGURE }
            if (exempt) {
                actions += Button("Disable") { disable() }.apply { testId = ID_DISABLE }
            }
        } else {
            actions += Button("Enable two-factor authentication") { enrolling = true; render() }.apply {
                addThemeVariants(ButtonVariant.PRIMARY)
                testId = ID_ENABLE
            }
        }
        val components = mutableListOf<Component>(status)
        if (draft.pendingTotpSecretHex != null || draft.pendingUse2fa != null) {
            components += Span("Pending. Click Save to apply.").apply { addClassName("jvmguard-field-hint") }
        }
        components += actionRow(*actions.toTypedArray())
        body.add(settingsSection("Two-factor authentication", *components.toTypedArray()))
    }

    private fun stageEnrollment() {
        val secretHex = enroller?.verifiedSecretHex() ?: return
        Sessions.accountDraft().apply {
            pendingTotpSecretHex = secretHex
            pendingUse2fa = true
            markDirty()
        }
        enrolling = false
        render()
    }

    private fun disable() {
        Sessions.accountDraft().apply {
            pendingTotpSecretHex = null
            pendingUse2fa = false
            markDirty()
        }
        render()
    }

    private fun actionRow(vararg buttons: Button) = HorizontalLayout(*buttons).apply {
        defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
        isPadding = false
    }

    private fun isOidc(): Boolean =
        Sessions.current()?.user?.userType == UserType.OIDC

    private fun globalUse2fa(): Boolean =
        Sessions.current()?.serverConnection?.getGlobalConfig(false)?.use2fa == true

    companion object {
        const val ID_ENABLE = "twofactor-enable"
        const val ID_RECONFIGURE = "twofactor-reconfigure"
        const val ID_DISABLE = "twofactor-disable"
        const val ID_USE = "twofactor-use"
    }
}
