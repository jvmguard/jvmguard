package dev.jvmguard.agent.tree;

import dev.jvmguard.agent.tree.Tree.Visitor;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public abstract class PrintVisitor<T extends Tree> implements Visitor<T> {
    int inset = 0;

    private Appendable appendable;
    private T root;

    @Override
    public String toString() {
        return appendable.toString();
    }

    public PrintVisitor(Appendable appendable) {
        this.appendable = appendable == null ? new StringBuilder() : appendable;
    }

    protected List<String> getAdditionalLines(T tree) {
        return Collections.emptyList();
    }

    protected abstract void appendInfo(Appendable appendable, T tree) throws IOException;

    @Override
    public boolean preVisit(T tree) throws IOException {
        if (root == null) {
            root = tree;
        }
        appendInset(inset);
        appendInfo(appendable, tree);
        for (String s : getAdditionalLines(tree)) {
            appendable.append('\n');
            appendInset(inset + 2);
            appendable.append(s);
        }
        appendable.append('\n');
        inset += 4;
        return true;
    }

    private void appendInset(int inset) throws IOException {
        for (int i = 0; i < inset; i++) {
            appendable.append(' ');
        }
    }

    @Override
    public void postVisit(T tree) {
        inset -= 4;
        if (tree == root) {
            finish(appendable);
        }
    }

    protected void finish(@SuppressWarnings("unused") Appendable appendable) {
    }
}
