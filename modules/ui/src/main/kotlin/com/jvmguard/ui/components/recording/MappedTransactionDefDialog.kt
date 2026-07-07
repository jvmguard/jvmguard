package com.jvmguard.ui.components.recording

import com.jvmguard.agent.config.transactions.MappedTransactionDef
import com.jvmguard.agent.config.transactions.MappedTransactionDef.AnnotatedTarget
import com.jvmguard.agent.config.transactions.MappedTransactionDef.MethodInterceptionMode
import com.jvmguard.ui.components.EnumSelect
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

    override val typeName: String get() = "Mapped"

    private val annotationName = TextField("Annotation class name").apply {
        setWidthFull()
        helperText = "Fully qualified name of the annotation, e.g. com.example.Traced."
    }
    private val annotatedTarget = EnumSelect("Annotation target", AnnotatedTarget::class.java) { it.toString() }.apply {
        addValueChangeListener { checkEnabled() }
    }
    private val interceptSubclasses = Checkbox("Intercept subclasses").apply {
        addValueChangeListener { checkEnabled() }
    }
    private val useDeclaringClassName = Checkbox("Use annotated class name for filter and naming")
    private val methodInterceptionMode = EnumSelect("Method selection", MethodInterceptionMode::class.java) { it.toString() }.apply {
        setWidthFull()
        helperText = "If you annotate marker interfaces or abstract base classes, select \"All public methods\"."
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
            .asRequired("Please enter a fully qualified annotation name.")
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
