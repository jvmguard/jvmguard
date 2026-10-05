package dev.jvmguard.ui.components.recording

import dev.jvmguard.agent.config.transactions.AnnotationValueFilter
import dev.jvmguard.agent.config.transactions.MappedTransactionDef
import dev.jvmguard.agent.config.transactions.MappedTransactionDef.AnnotatedTarget
import dev.jvmguard.agent.config.transactions.MappedTransactionDef.MethodInterceptionMode
import dev.jvmguard.ui.components.EnumSelect
import dev.jvmguard.ui.server.t
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
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

    private val filterEnabled = Checkbox(t("recording.transaction.annotationFilter.enabled")).apply {
        addValueChangeListener { event -> if (event.isFromClient) updateFilterEnabled() }
    }
    private val filterAttribute = TextField(t("recording.transaction.annotationFilter.attribute")).apply {
        addValueChangeListener { isInvalid = false }
    }
    private val filterValue = TextField().apply {
        helperText = t("recording.transaction.classFilter.helper")
        addValueChangeListener { isInvalid = false }
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

    override fun filterTabExtras(): List<Component> = listOf(
        filterEnabled,
        HorizontalLayout(filterAttribute, Span("="), filterValue).apply {
            defaultVerticalComponentAlignment = FlexComponent.Alignment.BASELINE
            isPadding = false
            setWidthFull()
            setFlexGrow(1.0, filterAttribute, filterValue)
        }
    )

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
        filterEnabled.value = def.annotationValueFilter != null
        filterAttribute.value = def.annotationValueFilter?.attributeName ?: ""
        filterValue.value = def.annotationValueFilter?.value ?: ""
        updateFilterEnabled()
        checkEnabled()
    }

    override fun writeDefinition(def: MappedTransactionDef): Boolean {
        if (!filterEnabled.value) {
            def.annotationValueFilter = null
            return true
        }
        val attribute = filterAttribute.value.trim()
        val value = filterValue.value.trim()
        if (attribute.isEmpty()) {
            filterAttribute.errorMessage = t("recording.transaction.annotationFilter.attribute.required")
            filterAttribute.isInvalid = true
            return false
        }
        if (value.isEmpty()) {
            filterValue.errorMessage = t("recording.transaction.annotationFilter.value.required")
            filterValue.isInvalid = true
            return false
        }
        val filter = def.annotationValueFilter ?: AnnotationValueFilter()
        filter.attributeName = attribute
        filter.value = value
        def.annotationValueFilter = filter
        return true
    }

    override fun namingForm(): NamingForm = AnnotationNamingForm()

    private fun checkEnabled() {
        useDeclaringClassName.isEnabled = interceptSubclasses.value
        methodInterceptionMode.isEnabled = interceptSubclasses.value && annotatedTarget.value == AnnotatedTarget.CLASS
    }

    private fun updateFilterEnabled() {
        filterAttribute.isEnabled = filterEnabled.value
        filterValue.isEnabled = filterEnabled.value
    }
}
