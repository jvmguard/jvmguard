package dev.jvmguard.agent.config.transactions;

import dev.jvmguard.agent.config.base.ConfigDoc;

public enum DurationType {
    @ConfigDoc("Value is a percentage slower than the running average.")
    PERCENT("% slower than average"),
    @ConfigDoc("Value is an absolute duration in milliseconds.")
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
