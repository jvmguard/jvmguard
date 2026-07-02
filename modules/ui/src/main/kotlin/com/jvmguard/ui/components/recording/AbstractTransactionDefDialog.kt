package com.jvmguard.ui.components.recording

import com.jvmguard.agent.config.transactions.ClassFilterTransactionDef
import com.jvmguard.ui.components.JvmGuardDialog
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.Binder

abstract class AbstractTransactionDefDialog<T : ClassFilterTransactionDef>(
    protected val def: T,
    private val isNew: Boolean,
    private val onSave: (T) -> Unit,
) : JvmGuardDialog() {

    protected val binder = Binder<T>()
    private val wizard = WizardTabs()
    private val policyForm = PolicyForm()
    private var naming: NamingForm? = null

    private val discard = Checkbox("Discard matching transactions (record nothing)")
    private val className = TextField("Class filter").apply {
        setWidthFull()
        helperText = "Wildcard (comma-separated) or regular expression; * matches all."
    }

    protected fun build() {
        headerTitle = (if (isNew) "Add " else "Edit ") + "$typeName transaction"
        width = "60rem"

        naming = namingForm()
        wizard.addTab("Definition", definitionTab())
        wizard.addTab("Filter", VerticalLayout(className, discard).apply { isPadding = false; isSpacing = true })
        naming?.let { wizard.addTab("Naming", it) }
        wizard.addTab("Policies", policyForm)
        add(wizard)

        bindShared()
        bindDefinition(binder)
        binder.readBean(def)
        readDefinition(def)
        policyForm.read(def.policy)
        naming?.read(def.naming)

        confirmFooter("Save", ID_SAVE) { save() }
    }

    protected abstract val typeName: String
    protected abstract fun definitionTab(): Component
    protected open fun bindDefinition(binder: Binder<T>) {}
    protected open fun readDefinition(def: T) {}
    protected open fun writeDefinition(def: T): Boolean = true
    protected open fun namingForm(): NamingForm? = null

    private fun bindShared() {
        binder.forField(discard).bind({ it.isDiscard }, { d, v -> d.isDiscard = v })
        binder.forField(className).bind({ it.className }, { d, v -> d.className = v })
    }

    private fun save() {
        if (!binder.writeBeanIfValid(def)) {
            return
        }
        if (!writeDefinition(def)) {
            return
        }
        if (!policyForm.writeIfValid(def.policy)) {
            return
        }
        val namingForm = naming
        if (namingForm != null && !namingForm.writeIfValid(def.naming)) {
            return
        }
        onSave(def)
        close()
    }

    companion object {
        const val ID_SAVE = "transaction-def-save"
    }
}
