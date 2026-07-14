package dev.jvmguard.ui.components.recording

import dev.jvmguard.agent.config.transactions.DurationType
import dev.jvmguard.agent.config.transactions.Policy
import dev.jvmguard.ui.components.EnumSelect
import com.vaadin.flow.component.HasEnabled
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.combobox.MultiSelectComboBox
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.IntegerField
import com.vaadin.flow.data.binder.Binder

class PolicyForm : VerticalLayout() {

    private val binder = Binder(Policy::class.java)

    private val active = Checkbox("Define policies").apply { testId = "policy-active" }

    private val slowValue = IntegerField()
    private val slowType = EnumSelect("", DurationType::class.java) { it.toString() }
    private val verySlowValue = IntegerField()
    private val verySlowType = EnumSelect("", DurationType::class.java) { it.toString() }
    private val overdueValue = IntegerField()
    private val overdueType = EnumSelect("", DurationType::class.java) { it.toString() }

    private val splitTree = Checkbox("Split the transaction tree for policy violations").apply {
        addClassName("jvmguard-settings-gap-before")
    }

    private val treatAsError = MultiSelectComboBox<ErrorSource>("Treat as error").apply {
        setItems(ErrorSource.entries)
        setItemLabelGenerator { it.label }
        setWidthFull()
        testId = "policy-errors"
    }

    private val gated: List<HasEnabled> = listOf(
        slowValue, slowType, verySlowValue, verySlowType, overdueValue, overdueType, splitTree, treatAsError,
    )

    init {
        isPadding = false
        isSpacing = true
        setWidthFull()

        active.addValueChangeListener { updateEnabled() }
        bind()

        add(active)
        add(thresholdRow("Slow", slowValue, slowType))
        add(thresholdRow("Very slow", verySlowValue, verySlowType))
        add(thresholdRow("Overdue", overdueValue, overdueType))
        add(splitTree)
        add(treatAsError)
    }

    fun read(policy: Policy) {
        binder.readBean(policy)
        treatAsError.value = errorSet(policy)
        updateEnabled()
    }

    fun writeIfValid(policy: Policy): Boolean {
        if (!binder.writeBeanIfValid(policy)) {
            return false
        }
        applyErrorSet(policy, treatAsError.value)
        return true
    }

    private fun updateEnabled() {
        gated.forEach { it.isEnabled = active.value }
    }

    private fun thresholdRow(label: String, value: IntegerField, type: EnumSelect<DurationType>): HorizontalLayout {
        value.label = label
        value.width = "8rem"
        return HorizontalLayout(value, type).apply {
            defaultVerticalComponentAlignment = FlexComponent.Alignment.END
            isPadding = false
        }
    }

    private fun errorSet(policy: Policy): Set<ErrorSource> = buildSet {
        if (policy.isErrorThrowableAsError) {
            add(ErrorSource.ERROR_THROWABLE)
        }
        if (policy.isRuntimeExceptionAsError) {
            add(ErrorSource.RUNTIME_EXCEPTION)
        }
        if (policy.isCheckedExceptionAsError) {
            add(ErrorSource.CHECKED_EXCEPTION)
        }
        if (policy.isLoggedErrorAsError) {
            add(ErrorSource.LOGGED_ERROR)
        }
        if (policy.isLoggedWarningAsError) {
            add(ErrorSource.LOGGED_WARNING)
        }
    }

    private fun applyErrorSet(policy: Policy, selected: Set<ErrorSource>) {
        policy.isErrorThrowableAsError = ErrorSource.ERROR_THROWABLE in selected
        policy.isRuntimeExceptionAsError = ErrorSource.RUNTIME_EXCEPTION in selected
        policy.isCheckedExceptionAsError = ErrorSource.CHECKED_EXCEPTION in selected
        policy.isLoggedErrorAsError = ErrorSource.LOGGED_ERROR in selected
        policy.isLoggedWarningAsError = ErrorSource.LOGGED_WARNING in selected
    }

    @Suppress("DuplicatedCode")
    private fun bind() {
        binder.forField(active).bind({ it.isActive }, { p, v -> p.isActive = v })
        binder.forField(slowValue).bind({ it.slowValue }, { p, v -> p.slowValue = v ?: 0 })
        binder.forField(slowType).bind({ it.slowDurationType }, { p, v -> p.slowDurationType = v })
        binder.forField(verySlowValue).bind({ it.verySlowValue }, { p, v -> p.verySlowValue = v ?: 0 })
        binder.forField(verySlowType).bind({ it.verySlowDurationType }, { p, v -> p.verySlowDurationType = v })
        binder.forField(overdueValue).bind({ it.overdueValue }, { p, v -> p.overdueValue = v ?: 0 })
        binder.forField(overdueType).bind({ it.overdueDurationType }, { p, v -> p.overdueDurationType = v })
        binder.forField(splitTree).bind({ it.isSplitTree }, { p, v -> p.isSplitTree = v })
    }

    enum class ErrorSource(val label: String) {
        ERROR_THROWABLE("Error throwables"),
        RUNTIME_EXCEPTION("Runtime exceptions"),
        CHECKED_EXCEPTION("Checked exceptions"),
        LOGGED_ERROR("Logged errors"),
        LOGGED_WARNING("Logged warnings"),
    }
}
