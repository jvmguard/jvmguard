package com.jvmguard.ui.components

import com.vaadin.flow.component.treegrid.TreeGrid

open class SelectableTreeGrid<T> : TreeGrid<T>() {

    init {
        addExpandListener { event -> if (event.isFromClient) event.items.firstOrNull()?.let { select(it) } }
        addCollapseListener { event -> if (event.isFromClient) event.items.firstOrNull()?.let { select(it) } }
    }
}
