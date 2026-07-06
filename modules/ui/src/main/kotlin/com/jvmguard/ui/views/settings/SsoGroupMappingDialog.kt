package com.jvmguard.ui.views.settings

import com.jvmguard.data.config.SsoGroupMapping
import com.jvmguard.data.user.AccessLevel
import com.jvmguard.ui.components.EnumSelect
import com.jvmguard.ui.components.JvmGuardDialog
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.formlayout.FormLayout
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.Binder

class SsoGroupMappingDialog(
    private val mapping: SsoGroupMapping,
    isNew: Boolean,
    private val onSave: (SsoGroupMapping) -> Unit,
) : JvmGuardDialog() {

    private val binder = Binder(SsoGroupMapping::class.java)

    private val claimValue = TextField("Group claim value").apply {
        setWidthFull()
        testId = ID_CLAIM_VALUE
    }
    private val catchAll = Checkbox("Match everyone else in the domain (catch-all)").apply {
        testId = ID_CATCH_ALL
    }
    private val accessLevel = EnumSelect("Access level", AccessLevel::class.java) { it.toString() }.apply {
        testId = ID_ACCESS_LEVEL
    }

    init {
        headerTitle = if (isNew) "Add access rule" else "Edit access rule"
        width = "32rem"

        bind()
        binder.readBean(mapping)
        updateCatchAllVisibility()
        catchAll.addValueChangeListener { updateCatchAllVisibility() }

        val hint = Span("Matches a group/role claim value from the IdP token. Use the catch-all to allow anyone in the domain who doesn't match a specific rule.")
            .apply { addClassName("jvmguard-field-hint") }
        add(VerticalLayout(FormLayout(claimValue, catchAll, accessLevel).apply {
            setResponsiveSteps(FormLayout.ResponsiveStep("0", 1))
        }, hint).apply {
            isPadding = false
            isSpacing = true
        })

        confirmFooter("Save", ID_SAVE) { save() }
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
        if (!binder.writeBeanIfValid(mapping)) {
            return
        }
        if (!catchAll.value && mapping.claimValue.isBlank()) {
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
