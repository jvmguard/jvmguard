package dev.jvmguard.ui.components.recording

import dev.jvmguard.agent.config.transactions.ComparisonType
import dev.jvmguard.agent.config.transactions.PolicySubDef
import dev.jvmguard.ui.components.EnumSelect
import dev.jvmguard.ui.components.JvmGuardDialog
import dev.jvmguard.ui.server.t
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.Binder

class PolicySubDefDialog(
    private val subDef: PolicySubDef,
    isNew: Boolean,
    private val onSave: (PolicySubDef) -> Unit,
) : JvmGuardDialog() {

    private val binder = Binder(PolicySubDef::class.java)
    private val filter = TextField(t("recording.subdef.filter")).apply {
        setWidthFull()
        helperText = t("recording.subdef.filter.helper")
    }
    private val comparisonType = EnumSelect(t("recording.comparison"), ComparisonType::class.java)
    private val wildcardCommaSeparated = Checkbox(t("recording.subdef.commaSeparated"))
    private val discard = Checkbox(t("recording.discard"))
    private val policyForm = PolicyForm()
    private val wizard = WizardTabs()

    init {
        headerTitle = t(if (isNew) "recording.subdef.dialog.add" else "recording.subdef.dialog.edit")
        width = "60rem"

        comparisonType.addValueChangeListener { updateWildcard() }

        wizard.addTab(t("recording.tab.filter"), VerticalLayout(filter, comparisonType, wildcardCommaSeparated, discard).apply {
            isPadding = false
            isSpacing = true
        })
        wizard.addTab(t("recording.tab.policies"), policyForm)
        add(wizard)

        bind()
        binder.readBean(subDef)
        policyForm.read(subDef.policy)
        updateWildcard()

        confirmFooter(t("common.save"), ID_SAVE) { save() }
    }

    @Suppress("DuplicatedCode")
    private fun bind() {
        binder.forField(filter)
            .asRequired(t("recording.subdef.filter.required"))
            .bind({ it.filter }, { s, v -> s.filter = v })
        binder.forField(comparisonType).bind({ it.comparisonType }, { s, v -> s.comparisonType = v })
        binder.forField(wildcardCommaSeparated).bind({ it.isWildcardCommaSeparated }, { s, v -> s.isWildcardCommaSeparated = v })
        binder.forField(discard).bind({ it.isDiscard }, { s, v -> s.isDiscard = v })
    }

    private fun save() {
        if (!binder.writeBeanIfValid(subDef) || !policyForm.writeIfValid(subDef.policy)) {
            return
        }
        onSave(subDef)
        close()
    }

    private fun updateWildcard() {
        wildcardCommaSeparated.isEnabled = comparisonType.value == ComparisonType.WILDCARD
    }

    companion object {
        const val ID_SAVE = "policy-subdef-save"
    }
}
