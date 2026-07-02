package com.jvmguard.ui.components.recording

import com.jvmguard.agent.config.transactions.ReentryInhibition
import com.jvmguard.agent.config.transactions.TransactionNaming
import com.jvmguard.ui.components.EnumSelect
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.html.H5
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.Binder

class NamingForm : VerticalLayout() {

    private val binder = Binder(TransactionNaming::class.java)

    private val active = Checkbox("Assign a custom transaction name").apply { testId = "naming-active" }
    private val reentry = EnumSelect("", ReentryInhibition::class.java) { it.toString() }
    private val group = TextField("Group name").apply { width = "20rem" }
    private val elementsEditor = NamingElementsEditor()

    private val reentryRow = HorizontalLayout(Span("Suppress nested transactions"), reentry).apply {
        defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
        isPadding = false
    }

    init {
        isPadding = false
        isSpacing = true
        setSizeFull()

        active.addValueChangeListener { updateEnabled() }
        reentry.addValueChangeListener { updateGroupEnabled() }
        binder.forField(active).bind({ it.isActive }, { n, v -> n.isActive = v })
        binder.forField(reentry).bind({ it.reentryInhibition }, { n, v -> n.reentryInhibition = v })

        add(active, reentryRow, group, H5("Naming elements").apply { addClassName("jvmguard-form-subhead") }, elementsEditor)
        setFlexGrow(1.0, elementsEditor)
    }

    fun read(naming: TransactionNaming) {
        binder.readBean(naming)
        group.value = naming.group.usedValue
        elementsEditor.setElements(naming.namingElements)
        updateEnabled()
    }

    fun writeIfValid(naming: TransactionNaming): Boolean {
        if (!binder.writeBeanIfValid(naming)) {
            return false
        }
        naming.group.value = group.value
        naming.group.isChecked = naming.reentryInhibition == ReentryInhibition.GROUP && group.value.isNotBlank()
        return true
    }

    private fun updateEnabled() {
        reentryRow.isEnabled = active.value
        elementsEditor.isEnabled = active.value
        updateGroupEnabled()
    }

    private fun updateGroupEnabled() {
        group.isVisible = active.value && reentry.value == ReentryInhibition.GROUP
    }
}
