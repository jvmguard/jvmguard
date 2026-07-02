package com.jvmguard.ui.components.recording

import com.jvmguard.agent.config.transactions.DevOpsAnnotatedTransactionDef
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextField

class DevOpsTransactionDefDialog(
    def: DevOpsAnnotatedTransactionDef,
    isNew: Boolean,
    onSave: (DevOpsAnnotatedTransactionDef) -> Unit,
) : AbstractTransactionDefDialog<DevOpsAnnotatedTransactionDef>(def, isNew, onSave) {

    override val typeName: String get() = "DevOps"

    private val group = TextField("Restrict to group name (optional)").apply {
        setWidthFull()
        helperText = "Only @com.jvmguard.annotations transactions with this group are recorded; leave empty for all."
    }

    init {
        build()
    }

    override fun definitionTab(): Component = VerticalLayout(group).apply {
        isPadding = false
        isSpacing = true
    }

    override fun readDefinition(def: DevOpsAnnotatedTransactionDef) {
        group.value = def.group.usedValue
    }

    override fun writeDefinition(def: DevOpsAnnotatedTransactionDef): Boolean {
        def.group.value = group.value
        def.group.isChecked = group.value.isNotBlank()
        return true
    }
}
