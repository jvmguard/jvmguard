package dev.jvmguard.ui.views.settings

import dev.jvmguard.data.config.SsoGroupMapping
import dev.jvmguard.data.config.SsoPreset
import dev.jvmguard.data.config.SsoProviderConfig
import dev.jvmguard.ui.components.EnumSelect
import dev.jvmguard.ui.components.JvmGuardDialog
import dev.jvmguard.ui.components.Notifications
import dev.jvmguard.ui.components.confirm
import dev.jvmguard.ui.components.editDeleteKeys
import dev.jvmguard.ui.components.menuButton
import dev.jvmguard.ui.server.ServerUrls
import dev.jvmguard.ui.server.Sessions
import dev.jvmguard.ui.server.enumLabel
import dev.jvmguard.ui.server.runInBackground
import dev.jvmguard.ui.server.t
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.formlayout.FormLayout
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.html.H4
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.PasswordField
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.Binder

class SsoProviderDialog(
    private val provider: SsoProviderConfig,
    isNew: Boolean,
    private val existingDisplayNames: Set<String> = emptySet(),
    private val onSave: (SsoProviderConfig) -> Unit,
) : JvmGuardDialog() {

    private val binder = Binder(SsoProviderConfig::class.java)

    private val displayName = TextField(t("settings.sso.provider.displayName")).apply { setWidthFull(); testId = ID_DISPLAY_NAME }
    private val preset = EnumSelect(t("settings.sso.provider.type"), SsoPreset::class.java).apply {
        testId = ID_PRESET
        addValueChangeListener { updatePresetFields() }
    }
    private val issuerUri = TextField(t("settings.sso.provider.issuerUri")).apply { setWidthFull(); testId = ID_ISSUER_URI }
    private val clientId = TextField(t("settings.sso.provider.clientId")).apply {
        setWidthFull(); testId = ID_CLIENT_ID
        helperText = t("settings.sso.provider.clientId.helper")
    }
    private val clientSecret = PasswordField(t("settings.sso.provider.clientSecret")).apply {
        setWidthFull(); testId = ID_CLIENT_SECRET
        helperText = t("settings.sso.provider.clientSecret.helper")
    }
    private val domainRestriction = TextField(t("settings.sso.provider.domain")).apply {
        setWidthFull()
        placeholder = t("settings.sso.provider.domain.placeholder")
        testId = ID_DOMAIN
    }
    private val claimName = TextField(t("settings.sso.provider.claimName")).apply {
        setWidthFull()
        testId = ID_CLAIM_NAME
    }
    private val requireVerifiedEmail = Checkbox(t("settings.sso.provider.requireVerifiedEmail")).apply { testId = ID_REQUIRE_VERIFIED_EMAIL }
    private val enabled = Checkbox(t("common.enabled")).apply { testId = ID_ENABLED }
    private var testButton: Button? = null

    private val redirectUri = TextField(t("settings.sso.provider.redirectUri")).apply {
        isReadOnly = true
        setWidthFull()
        testId = ID_REDIRECT_URI
    }

    private val accessRulesGrid = Grid(SsoGroupMapping::class.java, false).apply {
        testId = ID_RULES_GRID
        addColumn { if (it.isCatchAll) t("settings.sso.rules.catchAllDisplay") else it.claimValue }.setHeader(t("settings.sso.rule.claimValue")).setFlexGrow(1)
        addColumn { enumLabel(it.accessLevel) }.setHeader(t("shell.userInfo.accessLevel")).setAutoWidth(true)
        addComponentColumn { ruleActions(it) }.setFlexGrow(0).setAutoWidth(true)
        addItemDoubleClickListener { editRule(it.item, false) }
        editDeleteKeys({ editRule(it, false) }, ::confirmDeleteRule)
        isAllRowsVisible = true
    }

    init {
        headerTitle = t(if (isNew) "settings.sso.provider.add" else "settings.sso.provider.edit")
        width = "36rem"

        bind()
        binder.readBean(provider)
        updatePresetFields()
        accessRulesGrid.setItems(provider.accessRules)
        displayName.addValueChangeListener { updateRedirectUri() }
        updateRedirectUri()

        val redirectRow = HorizontalLayout(redirectUri, copyRedirectButton()).apply {
            setWidthFull()
            setFlexGrow(1.0, redirectUri)
            defaultVerticalComponentAlignment = FlexComponent.Alignment.END
            isPadding = false
        }
        val redirectHint = Span(t("settings.sso.provider.redirectHint")).apply { addClassName("jvmguard-field-hint") }

        val addRule = Button(t("settings.sso.provider.addRule"), VaadinIcon.PLUS.create()) { editRule(SsoGroupMapping(), true) }.apply {
            addThemeVariants(ButtonVariant.PRIMARY)
            testId = ID_ADD_RULE
        }
        val rulesTitle = H4(t("settings.sso.provider.rules"))
        val rulesHeader = HorizontalLayout(rulesTitle, addRule).apply {
            defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
            setWidthFull()
            isPadding = false
            expand(rulesTitle)
        }
        val rulesHint = Span(t("settings.sso.provider.rulesHint")).apply {
            addClassName("jvmguard-field-hint")
        }

        val testConnection = Button(t("settings.sso.provider.testConnection"), VaadinIcon.CONNECT.create()) { runTestConnection() }.apply {
            testId = ID_TEST_CONNECTION
        }
        testButton = testConnection

        add(VerticalLayout(
            FormLayout(displayName, preset, issuerUri, clientId, clientSecret, domainRestriction, claimName, requireVerifiedEmail, enabled).apply {
                setResponsiveSteps(FormLayout.ResponsiveStep("0", 1))
            },
            redirectRow,
            redirectHint,
            testConnection,
            rulesHeader,
            rulesHint,
            accessRulesGrid,
        ).apply {
            isPadding = false
            isSpacing = true
        })

        confirmFooter(t("common.save"), ID_SAVE) { save() }
    }

    @Suppress("DuplicatedCode")
    private fun bind() {
        binder.forField(displayName)
            .asRequired(t("settings.sso.provider.validation.displayName"))
            .withValidator({ it !in existingDisplayNames }, t("settings.sso.provider.validation.nameExists"))
            .bind({ it.displayName }, { p, v -> p.displayName = v })
        binder.forField(preset)
            .bind({ it.preset }, { p, v -> p.preset = v })
        binder.forField(issuerUri)
            .asRequired(t("settings.sso.provider.validation.issuerUri"))
            .bind({ it.issuerUri }, { p, v -> p.issuerUri = v })
        binder.forField(clientId)
            .bind({ it.clientId }, { p, v -> p.clientId = v })
        binder.forField(clientSecret)
            .bind({ it.clientSecret }, { p, v -> p.clientSecret = v })
        binder.forField(domainRestriction)
            .bind({ it.domainRestriction }, { p, v -> p.domainRestriction = v })
        binder.forField(claimName)
            .bind({ it.claimName }, { p, v -> p.claimName = v })
        binder.forField(requireVerifiedEmail)
            .bind({ it.requireVerifiedEmail }, { p, v -> p.requireVerifiedEmail = v })
        binder.forField(enabled)
            .bind({ it.enabled }, { p, v -> p.enabled = v })
    }

    private fun updatePresetFields() {
        val p = preset.value ?: return
        requireVerifiedEmail.isVisible = !p.emailAlwaysVerified
        if (p == SsoPreset.GOOGLE_WORKSPACE) {
            displayName.value = "Google"
            displayName.isEnabled = false
            issuerUri.isVisible = false
            issuerUri.value = SsoPreset.defaultIssuer(p)
            claimName.isVisible = false
            domainRestriction.label = t("settings.sso.provider.hostedDomain")
            domainRestriction.placeholder = t("settings.sso.provider.hostedDomain.placeholder")
        } else {
            displayName.isEnabled = true
            issuerUri.isVisible = true
            claimName.isVisible = (p == SsoPreset.GENERIC_OIDC)
            domainRestriction.label = t("settings.sso.provider.domain")
            domainRestriction.placeholder = t("settings.sso.provider.domain.placeholder")
        }
        updateRedirectUri()
    }

    private fun updateRedirectUri() {
        val slug = SsoProviderConfig.slugify(displayName.value ?: "")
        redirectUri.value = "${ServerUrls.baseUrl()}/login/oauth2/code/$slug"
    }

    private fun copyRedirectButton(): Button = Button(VaadinIcon.COPY.create()) {
        redirectUri.element.executeJs("navigator.clipboard && navigator.clipboard.writeText(this.value)")
    }.apply {
        addThemeVariants(ButtonVariant.TERTIARY)
        setAriaLabel(t("settings.sso.provider.copyRedirect.aria"))
        setTooltipText(t("settings.sso.provider.copyRedirect.tooltip"))
    }

    private fun ruleActions(rule: SsoGroupMapping): Component =
        menuButton(VaadinIcon.ELLIPSIS_DOTS_V, t("settings.sso.rules.actions"), "$ID_RULES_ROW_MENU-${rule.claimValue}") {
            addItem(t("common.edit")) { editRule(rule, false) }
            addItem(t("common.delete")) { confirmDeleteRule(rule) }
        }

    private fun editRule(rule: SsoGroupMapping, isNew: Boolean) {
        val workingCopy = if (isNew) rule else {
            SsoGroupMapping().apply {
                claimValue = rule.claimValue
                accessLevel = rule.accessLevel
            }
        }
        val hasCatchAll = provider.accessRules.any { it.isCatchAll && it !== rule }
        SsoGroupMappingDialog(workingCopy, isNew,
            groupsSupported = provider.preset.supportsGroups,
            catchAllExists = hasCatchAll,
        ) { saved ->
            if (isNew) {
                provider.accessRules.add(saved)
            } else {
                val index = provider.accessRules.indexOfFirst { it === rule }
                if (index >= 0) {
                    provider.accessRules[index] = saved
                }
            }
            accessRulesGrid.setItems(provider.accessRules)
        }.open()
    }

    private fun confirmDeleteRule(rule: SsoGroupMapping) {
        confirm(t("settings.sso.rules.deleteTitle"), t("settings.sso.rules.deleteText", if (rule.isCatchAll) "*" else rule.claimValue), t("common.delete")) {
            provider.accessRules.remove(rule)
            accessRulesGrid.setItems(provider.accessRules)
        }
    }

    private fun save() {
        if (!binder.writeBeanIfValid(provider)) {
            return
        }
        onSave(provider)
        close()
    }

    private fun runTestConnection() {
        val issuer = issuerUri.value.trim()
        if (issuer.isBlank()) {
            Notifications.show(t("settings.sso.provider.enterIssuerFirst"))
            return
        }
        val connection = Sessions.current()?.serverConnection ?: return
        val button = testButton ?: return
        button.text = t("settings.sso.provider.testing")
        button.isEnabled = false
        val ui = UI.getCurrent()
        runInBackground {
            val result = connection.testSsoDiscovery(issuer)
            ui.access {
                Notifications.show(t(result.messageKey, *result.messageParams))
                button.text = t("settings.sso.provider.testConnection")
                button.isEnabled = true
            }
        }
    }

    companion object {
        const val ID_DISPLAY_NAME = "sso-provider-display-name"
        const val ID_PRESET = "sso-provider-preset"
        const val ID_ISSUER_URI = "sso-provider-issuer"
        const val ID_CLIENT_ID = "sso-provider-client-id"
        const val ID_CLIENT_SECRET = "sso-provider-client-secret"
        const val ID_DOMAIN = "sso-provider-domain"
        const val ID_CLAIM_NAME = "sso-provider-claim-name"
        const val ID_REQUIRE_VERIFIED_EMAIL = "sso-provider-require-verified-email"
        const val ID_ENABLED = "sso-provider-enabled"
        const val ID_REDIRECT_URI = "sso-provider-redirect-uri"
        const val ID_TEST_CONNECTION = "sso-provider-test-connection"
        const val ID_RULES_GRID = "sso-rules-grid"
        const val ID_ADD_RULE = "sso-add-rule"
        const val ID_RULES_ROW_MENU = "sso-rules-row-menu"
        const val ID_SAVE = "sso-provider-save"
    }
}
