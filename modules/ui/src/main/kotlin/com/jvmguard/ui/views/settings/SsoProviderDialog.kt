package com.jvmguard.ui.views.settings

import com.jvmguard.data.config.SsoGroupMapping
import com.jvmguard.data.config.SsoPreset
import com.jvmguard.data.config.SsoProviderConfig
import com.jvmguard.ui.components.EnumSelect
import com.jvmguard.ui.components.JvmGuardDialog
import com.jvmguard.ui.components.Notifications
import com.jvmguard.ui.components.confirm
import com.jvmguard.ui.components.editDeleteKeys
import com.jvmguard.ui.components.menuButton
import com.jvmguard.ui.server.Sessions
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import org.springframework.security.core.context.SecurityContextHolder
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
    private val onSave: (SsoProviderConfig) -> Unit,
) : JvmGuardDialog() {

    private val binder = Binder(SsoProviderConfig::class.java)

    private val displayName = TextField("Display name").apply { setWidthFull(); testId = ID_DISPLAY_NAME }
    private val preset = EnumSelect("Provider type", SsoPreset::class.java) { it.toString() }.apply {
        testId = ID_PRESET
        addValueChangeListener { updatePresetFields() }
    }
    private val issuerUri = TextField("Issuer URI").apply { setWidthFull(); testId = ID_ISSUER_URI }
    private val clientId = TextField("Client ID").apply { setWidthFull(); testId = ID_CLIENT_ID }
    private val clientSecret = PasswordField("Client secret").apply { setWidthFull(); testId = ID_CLIENT_SECRET }
    private val domainRestriction = TextField("Domain / tenant").apply {
        setWidthFull()
        placeholder = "e.g. yourco.com"
        testId = ID_DOMAIN
    }
    private val claimName = TextField("Group claim name").apply {
        setWidthFull()
        testId = ID_CLAIM_NAME
    }
    private val requireVerifiedEmail = Checkbox("Require verified email").apply { testId = ID_REQUIRE_VERIFIED_EMAIL }
    private val enabled = Checkbox("Enabled").apply { testId = ID_ENABLED }
    private var testButton: Button? = null

    private val accessRulesGrid = Grid(SsoGroupMapping::class.java, false).apply {
        testId = ID_RULES_GRID
        addColumn { if (it.isCatchAll) "* (everyone else)" else it.claimValue }.setHeader("Group claim value").setFlexGrow(1)
        addColumn { it.accessLevel.toString() }.setHeader("Access level").setAutoWidth(true)
        addComponentColumn { ruleActions(it) }.setFlexGrow(0).setAutoWidth(true)
        addItemDoubleClickListener { editRule(it.item, false) }
        editDeleteKeys({ editRule(it, false) }, ::confirmDeleteRule)
        isAllRowsVisible = true
    }

    init {
        headerTitle = if (isNew) "Add SSO provider" else "Edit SSO provider"
        width = "36rem"

        bind()
        binder.readBean(provider)
        updatePresetFields()
        accessRulesGrid.setItems(provider.accessRules)

        val addRule = Button("Add rule", VaadinIcon.PLUS.create()) { editRule(SsoGroupMapping(), true) }.apply {
            addThemeVariants(ButtonVariant.PRIMARY)
            testId = ID_ADD_RULE
        }
        val rulesTitle = H4("Access rules")
        val rulesHeader = HorizontalLayout(rulesTitle, addRule).apply {
            defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
            setWidthFull()
            isPadding = false
            expand(rulesTitle)
        }
        val rulesHint = Span("(empty = only pre-created users user with type \"SSO\" can sign in)").apply {
            addClassName("jvmguard-field-hint")
        }

        val testConnection = Button("Test connection", VaadinIcon.CONNECT.create()) { runTestConnection() }.apply {
            testId = ID_TEST_CONNECTION
        }
        testButton = testConnection

        add(VerticalLayout(
            FormLayout(displayName, preset, issuerUri, clientId, clientSecret, domainRestriction, claimName, requireVerifiedEmail, enabled).apply {
                setResponsiveSteps(FormLayout.ResponsiveStep("0", 1))
            },
            testConnection,
            rulesHeader,
            rulesHint,
            accessRulesGrid,
        ).apply {
            isPadding = false
            isSpacing = true
        })

        confirmFooter("Save", ID_SAVE) { save() }
    }

    private fun bind() {
        binder.forField(displayName)
            .asRequired("Enter a display name.")
            .bind({ it.displayName }, { p, v -> p.displayName = v })
        binder.forField(preset)
            .bind({ it.preset }, { p, v -> p.preset = v })
        binder.forField(issuerUri)
            .asRequired("Enter an issuer URI.")
            .bind({ it.issuerUri }, { p, v -> p.issuerUri = v })
        binder.forField(clientId)
            .asRequired("Enter a client ID.")
            .bind({ it.clientId }, { p, v -> p.clientId = v })
        binder.forField(clientSecret)
            .asRequired("Enter a client secret.")
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
            issuerUri.isVisible = false
            issuerUri.value = SsoPreset.defaultIssuer(p)
            claimName.isVisible = false
            domainRestriction.label = "Hosted domain"
            domainRestriction.placeholder = "yourco.com"
        } else {
            issuerUri.isVisible = true
            claimName.isVisible = (p == SsoPreset.GENERIC_OIDC)
            domainRestriction.label = "Domain / tenant"
            domainRestriction.placeholder = "e.g. yourco.com"
        }
    }

    private fun ruleActions(rule: SsoGroupMapping): Component =
        menuButton(VaadinIcon.ELLIPSIS_DOTS_V, "Actions for this rule", "$ID_RULES_ROW_MENU-${rule.claimValue}") {
            addItem("Edit") { editRule(rule, false) }
            addItem("Delete") { confirmDeleteRule(rule) }
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
        confirm("Delete rule", "Delete the access rule for \"${if (rule.isCatchAll) "*" else rule.claimValue}\"?", "Delete") {
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
            Notifications.show("Enter an issuer URI first.")
            return
        }
        val connection = Sessions.current()?.serverConnection ?: return
        val button = testButton ?: return
        button.text = "Testing..."
        button.isEnabled = false
        val ui = UI.getCurrent()
        val securityContext = SecurityContextHolder.getContext()
        Thread.startVirtualThread {
            SecurityContextHolder.setContext(securityContext)
            try {
                val result = connection.testSsoDiscovery(issuer)
                ui.access {
                    Notifications.show(result)
                    button.text = "Test connection"
                    button.isEnabled = true
                }
            } finally {
                SecurityContextHolder.clearContext()
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
        const val ID_TEST_CONNECTION = "sso-provider-test-connection"
        const val ID_RULES_GRID = "sso-rules-grid"
        const val ID_ADD_RULE = "sso-add-rule"
        const val ID_RULES_ROW_MENU = "sso-rules-row-menu"
        const val ID_SAVE = "sso-provider-save"
    }
}
