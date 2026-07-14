package dev.jvmguard.ui.views.settings

import dev.jvmguard.data.config.LdapUserMapping
import dev.jvmguard.data.user.AccessLevel
import dev.jvmguard.ui.components.EnumSelect
import dev.jvmguard.ui.components.JvmGuardDialog
import com.vaadin.flow.component.formlayout.FormLayout
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.Binder

class LdapMappingDialog(
    private val mapping: LdapUserMapping,
    isNew: Boolean,
    private val onSave: (LdapUserMapping) -> Unit,
) : JvmGuardDialog() {

    private val binder = Binder(LdapUserMapping::class.java)

    private val searchBase = TextField("Search base").apply {
        setWidthFull()
        testId = ID_SEARCH_BASE
    }
    private val userFilter = TextField("User filter").apply {
        setWidthFull()
        testId = ID_USER_FILTER
    }
    private val accessLevel = EnumSelect("Access level", AccessLevel::class.java) { it.toString() }.apply {
        testId = ID_ACCESS_LEVEL
    }

    init {
        headerTitle = if (isNew) "Add user mapping" else "Edit user mapping"
        width = "32rem"

        bind()
        binder.readBean(mapping)

        val hint = Span("The user filter must contain ${LdapUserMapping.TOKEN_USER}, which is replaced with the login name.")
            .apply { addClassName("jvmguard-field-hint") }
        add(VerticalLayout(FormLayout(searchBase, userFilter, accessLevel).apply {
            setResponsiveSteps(FormLayout.ResponsiveStep("0", 1))
        }, hint).apply {
            isPadding = false
            isSpacing = true
        })

        confirmFooter("Save", ID_SAVE) { save() }
    }

    private fun bind() {
        binder.forField(searchBase)
            .asRequired("Enter a search base.")
            .bind({ it.searchBase }, { m, value -> m.searchBase = value })
        binder.forField(userFilter)
            .asRequired("Enter a user filter.")
            .withValidator({ it.contains(LdapUserMapping.TOKEN_USER) }, "The filter must contain ${LdapUserMapping.TOKEN_USER}.")
            .bind({ it.userFilter }, { m, value -> m.userFilter = value })
        binder.forField(accessLevel)
            .bind({ it.accessLevel }, { m, value -> m.accessLevel = value })
    }

    private fun save() {
        if (!binder.writeBeanIfValid(mapping)) {
            return
        }
        onSave(mapping)
        close()
    }

    companion object {
        const val ID_SEARCH_BASE = "ldap-mapping-search-base"
        const val ID_USER_FILTER = "ldap-mapping-user-filter"
        const val ID_ACCESS_LEVEL = "ldap-mapping-access-level"
        const val ID_SAVE = "ldap-mapping-save"
    }
}
