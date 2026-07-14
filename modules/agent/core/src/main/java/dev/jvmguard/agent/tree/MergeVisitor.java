package dev.jvmguard.agent.tree;

import dev.jvmguard.agent.tree.Tree.Visitor;

public class MergeVisitor implements Visitor<AgentTransactionTree> {
    private AgentTransactionTree result;
    private AgentTransactionTree rootTree;
    private final boolean stopAtZero;

    public AgentTransactionTree getResult() {
        return result;
    }

    public MergeVisitor(boolean stopAtZero) {
        this(stopAtZero, new AgentTransactionTree());
    }

    public MergeVisitor(boolean stopAtZero, AgentTransactionTree result) {
        this.stopAtZero = stopAtZero;
        this.result = result;
    }

    public void init(AgentTransactionTree rootTree) {
        this.rootTree = rootTree;
    }

    @Override
    public boolean preVisit(AgentTransactionTree tree) throws Exception {
        if (tree != rootTree) {
            if (stopAtZero && tree.getCount() == 0 && tree.getTime() == 0) {
                return false;
            }
            result = result.getOrCreateChild(tree);
        }
        result.addTime(tree.getTime());
        result.addCount(tree.getCount());
        return true;
    }

    @Override
    public void postVisit(AgentTransactionTree tree) {
        if (tree != rootTree) {
            result = result.getParent();
        }
    }
}
