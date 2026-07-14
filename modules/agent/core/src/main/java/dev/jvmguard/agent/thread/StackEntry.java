package dev.jvmguard.agent.thread;

import dev.jvmguard.agent.config.transactions.DeclaredTransactionDef;
import dev.jvmguard.agent.config.transactions.TransactionDef;
import dev.jvmguard.agent.config.transactions.TransactionType;
import dev.jvmguard.agent.policy.PolicyHandler;
import dev.jvmguard.agent.tree.AgentTransactionTree;
import dev.jvmguard.annotation.ReentryInhibition;

import javax.annotation.concurrent.GuardedBy;
import java.lang.annotation.Annotation;
import java.util.IdentityHashMap;

public class StackEntry {
    private static final int DECLARED_INHIBITION_ID_NAME = 0;
    private static final int DECLARED_INHIBITION_ID_GROUP = -1;
    private static final int DECLARED_INHIBITION_ID_TYPE = -2;
    private static final int DECLARED_INHIBITION_ID_ALL = -3;

    @GuardedBy("declaredAnnotationIds")
    private static final IdentityHashMap<Annotation, Integer> declaredAnnotationIds = new IdentityHashMap<>();
    @GuardedBy("declaredAnnotationIds")
    private static int nextDeclaredAnnotationId = 1;


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
    private boolean declared;

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
        declared = namingTransaction != null && namingTransaction.getTransactionType() == TransactionType.DECLARED;

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

    public boolean shouldCreateNewTree(String transactionName, TransactionDef newNamingTransaction, String declaredGroupName, int declaredInhibitionId) {
        if (isPreventSpecialReentry(newNamingTransaction, declaredGroupName, declaredInhibitionId)) {
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

    protected boolean isPreventSpecialReentry(TransactionDef newNamingTransaction, String newDeclaredGroupName, int newDeclaredInhibitionId) {
        if (declared) {
            if (inhibitionId > 0) {
                return newDeclaredInhibitionId == inhibitionId;
            } else {
                switch (inhibitionId) {
                    case DECLARED_INHIBITION_ID_GROUP:
                        // group is interned
                        //noinspection StringEquality
                        return newNamingTransaction != null && groupName != null && groupName == getActualGroupName(newNamingTransaction, newDeclaredGroupName);
                    case DECLARED_INHIBITION_ID_TYPE:
                        return newNamingTransaction != null && TransactionType.DECLARED == newNamingTransaction.getTransactionType();
                    case DECLARED_INHIBITION_ID_ALL:
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
                    return newNamingTransaction != null && namingTransaction.getUsedGroup() == getActualGroupName(newNamingTransaction, newDeclaredGroupName);
                case TYPE:
                    return newNamingTransaction != null && namingTransaction.getTransactionType() == newNamingTransaction.getTransactionType();
                case ALL:
                    return true;
            }
        }
        return false;
    }

    protected String getActualGroupName(TransactionDef newNamingTransaction, String newDeclaredGroupName) {
        return newNamingTransaction instanceof DeclaredTransactionDef ? newDeclaredGroupName : newNamingTransaction.getUsedGroup();
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

    public static Integer getDeclaredInhibitionId(ReentryInhibition reentryInhibition, Annotation annotation) {
        switch (reentryInhibition) {
            case ANNOTATION:
                synchronized (declaredAnnotationIds) {
                    Integer ret = declaredAnnotationIds.get(annotation);
                    if (ret == null) {
                        ret = nextDeclaredAnnotationId++;
                        declaredAnnotationIds.put(annotation, ret);
                    }
                    return ret;
                }
            case GROUP:
                return DECLARED_INHIBITION_ID_GROUP;
            case DECLARED:
                return DECLARED_INHIBITION_ID_TYPE;
            case ALL:
                return DECLARED_INHIBITION_ID_ALL;
        }
        return DECLARED_INHIBITION_ID_NAME;
    }


}
