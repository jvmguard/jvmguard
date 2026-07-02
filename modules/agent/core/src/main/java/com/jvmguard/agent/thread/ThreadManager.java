package com.jvmguard.agent.thread;

import com.jvmguard.agent.RequestSession;
import com.jvmguard.agent.callee.DevOpsHandler.GetterChainEntry;
import com.jvmguard.agent.config.transactions.PolicyDef;
import com.jvmguard.agent.config.transactions.PolicyEventType;
import com.jvmguard.agent.config.transactions.TransactionDef;
import com.jvmguard.agent.config.transactions.TransactionType;
import com.jvmguard.agent.policy.PolicyHandler;
import com.jvmguard.agent.tree.AbstractTransactionTree.PolicyType;
import com.jvmguard.agent.tree.AgentTransactionInfo;
import com.jvmguard.agent.tree.AgentTransactionTree;
import com.jvmguard.agent.tree.MergeVisitor;
import com.jvmguard.agent.tree.PolicySplitVisitor;

import java.lang.ref.WeakReference;
import java.util.IdentityHashMap;

public class ThreadManager {

    private final Object threadLock = new Object();
    private AgentTransactionTree rootTree = new AgentTransactionTree();

    private final AgentTransactionTree lookupTree = new AgentTransactionTree();
    private final AgentTransactionInfo lookupInfo = new AgentTransactionInfo();
    private final PolicySplitVisitor policySplitVisitor = new PolicySplitVisitor();

    private JvmGuardStack stack = new JvmGuardStack();

    private IdentityHashMap<String, GetterChainEntry[]> devOpsGetterChainMap = new IdentityHashMap<>();

    private WeakReference<Thread> threadRef;

    private AgentTransactionTree newOverdueTree;

    public ThreadManager(Thread thread) {
        threadRef = new WeakReference<>(thread);
    }

    public IdentityHashMap<String, GetterChainEntry[]> getDevOpsGetterChainMap() {
        return devOpsGetterChainMap;
    }

    public Thread getThread() {
        return threadRef != null ? threadRef.get() : null;
    }

