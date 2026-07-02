package com.jvmguard.agent.policy;

import com.jvmguard.agent.config.transactions.DurationType;
import com.jvmguard.agent.config.transactions.Policy;
import com.jvmguard.agent.config.transactions.PolicyEventType;
import com.jvmguard.agent.tree.AgentTransactionInfo;

public class PolicyHandler {

    private Policy policy;

    private boolean useAverage;

    public PolicyHandler(Policy policy) {
        this.policy = policy;
        useAverage = (policy.getSlowValue() > 0 && policy.getSlowDurationType() == DurationType.PERCENT) ||
            (policy.getVerySlowValue() > 0 && policy.getVerySlowDurationType() == DurationType.PERCENT) ||
            (policy.getOverdueValue() > 0 && policy.getOverdueDurationType() == DurationType.PERCENT);
    }


    public boolean isOverdue(long nanoTime, AgentTransactionInfo agentTransactionInfo) {
        boolean overdue = false;
        if (policy.getOverdueValue() > 0) {
            overdue = checkTiming(nanoTime, getAverage(agentTransactionInfo), policy.getOverdueDurationType(), policy.getOverdueValue());
        }
        return overdue;
    }

    public PolicyEventType getFinishedEventType(String errorString, long nanoTime, AgentTransactionInfo agentTransactionInfo) {
        long average = getAverage(agentTransactionInfo);
        if (errorString != null) {
            return PolicyEventType.ERROR;
        } else if (checkTiming(nanoTime, average, policy.getVerySlowDurationType(), policy.getVerySlowValue())) {
            return PolicyEventType.VERY_SLOW;
        } else if (checkTiming(nanoTime, average, policy.getSlowDurationType(), policy.getSlowValue())) {
            return PolicyEventType.SLOW;
        }
        return null;
    }

    private static boolean checkTiming(long nanoTime, long average, DurationType durationType, long policyValue) {
        if (policyValue > 0) {
            if (durationType == DurationType.MILLIS) {
                if (nanoTime >= policyValue * 1000 * 1000) {
                    return true;
                }
            } else {
                if (average > 0 && (nanoTime * 100) / average >= policyValue) {
                    return true;
                }
            }
        }
        return false;
    }

    private long getAverage(AgentTransactionInfo agentTransactionInfo) {
        if (useAverage) {
            return agentTransactionInfo.getAverage();
        }
        return -1;
    }

    public Policy getPolicy() {
        return policy;
    }
}
