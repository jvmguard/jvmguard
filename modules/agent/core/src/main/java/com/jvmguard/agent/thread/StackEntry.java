package com.jvmguard.agent.thread;

import com.jvmguard.agent.config.transactions.DevOpsAnnotatedTransactionDef;
import com.jvmguard.agent.config.transactions.TransactionDef;
import com.jvmguard.agent.config.transactions.TransactionType;
import com.jvmguard.agent.policy.PolicyHandler;
import com.jvmguard.agent.tree.AgentTransactionTree;
import com.jvmguard.annotation.ReentryInhibition;

import javax.annotation.concurrent.GuardedBy;
import java.lang.annotation.Annotation;
import java.util.IdentityHashMap;

public class StackEntry {
    private static final int DEVOPS_INHIBITION_ID_NAME = 0;
    private static final int DEVOPS_INHIBITION_ID_GROUP = -1;
    private static final int DEVOPS_INHIBITION_ID_TYPE = -2;
    private static final int DEVOPS_INHIBITION_ID_ALL = -3;

    @GuardedBy("devOpsAnnotationIds")
    private static final IdentityHashMap<Annotation, Integer> devOpsAnnotationIds = new IdentityHashMap<>();
    @GuardedBy("devOpsAnnotationIds")
    private static int nextDevOpsAnnotationId = 1;


    long startTime;
    long samplingDistance;
    AgentTransactionTree tree;
    PolicyHandler policyHandler;
    TransactionDef namingTransaction;
    boolean endUserRequestSet;
    boolean origin;
    boolean countInvocation;

    private String groupName;
    private int inhibitionId;
    private boolean devops;

    int entryCount;
    boolean overdue;
    String errorMessage;

    Object transactionDetail;

    public StackEntry init(long startTime, long samplingDistance, AgentTransactionTree tree, PolicyHandler policyHandler, TransactionDef namingTransaction, boolean endUserRequestSet, boolean origin, String groupName, int inhibitionId, Object transactionDetail, boolean countInvocation) {
        this.startTime = startTime;
        this.samplingDistance = samplingDistance;
        this.tree = tree;
        this.policyHandler = policyHandler;
        this.namingTransaction = namingTransaction;
        this.endUserRequestSet = endUserRequestSet;
        this.origin = origin;
        this.groupName = groupName;
        this.inhibitionId = inhibitionId;
        this.transactionDetail = transactionDetail;
        this.countInvocation = countInvocation;
        devops = namingTransaction != null && namingTransaction.getTransactionType() == TransactionType.DEVOPS;

        entryCount = 1;
        overdue = false;
        errorMessage = null;

        return this;
    }

    public PolicyHandler getPolicyHandler() {
        return policyHandler;
    }

    public boolean shouldLeaveCurrentTree() {
        return --entryCount <= 0;
    }

    public boolean shouldCreateNewTree(String transactionName, TransactionDef newNamingTransaction, String devOpsGroupName, int devOpsInhibitionId) {
        if (isPreventSpecialReentry(newNamingTransaction, devOpsGroupName, devOpsInhibitionId)) {
            entryCount++;
            return false;
        }
        if (tree != null && transactionName.equals(tree.getName())) {
            entryCount++;
            return false;
        } else {
            return true;
        }
    }

    protected boolean isPreventSpecialReentry(TransactionDef newNamingTransaction, String newDevOpsGroupName, int newDevOpsInhibitionId) {
        if (devops) {
            if (inhibitionId > 0) {
                return newDevOpsInhibitionId == inhibitionId;
            } else {
                switch (inhibitionId) {
                    case DEVOPS_INHIBITION_ID_GROUP:
                        // group is interned
                        //noinspection StringEquality
                        return newNamingTransaction != null && groupName != null && groupName == getActualGroupName(newNamingTransaction, newDevOpsGroupName);
                    case DEVOPS_INHIBITION_ID_TYPE:
                        return newNamingTransaction != null && TransactionType.DEVOPS == newNamingTransaction.getTransactionType();
                    case DEVOPS_INHIBITION_ID_ALL:
                        return true;
                }
            }
        } else if (namingTransaction != null) {
            switch (namingTransaction.getNaming().getReentryInhibition()) {
                case DEF:
                    return namingTransaction == newNamingTransaction;
                case GROUP:
                    // group is interned
                    //noinspection StringEquality
                    return newNamingTransaction != null && namingTransaction.getUsedGroup() == getActualGroupName(newNamingTransaction, newDevOpsGroupName);
                case TYPE:
                    return newNamingTransaction != null && namingTransaction.getTransactionType() == newNamingTransaction.getTransactionType();
                case ALL:
                    return true;
            }
        }
        return false;
    }

    protected String getActualGroupName(TransactionDef newNamingTransaction, String newDevOpsGroupName) {
        return newNamingTransaction instanceof DevOpsAnnotatedTransactionDef ? newDevOpsGroupName : newNamingTransaction.getUsedGroup();
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public long getStartTime() {
        return startTime;
    }

    public boolean isOverdue() {
        return overdue;
    }

    public void setOverdue(boolean overdue) {
        this.overdue = overdue;
    }

    public AgentTransactionTree getTree() {
        return tree;
    }

    @Override
    public String toString() {
        return "StackEntry{" +
            "startTime=" + startTime +
            ", overdue=" + overdue +
            ", tree=" + tree +
            ", entryCount=" + entryCount +
            '}';
    }

    public static Integer getDevOpsInhibitionId(ReentryInhibition reentryInhibition, Annotation annotation) {
        switch (reentryInhibition) {
            case ANNOTATION:
                synchronized (devOpsAnnotationIds) {
                    Integer ret = devOpsAnnotationIds.get(annotation);
                    if (ret == null) {
                        ret = nextDevOpsAnnotationId++;
                        devOpsAnnotationIds.put(annotation, ret);
                    }
                    return ret;
                }
            case GROUP:
                return DEVOPS_INHIBITION_ID_GROUP;
            case DEV_OPS:
                return DEVOPS_INHIBITION_ID_TYPE;
            case ALL:
                return DEVOPS_INHIBITION_ID_ALL;
        }
        return DEVOPS_INHIBITION_ID_NAME;
    }


}
