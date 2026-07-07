package com.jvmguard.ui.components.recording

import com.jvmguard.agent.config.transactions.MatchedTransactionDef
import com.jvmguard.agent.config.transactions.MatchedTransactionDef.InterceptionTarget
import com.jvmguard.agent.config.transactions.MatchedTransactionDef.MethodInterceptionMode
import com.jvmguard.ui.components.EnumSelect
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.Binder

class MatchedTransactionDefDialog(
    def: MatchedTransactionDef,
    isNew: Boolean,
    onSave: (MatchedTransactionDef) -> Unit,
) : AbstractTransactionDefDialog<MatchedTransactionDef>(def, isNew, onSave) {

    override val typeName: String get() = "Matched"

    private val target = EnumSelect("Intercept", InterceptionTarget::class.java) { it.toString() }.apply {
        addValueChangeListener { updateConditional() }
    }
    private val declaringClassName = TextField("Class or interface name").apply { setWidthFull() }
    private val methodInterceptionMode = EnumSelect("Methods", MethodInterceptionMode::class.java) { it.toString() }.apply {
        setWidthFull()
    }
    private val interceptSubclasses = Checkbox("Also intercept subclasses and implementations")
    private val methodName = TextField("Method name").apply { setWidthFull() }
    private val methodSignature = TextField("Method signature (optional)").apply {
        setWidthFull()
        helperText = "JVM signature, e.g. (Ljava/lang/String;)V"
    }
    private val staticMethods = Checkbox("Only static methods")

    init {
        build()
    }

    override fun definitionTab(): Component = VerticalLayout(
        target, declaringClassName, methodInterceptionMode, interceptSubclasses, methodName, methodSignature, staticMethods,
    ).apply {
        isPadding = false
        isSpacing = true
    }

    @Suppress("DuplicatedCode")
    override fun bindDefinition(binder: Binder<MatchedTransactionDef>) {
        binder.forField(target).bind({ it.interceptionTarget }, { d, v -> d.interceptionTarget = v })
        binder.forField(declaringClassName)
            .asRequired("Enter a class or interface name.")
            .bind({ it.declaringClassName }, { d, v -> d.declaringClassName = v })
        binder.forField(methodInterceptionMode).bind({ it.methodInterceptionMode }, { d, v -> d.methodInterceptionMode = v })
        binder.forField(interceptSubclasses).bind({ it.isInterceptSubclasses }, { d, v -> d.isInterceptSubclasses = v })
        binder.forField(methodName).bind({ it.methodName }, { d, v -> d.methodName = v })
        binder.forField(methodSignature).bind({ it.methodSignature }, { d, v -> d.methodSignature = v })
        binder.forField(staticMethods).bind({ it.isStaticMethods }, { d, v -> d.isStaticMethods = v })
    }

    override fun readDefinition(def: MatchedTransactionDef) {
        updateConditional()
    }

    override fun namingForm(): NamingForm = NamingForm()

    private fun updateConditional() {
        val byClass = target.value == InterceptionTarget.CLASS
        methodInterceptionMode.isVisible = byClass
        methodName.isVisible = !byClass
        methodSignature.isVisible = !byClass
        staticMethods.isVisible = !byClass
    }
}
