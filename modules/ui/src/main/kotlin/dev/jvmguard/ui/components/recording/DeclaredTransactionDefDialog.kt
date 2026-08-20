package dev.jvmguard.ui.components.recording

import dev.jvmguard.agent.config.transactions.DeclaredTransactionDef
import dev.jvmguard.ui.server.t
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextField

class DeclaredTransactionDefDialog(
    def: DeclaredTransactionDef,
    isNew: Boolean,
    onSave: (DeclaredTransactionDef) -> Unit,
) : AbstractTransactionDefDialog<DeclaredTransactionDef>(def, isNew, onSave) {

    override val typeKey: String get() = "declared"

    private val group = TextField(t("recording.transaction.declared.group")).apply {
        setWidthFull()
        helperText = t("recording.transaction.declared.group.helper")
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
