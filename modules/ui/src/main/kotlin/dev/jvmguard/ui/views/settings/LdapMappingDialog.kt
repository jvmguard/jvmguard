package dev.jvmguard.ui.views.settings

import dev.jvmguard.data.config.LdapUserMapping
import dev.jvmguard.data.user.AccessLevel
import dev.jvmguard.ui.components.EnumSelect
import dev.jvmguard.ui.components.JvmGuardDialog
import dev.jvmguard.ui.server.t
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

    private val searchBase = TextField(t("settings.ldap.mapping.searchBase")).apply {
        setWidthFull()
        testId = ID_SEARCH_BASE
    }
    private val userFilter = TextField(t("settings.ldap.mapping.userFilter")).apply {
        setWidthFull()
        testId = ID_USER_FILTER
    }
    private val accessLevel = EnumSelect(t("shell.userInfo.accessLevel"), AccessLevel::class.java).apply {
        testId = ID_ACCESS_LEVEL
    }

    init {
        headerTitle = t(if (isNew) "settings.ldap.mapping.add" else "settings.ldap.mapping.edit")
        width = "32rem"

        bind()
        binder.readBean(mapping)

        val hint = Span(t("settings.ldap.mapping.hint", LdapUserMapping.TOKEN_USER))
            .apply { addClassName("jvmguard-field-hint") }
        add(VerticalLayout(FormLayout(searchBase, userFilter, accessLevel).apply {
            setResponsiveSteps(FormLayout.ResponsiveStep("0", 1))
        }, hint).apply {
            isPadding = false
            isSpacing = true
        })

        confirmFooter(t("common.save"), ID_SAVE) { save() }
    }

    private fun bind() {
        binder.forField(searchBase)
            .asRequired(t("settings.ldap.mapping.validation.searchBase"))
            .bind({ it.searchBase }, { m, value -> m.searchBase = value })
        binder.forField(userFilter)
            .asRequired(t("settings.ldap.mapping.validation.userFilter"))
            .withValidator({ it.contains(LdapUserMapping.TOKEN_USER) }, t("settings.ldap.mapping.validation.token", LdapUserMapping.TOKEN_USER))
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
