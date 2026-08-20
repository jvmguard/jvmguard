package dev.jvmguard.ui.components.recording

import dev.jvmguard.agent.config.transactions.MatchedTransactionDef
import dev.jvmguard.agent.config.transactions.MatchedTransactionDef.InterceptionTarget
import dev.jvmguard.agent.config.transactions.MatchedTransactionDef.MethodInterceptionMode
import dev.jvmguard.ui.components.EnumSelect
import dev.jvmguard.ui.server.t
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

    override val typeKey: String get() = "matched"

    private val target = EnumSelect(t("recording.transaction.matched.intercept"), InterceptionTarget::class.java).apply {
        addValueChangeListener { updateConditional() }
    }
    private val declaringClassName = TextField(t("recording.transaction.matched.className")).apply { setWidthFull() }
    private val methodInterceptionMode = EnumSelect(t("recording.transaction.matched.methods"), MethodInterceptionMode::class.java).apply {
        setWidthFull()
    }
    private val interceptSubclasses = Checkbox(t("recording.transaction.matched.interceptSubclasses"))
    private val methodName = TextField(t("recording.transaction.matched.methodName")).apply { setWidthFull() }
    private val methodSignature = TextField(t("recording.transaction.matched.methodSignature")).apply {
        setWidthFull()
        helperText = t("recording.transaction.matched.methodSignature.helper")
    }
    private val staticMethods = Checkbox(t("recording.transaction.matched.staticMethods"))

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
            .asRequired(t("recording.transaction.matched.className.required"))
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
