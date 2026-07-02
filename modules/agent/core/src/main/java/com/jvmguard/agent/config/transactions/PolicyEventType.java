package com.jvmguard.agent.config.transactions;

import com.jvmguard.agent.tree.AbstractTransactionTree.PolicyType;

import java.util.HashMap;
import java.util.Map;

public enum PolicyEventType {
    NORMAL("Normal", PolicyType.NORMAL),
    SLOW("Slow", PolicyType.SLOW),
    VERY_SLOW("Very slow", PolicyType.VERY_SLOW),
    OVERDUE("Overdue", PolicyType.NORMAL),
    ERROR("Error", PolicyType.ERROR);

    private final String verbose;
    private final PolicyType transactionTreePolicyType;

    private static final Map<PolicyType, PolicyEventType> transactionTreeTypeToType = new HashMap<>();

    static {
        for (PolicyEventType type : values()) {
            if (!transactionTreeTypeToType.containsKey(type.getTransactionTreePolicyType())) {
                transactionTreeTypeToType.put(type.getTransactionTreePolicyType(), type);
            }
        }
    }

    public static PolicyEventType getByTransactionTreeType(PolicyType policyType) {
        PolicyEventType ret = transactionTreeTypeToType.get(policyType);
        return ret == null ? NORMAL : ret;
    }

    PolicyEventType(String verbose, PolicyType transactionTreePolicyType) {
        this.verbose = verbose;
        this.transactionTreePolicyType = transactionTreePolicyType;
    }

    public PolicyType getTransactionTreePolicyType() {
        return transactionTreePolicyType;
    }

    @Override
    public String toString() {
        return verbose;
    }
}
