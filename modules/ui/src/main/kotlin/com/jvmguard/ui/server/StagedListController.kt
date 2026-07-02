package com.jvmguard.ui.server

import com.jvmguard.agent.config.base.Identifiable

class StagedListController<T : Identifiable>(
    private val edits: () -> StagedListEdits<T>,
    private val load: () -> List<T>,
    private val markDirty: () -> Unit,
    private val render: (List<T>) -> Unit,
) {

    fun reload() {
        edits().ensureLoaded(load)
        render(edits().items())
    }

    fun add(item: T) = changed { it.add(item) }

    fun markModified(item: T) = changed { it.markModified(item) }

    fun remove(item: T) = changed { it.remove(item) }

    private fun changed(op: (StagedListEdits<T>) -> Unit) {
        op(edits())
        markDirty()
        render(edits().items())
    }
}
