package com.jvmguard.agent.tree;

import com.jvmguard.agent.comm.AgentSerializable;
import com.jvmguard.agent.comm.CommunicationContext;
import com.jvmguard.agent.util.collection.ArrayStack;
import org.jetbrains.annotations.NotNull;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public abstract class Tree<T extends Tree<T>> implements AgentSerializable, Comparable<T>, Iterable<T> {
    private T parent;

    protected Map<T, T> children;

    protected Tree() {
        children = new HashMap<>();
    }

    protected Tree(T parent) {
        this.parent = parent;
    }

    protected abstract T createChildInt(T lookupTree);

    protected final void setParent(T parent) {
        this.parent = parent;
    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws IOException {
        int childrenCount = in.readInt();
        for (int i = 0; i < childrenCount; i++) {
            if (children == null) {
                children = new HashMap<>();
            }

            T child = createChildInt(null);
            child.read(context, in);
            children.put(child, child);
        }
    }

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws IOException {
        if (children == null) {
            out.writeInt(0);
        } else {
            out.writeInt(children.size());
            for (T child : children.keySet()) {
                child.write(context, out);
            }
        }
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return children == null ? Collections.emptyIterator() : children.keySet().iterator();
    }

    public void remove(T child) {
        if (children != null) {
            children.remove(child);
        }
    }

    public void add(T newChild) {
        if (children == null) {
            children = new HashMap<>();
        }
        children.put(newChild, newChild);
    }

    public Set<T> children() {
        if (children != null) {
            return children.keySet();
        } else {
            return Collections.emptySet();
        }
    }

    public int getChildCount() {
        if (children == null) {
            return 0;
        } else {
            return children.size();
        }
    }

    public T getChild(T lookupTree) {
        T ret = null;
        if (children != null) {
            ret = children.get(lookupTree);
        }
        return ret;
    }

    public T getOrCreateChild(T lookupTree) {
        T ret;
        if (children != null) {
            ret = children.get(lookupTree);
            if (ret != null) {
                return ret;
            }
        } else {
            children = new HashMap<>();
        }
        ret = createChildInt(lookupTree);
        children.put(ret, ret);
        return ret;
    }

    public T createChild(T lookupTree) {
        T ret;
        if (children == null) {
            children = new HashMap<>();
        }
        ret = createChildInt(lookupTree);
        children.put(ret, ret);
        return ret;
    }

    public final void visit(Visitor<T> visitor) {
        visit(false, visitor);
    }

    @SuppressWarnings("unchecked")
    public final void visit(boolean sorted, Visitor<T> visitor) {
        ArrayStack<StackEntry<T>> stack = new ArrayStack<>();
        stack.push(new StackEntry<>((T)this));

        try {
            while (!stack.isEmpty()) {
                StackEntry<T> stackEntry = stack.peek();
                T currentTree = stackEntry.tree;
                switch (stackEntry.state) {
                    case PRE:
                        if (visitor.preVisit(currentTree)) {
                            if (currentTree.children != null) {
                                Collection<T> collection = currentTree.children.keySet();
                                if (sorted) {
                                    List<T> list = new ArrayList<>(collection);
                                    Collections.sort(list);
                                    collection = list;
                                }
                                stackEntry.iterator = collection.iterator();
                                stackEntry.state = EntryState.ITERATING;
                            } else {
                                stackEntry.state = EntryState.POST;
                            }
                        } else {
                            stack.pop();
                        }
                        break;
                    case ITERATING:
                        if (stackEntry.iterator.hasNext()) {
                            stack.push(new StackEntry<>(stackEntry.iterator.next()));
                        } else {
                            stackEntry.state = EntryState.POST;
                        }
                        break;
                    case POST:
                        visitor.postVisit(currentTree);
                        stack.pop();
                        break;
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class StackEntry<T extends Tree> {
        T tree;
        EntryState state = EntryState.PRE;
        Iterator<T> iterator;

        public StackEntry(T tree) {
            this.tree = tree;
        }
    }

    private enum EntryState {
        PRE,
        ITERATING,
        POST
    }

    // do not call this from frontend, parent might be from a different tree
    public T getParent() {
        return parent;
    }

    @Override
    public String toString() {
        return "Tree{" +
            "parent=" + parent +
            '}';
    }

    public interface Visitor<T extends Tree> {
        boolean preVisit(T tree) throws Exception;
        void postVisit(T tree) throws Exception;
    }

}
