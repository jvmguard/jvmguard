package dev.jvmguard.agent.util.collection;

import java.util.ArrayList;

public class ArrayStack<T> extends ArrayList<T> {

    public ArrayStack() {
    }

    public void push(T t) {
        add(t);
    }

    public T pop() {
        return remove(size() - 1);
    }

    public T peek() {
        return get(size() - 1);
    }

}
