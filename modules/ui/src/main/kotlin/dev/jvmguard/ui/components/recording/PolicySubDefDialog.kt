package dev.jvmguard.ui.components.recording

import dev.jvmguard.agent.config.transactions.ComparisonType
import dev.jvmguard.agent.config.transactions.PolicySubDef
import dev.jvmguard.ui.components.EnumSelect
import dev.jvmguard.ui.components.JvmGuardDialog
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
    private val filter = TextField("Match transaction names").apply {
        setWidthFull()
        helperText = "Name pattern that selects the transactions handled by this specialization."
    }
    private val comparisonType = EnumSelect("Comparison", ComparisonType::class.java) { it.toString() }
    private val wildcardCommaSeparated = Checkbox("Comma-separated patterns")
    private val discard = Checkbox("Discard matching transactions (record nothing)")
    private val policyForm = PolicyForm()
    private val wizard = WizardTabs()

    init {
        headerTitle = if (isNew) "Add policy specialization" else "Edit policy specialization"
        width = "60rem"

        comparisonType.addValueChangeListener { updateWildcard() }

        wizard.addTab("Filter", VerticalLayout(filter, comparisonType, wildcardCommaSeparated, discard).apply {
            isPadding = false
            isSpacing = true
        })
        wizard.addTab("Policies", policyForm)
        add(wizard)

        bind()
        binder.readBean(subDef)
        policyForm.read(subDef.policy)
        updateWildcard()

        confirmFooter("Save", ID_SAVE) { save() }
    }

    @Suppress("DuplicatedCode")
    private fun bind() {
        binder.forField(filter)
            .asRequired("Enter a name pattern.")
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
