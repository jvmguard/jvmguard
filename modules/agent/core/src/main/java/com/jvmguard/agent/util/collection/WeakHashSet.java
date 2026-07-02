package com.jvmguard.agent.util.collection;

import org.jetbrains.annotations.NotNull;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.WeakHashMap;

public class WeakHashSet<T> extends AbstractSet<T> {
    private final Object PRESENT = new Object();
    private final WeakHashMap<T, Object> backingMap = new WeakHashMap<>();

    @Override
    public boolean add(T o) {
        return backingMap.put(o, PRESENT) == null;
    }

    @Override
    public boolean contains(Object o) {
        //noinspection SuspiciousMethodCalls
        return backingMap.containsKey(o);
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return backingMap.keySet().iterator();
    }

    @Override
    public int size() {
        return backingMap.size();
    }
}


