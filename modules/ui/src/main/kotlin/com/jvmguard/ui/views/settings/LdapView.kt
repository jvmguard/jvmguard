package com.jvmguard.ui.views.settings

import com.jvmguard.data.config.GlobalConfig
import com.jvmguard.data.config.LdapUserMapping
import com.jvmguard.data.user.Roles
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
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.html.H4
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.textfield.PasswordField
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed

@RolesAllowed(Roles.ADMIN)
@Route(value = "settings/ldap", layout = MainLayout::class)
@PageTitle("jvmguard: Settings")
class LdapView : AbstractSettingsSectionView() {

    private val url = TextField("LDAP URL").apply {
        setWidthFull()
        placeholder = "ldap://host:389"
        testId = ID_URL
    }
    private val useStartTls = Checkbox("Use StartTLS")
    private val authenticate = Checkbox("Authenticate to the directory").apply {
        addClassName("jvmguard-settings-gap-before")
        addValueChangeListener { updateAuthEnabled() }
        testId = ID_AUTHENTICATE
    }
    private val userName = TextField("Bind user name").apply {
        setWidthFull()
        testId = ID_USER_NAME
    }
    private val password = PasswordField("Bind password").apply {
        setWidthFull()
        testId = ID_PASSWORD
    }

    private val mappingGrid = Grid(LdapUserMapping::class.java, false).apply {
        testId = ID_MAPPING_GRID
        addColumn { it.searchBase }.setHeader("Search base").setAutoWidth(true)
        addColumn { it.userFilter }.setHeader("User filter").setFlexGrow(1)
        addColumn { it.accessLevel.toString() }.setHeader("Access level").setAutoWidth(true)
        addComponentColumn { rowActions(it) }.setFlexGrow(0).setAutoWidth(true)
        addItemDoubleClickListener { edit(it.item, false) }
        editDeleteKeys({ edit(it, false) }, ::confirmDelete)
        isAllRowsVisible = true
    }

    init {
        add(settingsSection("LDAP", url, useStartTls, authenticate, userName, password))

        val addMapping = Button("Add mapping", VaadinIcon.PLUS.create()) { edit(LdapUserMapping(), true) }.apply {
            addThemeVariants(ButtonVariant.PRIMARY)
            testId = ID_ADD_MAPPING
        }
        val mappingTitle = H4("User mappings")
        val mappingHeader = HorizontalLayout(mappingTitle, addMapping).apply {
            addClassName("jvmguard-settings-gap-before")
            defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
            setWidthFull()
            isPadding = false
            expand(mappingTitle)
        }
        mappingGrid.addClassName("jvmguard-settings-gap-before")
        add(mappingHeader, mappingGrid)
    }

    @Suppress("DuplicatedCode")
    override fun bind(binder: Binder<GlobalConfig>) {
        binder.forField(url)
            .withValidator({ it.isNullOrEmpty() || it.startsWith("ldap://") || it.startsWith("ldaps://") }, "Use an ldap:// or ldaps:// URL.")
            .bind({ it.ldapConfig.url }, { config, value -> config.ldapConfig.url = value })
        binder.forField(useStartTls)
            .bind({ it.ldapConfig.useStartTls }, { config, value -> config.ldapConfig.useStartTls = value })
        binder.forField(authenticate)
            .bind({ it.ldapConfig.isAuthenticate }, { config, value -> config.ldapConfig.isAuthenticate = value })
        binder.forField(userName)
            .bind({ it.ldapConfig.userName }, { config, value -> config.ldapConfig.userName = value })
        binder.forField(password)
            .bind({ it.ldapConfig.password }, { config, value -> config.ldapConfig.password = value })
    }

    private val mappings = StagedListController(
        edits = { Sessions.settingsDraft().ldapMappings },
        load = { Sessions.settingsDraft().config.ldapConfig.userMappings },
        markDirty = { Sessions.settingsDraft().markDirty() },
        render = { mappingGrid.setItems(it) },
    )

    override fun onAttach(attachEvent: AttachEvent) {
        super.onAttach(attachEvent)
        updateAuthEnabled()
        mappings.reload()
    }

    private fun rowActions(mapping: LdapUserMapping): Component =
        menuButton(VaadinIcon.ELLIPSIS_DOTS_V, "Actions for this mapping", "$ID_MAPPING_ROW_MENU-${mapping.searchBase}") {
            addItem("Edit") { edit(mapping, false) }
            addItem("Delete") { confirmDelete(mapping) }
        }

    private fun edit(mapping: LdapUserMapping, isNew: Boolean) {
        LdapMappingDialog(mapping, isNew) { saved ->
            if (isNew) {
                mappings.add(saved)
            } else {
                mappings.markModified(saved)
            }
        }.open()
    }

    private fun confirmDelete(mapping: LdapUserMapping) {
        confirm("Delete mapping", "Delete the mapping for \"${mapping.searchBase}\"?", "Delete") {
            mappings.remove(mapping)
        }
    }

    private fun updateAuthEnabled() {
        userName.isEnabled = authenticate.value
        password.isEnabled = authenticate.value
    }

    companion object {
        const val ID_URL = "settings-ldap-url"
        const val ID_AUTHENTICATE = "settings-ldap-authenticate"
        const val ID_USER_NAME = "settings-ldap-username"
        const val ID_PASSWORD = "settings-ldap-password"
        const val ID_MAPPING_GRID = "ldap-mapping-grid"
        const val ID_ADD_MAPPING = "ldap-add-mapping"
        const val ID_MAPPING_ROW_MENU = "ldap-mapping-row-menu"
    }
}
