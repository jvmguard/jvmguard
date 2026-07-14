package dev.jvmguard.agent.thread;

import dev.jvmguard.agent.config.transactions.TransactionDef;
import dev.jvmguard.agent.policy.PolicyHandler;
import dev.jvmguard.agent.tree.AgentTransactionTree;

import java.util.Arrays;

class JvmGuardStack {
    private StackEntry[] entries = new StackEntry[4];
    private int pos = -1;

    public StackEntry push(long startTime, long samplingDistance, AgentTransactionTree tree, PolicyHandler policyHandler, TransactionDef namingTransaction, boolean endUserRequestSet, boolean origin, String groupName, int inhibitionId, Object transactionDetail, boolean countInvocation) {
        pos++;
        if (pos >= entries.length) {
            grow();
        }
        StackEntry stackEntry = entries[pos];
        if (stackEntry == null) {
            stackEntry = new StackEntry();
            entries[pos] = stackEntry;
        }
        stackEntry.init(startTime, samplingDistance, tree, policyHandler, namingTransaction, endUserRequestSet, origin, groupName, inhibitionId, transactionDetail, countInvocation);
        return stackEntry;
    }

    public void pop() {
        if (pos > -1) {
            pos--;
        }
    }

    public StackEntry peek() {
        if (pos < 0) {
            return null;
        } else {
            return entries[pos];
        }
    }

    private void grow() {
        int oldCapacity = entries.length;
        int newCapacity = oldCapacity + (oldCapacity >> 1);
        entries = Arrays.copyOf(entries, newCapacity);
    }

    public int size() {
        return pos + 1;
    }

    public StackEntry get(int i) {
        return entries[i];
    }
}
