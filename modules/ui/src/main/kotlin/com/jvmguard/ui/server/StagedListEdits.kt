package com.jvmguard.ui.server

import com.jvmguard.agent.config.base.Identifiable
import com.jvmguard.common.helper.DeepCopy
import com.jvmguard.common.helper.ListModification

class StagedListEdits<T : Identifiable>(private val itemClass: Class<T>) {

    private val items = mutableListOf<T>()
    private val removed = mutableListOf<T>()
    private val modifiedIds = mutableSetOf<Long>()
    private var loaded = false

    fun ensureLoaded(load: () -> List<T>) {
        if (!loaded) {
            load().forEach { items.add(DeepCopy.clone(it)) }
            loaded = true
        }
    }

    fun items(): List<T> = items

    fun add(item: T) {
        items.add(item)
    }

    fun markModified(item: T) {
        item.id?.let { modifiedIds.add(it) }
    }

    fun remove(item: T) {
        items.remove(item)
        item.id?.let { id ->
            modifiedIds.remove(id)
            removed.add(item)
        }
    }

    fun hasChanges(): Boolean =
        removed.isNotEmpty() || modifiedIds.isNotEmpty() || items.any { it.id == null }

    fun toModification(): ListModification<T> {
        val added = items.filter { it.id == null }
        val modified = items.filter { it.id != null && it.id in modifiedIds }
        return ListModification(modified, removed, added, itemClass)
    }
}
