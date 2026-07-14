package dev.jvmguard.ui.components.recording

import dev.jvmguard.agent.config.transactions.DeclaredTransactionDef
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextField

class DeclaredTransactionDefDialog(
    def: DeclaredTransactionDef,
    isNew: Boolean,
    onSave: (DeclaredTransactionDef) -> Unit,
) : AbstractTransactionDefDialog<DeclaredTransactionDef>(def, isNew, onSave) {

    override val typeName: String get() = "Declared"

    private val group = TextField("Restrict to group name (optional)").apply {
        setWidthFull()
        helperText = "Only Declared transactions with this group are recorded; leave empty for all."
    }

    init {
        build()
    }

    override fun definitionTab(): Component = VerticalLayout(group).apply {
        isPadding = false
        isSpacing = true
    }

    override fun readDefinition(def: DeclaredTransactionDef) {
        group.value = def.group.usedValue
    }

    override fun writeDefinition(def: DeclaredTransactionDef): Boolean {
        def.group.value = group.value
        def.group.isChecked = group.value.isNotBlank()
        return true
    }
}
