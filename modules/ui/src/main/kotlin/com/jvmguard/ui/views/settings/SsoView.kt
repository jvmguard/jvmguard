package com.jvmguard.ui.views.settings

import com.jvmguard.data.config.GlobalConfig
import com.jvmguard.data.config.SsoProviderConfig
import com.jvmguard.data.user.Roles
import com.jvmguard.data.user.UserType
import com.jvmguard.ui.components.confirm
import com.jvmguard.ui.components.editDeleteKeys
import com.jvmguard.ui.components.menuButton
import com.jvmguard.ui.server.Sessions
import com.jvmguard.ui.server.StagedListController
import com.jvmguard.ui.shell.MainLayout
import com.vaadin.flow.component.AttachEvent
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.html.H4
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed

@RolesAllowed(Roles.ADMIN)
@Route(value = "settings/sso", layout = MainLayout::class)
@PageTitle("jvmguard: Settings")
class SsoView : AbstractSettingsSectionView() {

    private val providerGrid = Grid(SsoProviderConfig::class.java, false).apply {
        testId = ID_PROVIDER_GRID
        addColumn { it.displayName.ifEmpty { "(unnamed)" } }.setHeader("Display name").setFlexGrow(1)
        addColumn { it.preset.toString() }.setHeader("Type").setAutoWidth(true)
        addColumn { if (it.enabled) "Enabled" else "Disabled" }.setHeader("Status").setAutoWidth(true)
        addComponentColumn { rowActions(it) }.setFlexGrow(0).setAutoWidth(true)
        addItemDoubleClickListener { edit(it.item, false) }
        editDeleteKeys({ edit(it, false) }, ::confirmDelete)
        isAllRowsVisible = true
    }

    init {
        val hint = Span(
            "Configure external identity providers (Google, Microsoft Entra ID, Okta, Keycloak, ...). " +
                "Users sign in through the provider. Access rules control who gets in and at what role."
        ).apply { addClassName("jvmguard-field-hint") }
        add(settingsSection("Single Sign-On", hint))

        val addProvider = Button("Add provider", VaadinIcon.PLUS.create()) { edit(SsoProviderConfig(), true) }.apply {
            addThemeVariants(ButtonVariant.PRIMARY)
            testId = ID_ADD_PROVIDER
        }
        val providerTitle = H4("Providers")
        val providerHeader = HorizontalLayout(providerTitle, addProvider).apply {
            addClassName("jvmguard-settings-gap-before")
            defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
            setWidthFull()
            isPadding = false
            expand(providerTitle)
        }
        providerGrid.addClassName("jvmguard-settings-gap-before")
        add(providerHeader, providerGrid)
    }

    override fun bind(binder: Binder<GlobalConfig>) {
        // SSO has no top-level GlobalConfig fields; all provider config is managed via the staged list.
    }

    private val providers = StagedListController(
        edits = { Sessions.settingsDraft().ssoProviders },
        load = { Sessions.settingsDraft().config.ssoConfig.providers },
        markDirty = { Sessions.settingsDraft().markDirty() },
        render = { providerGrid.setItems(it) },
    )

    override fun onAttach(attachEvent: AttachEvent) {
        super.onAttach(attachEvent)
        providers.reload()
    }

    private fun rowActions(provider: SsoProviderConfig): Component =
        menuButton(VaadinIcon.ELLIPSIS_DOTS_V, "Actions for this provider", "$ID_PROVIDER_ROW_MENU-${provider.displayName}") {
            addItem("Edit") { edit(provider, false) }
            addItem("Delete") { confirmDelete(provider) }
        }

    private fun edit(provider: SsoProviderConfig, isNew: Boolean) {
        SsoProviderDialog(provider, isNew) { saved ->
            if (isNew) {
                providers.add(saved)
            } else {
                providers.markModified(saved)
            }
        }.open()
    }

    private fun confirmDelete(provider: SsoProviderConfig) {
        val boundCount = Sessions.current()?.serverConnection?.users
            ?.count { it.userType == UserType.OIDC && it.ssoIssuer.trim() == provider.issuerUri.trim() }
            ?: 0
        val message = if (boundCount > 0) {
            "Delete the provider \"${provider.displayName}\"? $boundCount user(s) are bound to this provider and will no longer be able to sign in."
        } else {
            "Delete the provider \"${provider.displayName}\"?"
        }
        confirm("Delete provider", message, "Delete") {
            providers.remove(provider)
        }
    }

    companion object {
        const val ID_PROVIDER_GRID = "sso-provider-grid"
        const val ID_ADD_PROVIDER = "sso-add-provider"
        const val ID_PROVIDER_ROW_MENU = "sso-provider-row-menu"
    }
}
