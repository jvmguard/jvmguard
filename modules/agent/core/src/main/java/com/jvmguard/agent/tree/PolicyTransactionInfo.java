package com.jvmguard.agent.tree;

import com.jvmguard.agent.AgentProperties;

public class PolicyTransactionInfo extends AgentTransactionInfo {
    private static final int MINIMUM_INVOCATIONS_FOR_AVERAGE = AgentProperties.getInteger("minimumInvocationsForAverage", 10);
    private static final int CHECK_LONG = -2;

    // will only be accessed inside config lock
    private long totalTime;
    private long invocationCount;

    private int compactAverage = -1; // update might take until comm thread, and the checking thread synchronizes again, doesn't matter (happens at least once per minute)
    private volatile long average = -1;

    public PolicyTransactionInfo(String name, int transactionTypeId, long id) {
        super(name, transactionTypeId, id);
    }

    public void addInvocations(long count, long time) {
        invocationCount += count;
        totalTime += time;
    }

    public void updateAverage() {
        if (invocationCount > MINIMUM_INVOCATIONS_FOR_AVERAGE) {
            long average = totalTime / invocationCount;
            if (average < Integer.MAX_VALUE) {
                compactAverage = (int)average;
            } else {
                compactAverage = CHECK_LONG;
            }
            this.average = average;
        }
    }

    @Override
    public long getAverage() {
        if (compactAverage == CHECK_LONG) {
            return average;
        } else {
            return compactAverage;
        }
    }
}
