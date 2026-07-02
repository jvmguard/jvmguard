package com.jvmguard.data.config.sets

import com.jvmguard.data.base.StoredConfig

abstract class AbstractSet<T> : StoredConfig, Comparable<AbstractSet<T>> {

    var name: String = "New set"
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var items: MutableList<T> = ArrayList()

    protected constructor()

    protected constructor(name: String, items: Collection<T>) {
        this.name = name
        this.items.addAll(items)
    }

    override fun compareTo(other: AbstractSet<T>): Int = name.compareTo(other.name)

    override fun toString(): String = name
}
