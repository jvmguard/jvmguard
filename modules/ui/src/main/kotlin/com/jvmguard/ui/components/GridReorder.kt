package com.jvmguard.ui.components

import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.grid.dnd.GridDropLocation
import com.vaadin.flow.component.grid.dnd.GridDropMode

fun <T> Grid<T>.enableRowReorder(items: () -> MutableList<T>, onReordered: () -> Unit) {
    onRowDrop { source, target, location ->
        moveWithin(items(), source, target, location)
        onReordered()
    }
}

// For grids where the drop target depends on the dragged row (a tree grid reordering within a node's
// sibling list); flat single-list grids use enableRowReorder. The source/self-drop guard is handled here.
fun <T> Grid<T>.onRowDrop(onDrop: (source: T, target: T, location: GridDropLocation) -> Unit) {
    var dragged: T? = null
    setRowsDraggable(true)
    dropMode = GridDropMode.BETWEEN
    addDragStartListener { dragged = it.draggedItems.firstOrNull() }
    addDropListener { event ->
        val source = dragged ?: return@addDropListener
        val target = event.dropTargetItem.orElse(null) ?: return@addDropListener
        if (source !== target) {
            onDrop(source, target, event.dropLocation)
        }
    }
    addDragEndListener { dragged = null }
}

fun <T> moveWithin(list: MutableList<T>, source: T, target: T, location: GridDropLocation) {
    list.remove(source)
    var index = list.indexOf(target)
    if (location == GridDropLocation.BELOW) {
        index += 1
    }
    list.add(index.coerceIn(0, list.size), source)
}
