package dev.jvmguard.ui.components.recording.sets

import dev.jvmguard.agent.config.base.Identifiable
import dev.jvmguard.common.helper.DeepCopy
import dev.jvmguard.common.helper.ListModification
import dev.jvmguard.data.config.sets.AbstractSet
import dev.jvmguard.ui.components.Notifications
import dev.jvmguard.ui.components.JvmGuardDialog
import dev.jvmguard.ui.server.Sessions
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextField

class SaveSetDialog<T : Identifiable, S : AbstractSet<T>>(private val spec: SetSpec<T, S>) : JvmGuardDialog() {

    private val existing: List<S> = spec.loadSets().toList()
    private val nameField = TextField("Name").apply {
        setWidthFull()
        testId = ID_NAME
    }

    init {
        headerTitle = "Save ${spec.singularName}"
        width = "34rem"
        isResizable = false

        val intro = Span(spec.saveSubtitle).apply { addClassName("jvmguard-field-hint") }
        add(VerticalLayout(intro, nameField).apply { isPadding = false; isSpacing = true })

        confirmFooter("Save", ID_CONFIRM) { save() }
    }

    private fun save() {
        val name = nameField.value.trim()
        if (name.isEmpty()) {
            nameField.isInvalid = true
            nameField.errorMessage = "Enter a name."
            return
        }
        val connection = Sessions.current()?.serverConnection ?: return
        val items = DeepCopy.clone(ArrayList(spec.currentItems())).onEach { it.resetModified() }
        val match = existing.firstOrNull { it.name == name }
        val modification = if (match != null) {
            match.items = items
            ListModification(listOf(match), emptyList(), emptyList(), spec.setClass)
        } else {
            ListModification(emptyList(), emptyList(), listOf(spec.createSet(name, items)), spec.setClass)
        }
        connection.applyListModification(modification)
        Notifications.show("Saved ${spec.singularName} \"$name\".")
        close()
    }

    companion object {
        const val ID_NAME = "set-save-name"
        const val ID_CONFIRM = "set-save-confirm"
    }
}
