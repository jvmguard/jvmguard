package dev.jvmguard.ui.components.recording

import dev.jvmguard.agent.config.transactions.ClassFilterTransactionDef
import dev.jvmguard.ui.components.JvmGuardDialog
import dev.jvmguard.ui.server.t
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

    private val discard = Checkbox(t("recording.discard"))
    private val className = TextField(t("recording.transaction.classFilter")).apply {
        setWidthFull()
        helperText = t("recording.transaction.classFilter.helper")
    }

    protected fun build() {
        headerTitle = t("recording.transaction.dialog." + (if (isNew) "add" else "edit") + "." + typeKey)
        width = "60rem"

        naming = namingForm()
        wizard.addTab(t("recording.tab.definition"), definitionTab())
        wizard.addTab(t("recording.tab.filter"), VerticalLayout(className, discard).apply { isPadding = false; isSpacing = true })
        naming?.let { wizard.addTab(t("recording.tab.naming"), it) }
        wizard.addTab(t("recording.tab.policies"), policyForm)
        add(wizard)

        bindShared()
        bindDefinition(binder)
        binder.readBean(def)
        readDefinition(def)
        policyForm.read(def.policy)
        naming?.read(def.naming)

        confirmFooter(t("common.save"), ID_SAVE) { save() }
    }

    protected abstract val typeKey: String
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
