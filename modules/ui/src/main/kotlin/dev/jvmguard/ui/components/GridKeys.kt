package dev.jvmguard.ui.components

import com.vaadin.flow.component.grid.Grid

fun <T> Grid<T>.editDeleteKeys(onEdit: (T) -> Unit = {}, onDelete: (T) -> Unit) {
    var focused: T? = null
    addCellFocusListener { focused = it.item.orElse(null) }
    element.addEventListener("keydown") { focused?.let(onEdit) }
        .setFilter("event.key === 'Enter' && event.target === event.currentTarget")
        .addEventData("event.preventDefault()")
    element.addEventListener("keydown") { focused?.let(onDelete) }
        .setFilter("event.key === 'Delete' && event.target === event.currentTarget")
        .addEventData("event.preventDefault()")
}
