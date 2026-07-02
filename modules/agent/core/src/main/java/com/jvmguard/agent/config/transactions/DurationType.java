package com.jvmguard.agent.config.transactions;

// do not rename enums
public enum DurationType {
    PERCENT("% slower than average"),
    MILLIS("ms and slower");

    private final String verbose;

    DurationType(String verbose) {
        this.verbose = verbose;
    }

    @Override
    public String toString() {
        return verbose;
    }
}
