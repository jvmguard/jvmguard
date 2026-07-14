package dev.jvmguard.ui.components.recording

import dev.jvmguard.ui.components.confirm
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.VerticalLayout

abstract class RecordingGrid : VerticalLayout() {
    abstract fun refresh()
    abstract fun addNew()

    protected fun emptyState(text: String): Span = Span(text).apply { addClassName("jvmguard-field-hint") }

    protected fun confirmDelete(noun: String, name: String, onConfirm: () -> Unit) =
        confirm("Delete $noun", "Delete \"$name\"?", "Delete", onConfirm)
}
