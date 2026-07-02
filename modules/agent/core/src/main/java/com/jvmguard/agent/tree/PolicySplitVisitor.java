package com.jvmguard.agent.tree;

import com.jvmguard.agent.tree.Tree.Visitor;

public class PolicySplitVisitor implements Visitor<AgentTransactionTree> {
    private boolean initial = true;
    private AgentTransactionTree currentDestinationTree;

    public PolicySplitVisitor init(AgentTransactionTree rootDestinationTree) {
        currentDestinationTree = rootDestinationTree;
        initial = true;
        return this;
    }

    @Override
    public boolean preVisit(AgentTransactionTree tree) throws Exception {
        if (tree.getCount() == 0) {
            return false;
        }

        if (!initial) {
            currentDestinationTree = currentDestinationTree.getOrCreateChild(tree);
        }
        initial = false;

        currentDestinationTree.addData(tree);
        tree.clear();
        return true;
    }

    @Override
    public void postVisit(AgentTransactionTree tree) throws Exception {
        currentDestinationTree = currentDestinationTree.getParent();
    }
}
