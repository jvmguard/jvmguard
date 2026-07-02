package com.jvmguard.common.helper

import com.jvmguard.agent.config.base.Identifiable
import com.jvmguard.data.base.StoredConfig

class ListModification<T : Identifiable>(
    val modifiedItems: Collection<T>,
    val removedItems: Collection<T>,
    val newItems: Collection<T>,
    val itemClass: Class<T>,
) {

    init {
        for (newItem in newItems) {
            if (newItem is StoredConfig) {
                newItem.id = null
            }
        }
    }

    fun modifiedOrNewItems(): Iterable<T> = modifiedItems + newItems

    fun isEmpty(): Boolean = modifiedItems.isEmpty() && removedItems.isEmpty() && newItems.isEmpty()
}
