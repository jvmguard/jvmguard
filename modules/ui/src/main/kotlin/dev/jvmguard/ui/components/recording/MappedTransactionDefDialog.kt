package dev.jvmguard.ui.components.recording

import dev.jvmguard.agent.config.transactions.MappedTransactionDef
import dev.jvmguard.agent.config.transactions.MappedTransactionDef.AnnotatedTarget
import dev.jvmguard.agent.config.transactions.MappedTransactionDef.MethodInterceptionMode
import dev.jvmguard.ui.components.EnumSelect
import dev.jvmguard.ui.server.t
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.Binder

class MappedTransactionDefDialog(
    def: MappedTransactionDef,
    isNew: Boolean,
    onSave: (MappedTransactionDef) -> Unit,
) : AbstractTransactionDefDialog<MappedTransactionDef>(def, isNew, onSave) {

    override val typeKey: String get() = "mapped"

    private val annotationName = TextField(t("recording.transaction.mapped.annotationName")).apply {
        setWidthFull()
        helperText = t("recording.transaction.mapped.annotationName.helper")
    }
    private val annotatedTarget = EnumSelect(t("recording.transaction.mapped.annotationTarget"), AnnotatedTarget::class.java).apply {
        addValueChangeListener { checkEnabled() }
    }
    private val interceptSubclasses = Checkbox(t("recording.transaction.mapped.interceptSubclasses")).apply {
        addValueChangeListener { checkEnabled() }
    }
    private val useDeclaringClassName = Checkbox(t("recording.transaction.mapped.useDeclaringClassName"))
    private val methodInterceptionMode = EnumSelect(t("recording.transaction.mapped.methodSelection"), MethodInterceptionMode::class.java).apply {
        setWidthFull()
        helperText = t("recording.transaction.mapped.methodSelection.helper")
    }

    init {
        build()
    }

    override fun definitionTab(): Component = VerticalLayout(
        annotationName, annotatedTarget, interceptSubclasses, useDeclaringClassName, methodInterceptionMode,
    ).apply {
        isPadding = false
        isSpacing = true
    }

    @Suppress("DuplicatedCode")
    override fun bindDefinition(binder: Binder<MappedTransactionDef>) {
        binder.forField(annotationName)
            .asRequired(t("recording.transaction.mapped.annotationName.required"))
            .bind({ it.annotationName }, { d, v -> d.annotationName = v })
        binder.forField(annotatedTarget).bind({ it.annotatedTarget }, { d, v -> d.annotatedTarget = v })
        binder.forField(interceptSubclasses).bind({ it.isInterceptSubclasses }, { d, v -> d.isInterceptSubclasses = v })
        binder.forField(useDeclaringClassName).bind({ it.isUseDeclaringClassName }, { d, v -> d.isUseDeclaringClassName = v })
        binder.forField(methodInterceptionMode).bind({ it.methodInterceptionMode }, { d, v -> d.methodInterceptionMode = v })
    }

    override fun readDefinition(def: MappedTransactionDef) {
        checkEnabled()
    }

    override fun namingForm(): NamingForm = NamingForm()

    private fun checkEnabled() {
        useDeclaringClassName.isEnabled = interceptSubclasses.value
        methodInterceptionMode.isEnabled = interceptSubclasses.value && annotatedTarget.value == AnnotatedTarget.CLASS
    }
}
