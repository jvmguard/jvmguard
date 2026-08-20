package dev.jvmguard.ui.components.recording

import dev.jvmguard.ui.components.confirm
import dev.jvmguard.ui.server.t
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.VerticalLayout

abstract class RecordingGrid : VerticalLayout() {
    abstract fun refresh()
    abstract fun addNew()

    protected fun emptyState(text: String): Span = Span(text).apply { addClassName("jvmguard-field-hint") }

    // "headerKey" is one of the `recording.delete.<noun>` keys
    protected fun confirmDelete(headerKey: String, name: String, onConfirm: () -> Unit) =
        confirm(t(headerKey), t("recording.delete.text", name), t("common.delete"), onConfirm)
}
