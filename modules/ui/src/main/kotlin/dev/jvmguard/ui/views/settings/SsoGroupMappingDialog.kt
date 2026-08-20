package dev.jvmguard.ui.views.settings

import dev.jvmguard.data.config.SsoGroupMapping
import dev.jvmguard.data.user.AccessLevel
import dev.jvmguard.ui.components.EnumSelect
import dev.jvmguard.ui.components.JvmGuardDialog
import dev.jvmguard.ui.server.t
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.formlayout.FormLayout
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.Binder

class SsoGroupMappingDialog(
    private val mapping: SsoGroupMapping,
    isNew: Boolean,
    private val groupsSupported: Boolean = true,
    private val catchAllExists: Boolean = false,
    private val onSave: (SsoGroupMapping) -> Unit,
) : JvmGuardDialog() {

    private val binder = Binder(SsoGroupMapping::class.java)

    private val claimValue = TextField(t("settings.sso.rule.claimValue")).apply {
        setWidthFull()
        testId = ID_CLAIM_VALUE
    }
    private val catchAll = Checkbox(t("settings.sso.rule.catchAll")).apply {
        testId = ID_CATCH_ALL
    }
    private val accessLevel = EnumSelect(t("shell.userInfo.accessLevel"), AccessLevel::class.java).apply {
        testId = ID_ACCESS_LEVEL
    }

    init {
        headerTitle = t(if (isNew) "settings.sso.rule.add" else "settings.sso.rule.edit")
        width = "32rem"

        bind()
        binder.readBean(mapping)

        if (!groupsSupported) {
            catchAll.value = true
            catchAll.isEnabled = false
            claimValue.isVisible = false
            mapping.claimValue = SsoGroupMapping.CATCH_ALL
        } else {
            updateCatchAllVisibility()
            catchAll.addValueChangeListener { updateCatchAllVisibility() }
        }

        val content = VerticalLayout(FormLayout(claimValue, catchAll, accessLevel).apply {
            setResponsiveSteps(FormLayout.ResponsiveStep("0", 1))
        })
        if (!groupsSupported) {
            content.add(Span(t("settings.sso.rule.noGroupsHint")).apply {
                addClassName("jvmguard-field-hint")
            })
        } else {
            content.add(Span(t("settings.sso.rule.hint")).apply {
                addClassName("jvmguard-field-hint")
            })
        }
        content.isPadding = false
        content.isSpacing = true
        add(content)

        confirmFooter(t("common.save"), ID_SAVE) { save() }
    }

    private fun bind() {
        binder.forField(catchAll)
            .bind(
                { it.isCatchAll },
                { m, value -> m.claimValue = if (value) SsoGroupMapping.CATCH_ALL else if (m.isCatchAll) "" else m.claimValue },
            )
        binder.forField(claimValue)
            .bind(
                { it.claimValue },
                { m, value -> if (!m.isCatchAll) m.claimValue = value },
            )
        binder.forField(accessLevel)
            .bind({ it.accessLevel }, { m, value -> m.accessLevel = value })
    }

    private fun updateCatchAllVisibility() {
        claimValue.isEnabled = !catchAll.value
        if (catchAll.value) {
            claimValue.value = SsoGroupMapping.CATCH_ALL
        }
    }

    private fun save() {
        claimValue.isInvalid = false
        if (!binder.writeBeanIfValid(mapping)) {
            return
        }
        if (groupsSupported && !catchAll.value && mapping.claimValue.isBlank()) {
            claimValue.isInvalid = true
            claimValue.errorMessage = t("settings.sso.rule.validation.claimValue")
            return
        }
        if (mapping.isCatchAll && catchAllExists) {
            claimValue.isInvalid = true
            claimValue.errorMessage = t("settings.sso.rule.validation.catchAllExists")
            return
        }
        onSave(mapping)
        close()
    }

    companion object {
        const val ID_CLAIM_VALUE = "sso-rule-claim-value"
        const val ID_CATCH_ALL = "sso-rule-catch-all"
        const val ID_ACCESS_LEVEL = "sso-rule-access-level"
        const val ID_SAVE = "sso-rule-save"
    }
}
