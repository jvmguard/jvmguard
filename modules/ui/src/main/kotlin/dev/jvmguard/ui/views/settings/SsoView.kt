package dev.jvmguard.ui.views.settings

import dev.jvmguard.data.config.GlobalConfig
import dev.jvmguard.data.config.SsoProviderConfig
import dev.jvmguard.data.user.Roles
import dev.jvmguard.data.user.UserType
import dev.jvmguard.ui.components.confirm
import dev.jvmguard.ui.components.editDeleteKeys
import dev.jvmguard.ui.components.menuButton
import dev.jvmguard.ui.server.Sessions
import dev.jvmguard.ui.server.StagedListController
import dev.jvmguard.ui.server.enumLabel
import dev.jvmguard.ui.server.t
import dev.jvmguard.ui.shell.MainLayout
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
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed

@RolesAllowed(Roles.ADMIN)
@Route(value = "settings/sso", layout = MainLayout::class)
class SsoView : AbstractSettingsSectionView() {

    private val providerGrid = Grid(SsoProviderConfig::class.java, false).apply {
        testId = ID_PROVIDER_GRID
        addColumn { it.displayName.ifEmpty { t("settings.sso.unnamed") } }.setHeader(t("settings.sso.provider.displayName")).setFlexGrow(1)
        addColumn { enumLabel(it.preset) }.setHeader(t("settings.sso.type")).setAutoWidth(true)
        addColumn { t(if (it.enabled) "common.enabled" else "common.disabled") }.setHeader(t("settings.sso.status")).setAutoWidth(true)
        addComponentColumn { rowActions(it) }.setFlexGrow(0).setAutoWidth(true)
        addItemDoubleClickListener { edit(it.item, false) }
        editDeleteKeys({ edit(it, false) }, ::confirmDelete)
        isAllRowsVisible = true
    }

    init {
        val hint = Span(t("settings.sso.hint")).apply { addClassName("jvmguard-field-hint") }
        add(settingsSection(t("nav.settings.sso"), hint))

        val addProvider = Button(t("settings.sso.addProvider"), VaadinIcon.PLUS.create()) { edit(SsoProviderConfig(), true) }.apply {
            addThemeVariants(ButtonVariant.PRIMARY)
            testId = ID_ADD_PROVIDER
        }
        val providerTitle = H4(t("settings.sso.providers"))
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
        menuButton(VaadinIcon.ELLIPSIS_DOTS_V, t("settings.sso.provider.actions"), "$ID_PROVIDER_ROW_MENU-${provider.displayName}") {
            addItem(t("common.edit")) { edit(provider, false) }
            addItem(t("common.delete")) { confirmDelete(provider) }
        }

    private fun edit(provider: SsoProviderConfig, isNew: Boolean) {
        val existingNames = (Sessions.settingsDraft().ssoProviders.items() + Sessions.settingsDraft().config.ssoConfig.providers)
            .map { it.displayName }
            .toMutableSet()
            .apply { if (!isNew) remove(provider.displayName) }
        SsoProviderDialog(provider, isNew, existingNames) { saved ->
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
            t("settings.sso.provider.deleteText.bound", provider.displayName, boundCount)
        } else {
            t("settings.sso.provider.deleteText", provider.displayName)
        }
        confirm(t("settings.sso.provider.deleteTitle"), message, t("common.delete")) {
            providers.remove(provider)
        }
    }

    companion object {
        const val ID_PROVIDER_GRID = "sso-provider-grid"
        const val ID_ADD_PROVIDER = "sso-add-provider"
        const val ID_PROVIDER_ROW_MENU = "sso-provider-row-menu"
    }
}