    private long enterInterceptionMethod(String transactionName, Object transactionDetail, TransactionType transactionType, PolicyDef policyDef, TransactionDef namingTransaction, String devOpsGroupName, int devOpsInhibitionId, boolean countInvocation) {
        try {
            synchronized (threadLock) {
                StackEntry stackEntry = stack.peek();
                if (stackEntry == null || stackEntry.shouldCreateNewTree(transactionName, namingTransaction, devOpsGroupName, devOpsInhibitionId)) {
                    AgentTransactionTree currentTree = rootTree;
                    if (stackEntry != null) {
                        currentTree = stackEntry.tree;
                    }
                    AgentTransactionTree childTree = currentTree.getChild(lookupTree.init(lookupInfo.init(transactionName, transactionType.getId()), PolicyType.PARTIAL.getTypeString()));
                    AgentTransactionInfo info;
                    if (childTree != null) {
                        currentTree = childTree;
                    } else {
                        info = RequestSession.getInstance().getTransactionInfo(lookupInfo);
                        currentTree = currentTree.createChild(lookupTree.init(info, PolicyType.PARTIAL.getTypeString()));
                    }

                    long startTime = System.nanoTime();

                    PolicyHandler policyHandler = null;
                    if (policyDef != null && policyDef.getPolicyHandler() != null) {
                        policyHandler = policyDef.getPolicyHandler();
                    }

                    stack.push(startTime, -1, currentTree, policyHandler, namingTransaction, false, false, devOpsGroupName, devOpsInhibitionId, transactionDetail, countInvocation);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return Long.MAX_VALUE;
    }

    public long enterInterceptionMethod(String transactionName, TransactionType transactionType, PolicyDef policyDef, TransactionDef namingTransaction, String devOpsGroupName, int devOpsInhibitionId) {
        return enterInterceptionMethod(transactionName, null, transactionType, policyDef, namingTransaction, devOpsGroupName, devOpsInhibitionId, true);
    }

    public long enterInterceptionMethod(String transactionName, Object transactionDetail, TransactionType transactionType, PolicyDef policyDef, TransactionDef namingTransaction) {
        return enterInterceptionMethod(transactionName, transactionDetail, transactionType, policyDef, namingTransaction, null, 0, true);
    }

    public boolean exitInterceptionMethod(Object errorObject) {
        return exitInterceptionMethod(errorObject, true);
    }

    public boolean exitInterceptionMethod(Object errorObject, boolean countInvocation) {
        boolean error = errorObject != null;
        synchronized (threadLock) {
            long currentTime = System.nanoTime();
            StackEntry stackEntry = stack.peek();
            if (stackEntry != null && stackEntry.shouldLeaveCurrentTree()) {
                stack.pop();
                AgentTransactionTree currentTree = stackEntry.tree;
                long time = currentTime - stackEntry.startTime;
                currentTree.addTime(time);
                if (countInvocation || stackEntry.countInvocation) {
                    currentTree.addCount(1);
                }

                String errorString = null;
                PolicyEventType policyEventType = null;
                boolean splitTree = false;

                PolicyHandler policyHandler = stackEntry.getPolicyHandler();
                if (policyHandler != null && currentTree.getInfo() != null) {
                    errorString = policyHandler.getPolicy().getUsedError(errorObject);
                    if (errorString == null) {
                        errorString = stackEntry.getErrorMessage();
                    }
                    policyEventType = policyHandler.getFinishedEventType(errorString, time, currentTree.getInfo());
                    splitTree = policyHandler.getPolicy().isSplitTree();
                    error = policyEventType == PolicyEventType.ERROR;
                }
                handlePolicySplit(currentTree, errorString, splitTree ? policyEventType : null);
            }
        }
        return error;
    }

    private void handlePolicySplit(AgentTransactionTree currentTree, String errorString, PolicyEventType policyEventType) {
        //noinspection StringEquality
        if (currentTree.getPolicyTypeString() == PolicyType.PARTIAL.getTypeString()) {
            String type;
            if (policyEventType == null) {
                type = PolicyType.NORMAL.getTypeString();
            } else if (policyEventType == PolicyEventType.ERROR) {
                type = errorString;
            } else {
                type = policyEventType.getTransactionTreePolicyType().getTypeString();
            }
            AgentTransactionTree parentTree = currentTree.getParent();
            if (parentTree != null) {
                AgentTransactionTree destinationTree = parentTree.getOrCreateChild(lookupTree.init(currentTree.getInfo(), type));
                currentTree.visit(policySplitVisitor.init(destinationTree));
            }
        }
    }

    public void mergeData(MergeVisitor transactionVisitor, MergeVisitor overdueVisitor) {
        synchronized (threadLock) {
            if (newOverdueTree != null && overdueVisitor != null) {
                overdueVisitor.init(newOverdueTree);
                newOverdueTree.visit(overdueVisitor);
            }
            newOverdueTree = null;

            transactionVisitor.init(rootTree);
            rootTree.visit(transactionVisitor);

            AgentTransactionTree newRoot = new AgentTransactionTree();
            if (stack.size() > 0) {
                newRoot.putChild(stack.get(0).tree);
            }
            rootTree = newRoot;
        }
    }

    public void checkOverdue(long currentTime) {
        synchronized (threadLock) {
            for (int stackIndex = 0; stackIndex < stack.size(); stackIndex++) {
                StackEntry stackEntry = stack.get(stackIndex);
                PolicyHandler policyHandler = stackEntry.getPolicyHandler();
                if (!stackEntry.isOverdue() && policyHandler != null && stackEntry.getTree() != null && stackEntry.getTree().getInfo() != null) {
                    if (policyHandler.isOverdue(currentTime - stackEntry.startTime, stackEntry.getTree().getInfo())) {
                        stackEntry.setOverdue(true);
                        if (newOverdueTree == null) {
                            newOverdueTree = new AgentTransactionTree();
                        }
                        AgentTransactionTree overdueLookupTree = new AgentTransactionTree();
                        AgentTransactionTree currentOverdueTree = newOverdueTree;
                        for (int overdueIndex = 0; overdueIndex <= stackIndex; overdueIndex++) {
                            StackEntry overdueEntry = stack.get(overdueIndex);
                            currentOverdueTree = currentOverdueTree.getOrCreateChild(overdueLookupTree.init(overdueEntry.tree.getInfo(), PolicyType.NORMAL.getTypeString()));
                        }
                        currentOverdueTree.addCount(1);
                    }
                }
            }
        }
    }

    public boolean isOverdue() {
        synchronized (threadLock) {
            for (int i = 0; i < stack.size(); i++) {
                StackEntry stackEntry = stack.get(i);
                if (stackEntry.isOverdue()) {
                    return true;
                }
            }
        }
        return false;
    }
}
