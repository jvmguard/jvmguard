package com.jvmguard.ui.components.recording.sets

import com.jvmguard.agent.config.base.Identifiable
import com.jvmguard.data.config.sets.AbstractSet
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant

class SetSpec<T : Identifiable, S : AbstractSet<T>>(
    val setClass: Class<S>,
    val singularName: String,
    val pluralName: String,
    val addSubtitle: String,
    val saveSubtitle: String,
    val loadSets: () -> Collection<S>,
    val currentItems: () -> MutableList<T>,
    val createSet: (String, List<T>) -> S,
    val appendItems: (List<T>) -> Unit,
)

fun <T : Identifiable, S : AbstractSet<T>> setActionButtons(spec: SetSpec<T, S>): List<Component> {
    val add = Button("Add set") { AddSetDialog(spec).open() }.apply {
        addThemeVariants(ButtonVariant.TERTIARY)
        testId = ID_ADD
        setTooltipText("Add a saved ${spec.singularName} to this group")
    }
    val save = Button("Save set") { SaveSetDialog(spec).open() }.apply {
        addThemeVariants(ButtonVariant.TERTIARY)
        testId = ID_SAVE
        isEnabled = spec.currentItems().isNotEmpty()
        setTooltipText("Save the current ${spec.pluralName} as a reusable ${spec.singularName}")
    }
    return listOf(add, save)
}

const val ID_ADD = "set-add"
const val ID_SAVE = "set-save"
