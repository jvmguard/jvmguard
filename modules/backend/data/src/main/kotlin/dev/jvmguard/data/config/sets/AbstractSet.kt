package dev.jvmguard.data.config.sets

import dev.jvmguard.data.base.StoredConfig

abstract class AbstractSet<T> : StoredConfig, Comparable<AbstractSet<T>> {

    var name: String = "New set"
        set(value) { field = changed(field, value) }

    var items: MutableList<T> = ArrayList()

    protected constructor()

    protected constructor(name: String, items: Collection<T>) {
        this.name = name
        this.items.addAll(items)
    }

    override fun compareTo(other: AbstractSet<T>): Int = name.compareTo(other.name)

    override fun toString(): String = name
}
